package no.nav.helse.hendelser

import no.nav.helse.person.*
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingstidslinje.Historie
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Økonomi
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

class Utbetalingshistorikk(
    meldingsreferanseId: UUID,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val vedtaksperiodeId: String,
    private val arbeidskategorikoder: Map<String, LocalDate>,
    utbetalinger: List<Infotrygdperiode>,
    private val inntektshistorikk: List<Inntektsopplysning>,
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : ArbeidstakerHendelse(meldingsreferanseId, aktivitetslogg) {
    private val utbetalinger = Infotrygdperiode.sorter(utbetalinger)

    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    override fun organisasjonsnummer() = organisasjonsnummer

    internal fun erRelevant(vedtaksperiodeId: UUID) =
        vedtaksperiodeId.toString() == this.vedtaksperiodeId

    internal fun valider(periode: Periode, skjæringstidspunkt: LocalDate?) =
        valider(false, periode, skjæringstidspunkt)

    internal fun validerOverlappende(periode: Periode, skjæringstidspunkt: LocalDate?) =
        valider(true, periode, skjæringstidspunkt)

    private fun valider(bareOverlappende: Boolean, periode: Periode, skjæringstidspunkt: LocalDate?): IAktivitetslogg {
        if (!erNormalArbeidstaker(skjæringstidspunkt)) error("Personen er ikke registrert som normal arbeidstaker i Infotrygd")
        Infotrygdperiode.Utbetalingsperiode.valider(utbetalinger, this, bareOverlappende, periode, organisasjonsnummer)
        Inntektsopplysning.valider(inntektshistorikk, this, skjæringstidspunkt, periode)
        return this
    }

    private fun erNormalArbeidstaker(skjæringstidspunkt: LocalDate?) =
        if(arbeidskategorikoder.isEmpty()) true
        else arbeidskategorikoder
            .filter { (_, dato) -> dato >= skjæringstidspunkt }
            .all { (arbeidskategorikode, _) -> arbeidskategorikode == "01" }

    internal fun addInntekter(person: Person, hendelse: PersonHendelse = this) {
        Inntektsopplysning.addInntekter(person, hendelse, inntektshistorikk)
    }

    class Inntektsopplysning(
        private val sykepengerFom: LocalDate,
        private val inntektPerMåned: Inntekt,
        private val orgnummer: String,
        private val refusjonTilArbeidsgiver: Boolean,
        private val refusjonTom: LocalDate? = null
    ) {

        internal companion object {
            private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

            fun valider(
                liste: List<Inntektsopplysning>,
                aktivitetslogg: IAktivitetslogg,
                skjæringstidspunkt: LocalDate?,
                periode: Periode
            ) {
                liste.validerAlleInntekterForSammenhengendePeriode(skjæringstidspunkt, aktivitetslogg, periode)
                liste.validerAntallInntekterPerArbeidsgiverPerDato(skjæringstidspunkt, aktivitetslogg, periode)
            }

            private fun List<Inntektsopplysning>.validerAlleInntekterForSammenhengendePeriode(
                skjæringstidspunkt: LocalDate?,
                aktivitetslogg: IAktivitetslogg,
                periode: Periode
            ) {
                val relevanteInntektsopplysninger = filter { it.sykepengerFom >= (skjæringstidspunkt ?: periode.start.minusMonths(12)) }
                relevanteInntektsopplysninger.forEach { it.valider(aktivitetslogg, periode) }
                val harFlereArbeidsgivere = relevanteInntektsopplysninger.distinctBy { it.orgnummer }.size > 1
                val harFlereSkjæringstidspunkt = relevanteInntektsopplysninger.distinctBy { it.sykepengerFom }.size > 1
                if (harFlereArbeidsgivere && harFlereSkjæringstidspunkt) {
                    aktivitetslogg.error("Har inntekt på flere arbeidsgivere med forskjellig fom dato")
                }
                if (this.isNotEmpty() && skjæringstidspunkt == null) sikkerLogg.info("Har inntekt i Infotrygd og skjæringstidspunkt er null")
            }

            private fun List<Inntektsopplysning>.validerAntallInntekterPerArbeidsgiverPerDato(
                skjæringstidspunkt: LocalDate?,
                aktivitetslogg: IAktivitetslogg,
                periode: Periode
            ) {
                val harFlereInntekterPåSammeAGogDato = filter { it.sykepengerFom >= (skjæringstidspunkt ?: periode.start.minusMonths(12)) }
                    .groupBy { it.orgnummer to it.sykepengerFom }
                    .any { (_, inntekter) -> inntekter.size > 1 }
                if (harFlereInntekterPåSammeAGogDato) {
                    aktivitetslogg.warn("Det er lagt inn flere inntekter i Infotrygd med samme fom-dato, den seneste er lagt til grunn. Kontroller sykepengegrunnlaget.")
                }
            }

            internal fun addInntekter(
                person: Person,
                hendelse: PersonHendelse,
                inntektsopplysninger: List<Inntektsopplysning>
            ) {
                inntektsopplysninger.groupBy { it.orgnummer }
                    .forEach { (orgnummer, opplysninger) ->
                        person.lagreInntekter(orgnummer, opplysninger, hendelse)
                    }
            }

            internal fun List<Inntektsopplysning>.lagreInntekter(
                inntektshistorikk: Inntektshistorikk,
                hendelseId: UUID
            ) {
                inntektshistorikk {
                    reversed().forEach {
                        addInfotrygd(it.sykepengerFom, hendelseId, it.inntektPerMåned)
                    }
                }
            }
        }

        internal fun valider(aktivitetslogg: IAktivitetslogg, periode: Periode) {
            if (orgnummer.isBlank()) aktivitetslogg.error("Organisasjonsnummer for inntektsopplysning fra Infotrygd mangler")
            if (refusjonTom != null && periode.slutterEtter(refusjonTom)) aktivitetslogg.error("Refusjon fra Infotrygd opphører i eller før perioden")
            if (!refusjonTilArbeidsgiver) aktivitetslogg.error("Utbetaling skal gå rett til bruker")
        }
    }

    internal fun append(oldtid: Historie.Historikkbøtte) {
        utbetalinger.forEach {
            it.append(oldtid)
        }
    }

    internal fun grunnlagsdata() = VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag()

    sealed class Infotrygdperiode(fom: LocalDate, tom: LocalDate) {
        internal companion object {
            fun sorter(liste: List<Infotrygdperiode>) = liste.sortedBy { it.periode.start }
        }

        protected val periode = Periode(fom, tom)

        internal open fun tidslinje() = Utbetalingstidslinje()
        internal open fun sykdomstidslinje() = Sykdomstidslinje()

        internal open fun append(oldtid: Historie.Historikkbøtte) {}

        internal open fun valider(
            aktivitetslogg: IAktivitetslogg,
            bareOverlappende: Boolean,
            other: Periode,
            organisasjonsnummer: String
        ) {
        }

        abstract class Utbetalingsperiode(
            fom: LocalDate,
            tom: LocalDate,
            inntekt: Inntekt,
            private val grad: Prosentdel,
            private val orgnr: String
        ) : Infotrygdperiode(fom, tom) {
            private val inntekt = (inntekt / grad.ratio())

            override fun tidslinje() = Utbetalingstidslinje().apply {
                periode.forEach { dag(this, it, grad) }
            }

            override fun append(oldtid: Historie.Historikkbøtte) {
                oldtid.add(orgnr, tidslinje())
                oldtid.add(orgnr, sykdomstidslinje())
            }

            override fun valider(aktivitetslogg: IAktivitetslogg, bareOverlappende: Boolean, other: Periode, organisasjonsnummer: String) {
                if (periode.overlapperMed(other)) aktivitetslogg.error("Hele eller deler av perioden er utbetalt i Infotrygd")
            }

            override fun sykdomstidslinje() =
                Sykdomstidslinje.sykedager(periode.start, periode.endInclusive, grad, SykdomstidslinjeHendelse.Hendelseskilde.INGEN)

            private fun dag(utbetalingstidslinje: Utbetalingstidslinje, dato: LocalDate, grad: Prosentdel) {
                if (dato.erHelg()) utbetalingstidslinje.addHelg(dato, Økonomi.sykdomsgrad(grad).inntekt(Inntekt.INGEN, skjæringstidspunkt = dato))
                else utbetalingstidslinje.addNAVdag(dato, Økonomi.sykdomsgrad(grad).inntekt(inntekt, skjæringstidspunkt = dato))
            }

            internal companion object {
                fun valider(
                    liste: List<Infotrygdperiode>,
                    aktivitetslogg: IAktivitetslogg,
                    bareOverlappende: Boolean,
                    periode: Periode,
                    organisasjonsnummer: String
                ): IAktivitetslogg {
                    liste.forEach { it.valider(aktivitetslogg, bareOverlappende, periode, organisasjonsnummer) }
                    return aktivitetslogg
                }
            }
        }

        class RefusjonTilArbeidsgiver(
            fom: LocalDate,
            tom: LocalDate,
            inntekt: Inntekt,
            grad: Prosentdel,
            orgnummer: String
        ) : Utbetalingsperiode(fom, tom, inntekt, grad, orgnummer)

        class ReduksjonArbeidsgiverRefusjon(
            fom: LocalDate,
            tom: LocalDate,
            inntekt: Inntekt,
            grad: Prosentdel,
            orgnummer: String
        ) : Utbetalingsperiode(fom, tom, inntekt, grad, orgnummer)

        class Utbetaling(fom: LocalDate, tom: LocalDate, inntekt: Inntekt, grad: Prosentdel, orgnummer: String) :
            Utbetalingsperiode(fom, tom, inntekt, grad, orgnummer)

        class ReduksjonMedlem(fom: LocalDate, tom: LocalDate, inntekt: Inntekt, grad: Prosentdel, orgnummer: String) :
            Utbetalingsperiode(fom, tom, inntekt, grad, orgnummer)

        class Ferie(fom: LocalDate, tom: LocalDate) : Infotrygdperiode(fom, tom) {
            override fun tidslinje() = Utbetalingstidslinje()
                .apply { periode.forEach { addFridag(it, Økonomi.ikkeBetalt().inntekt(Inntekt.INGEN, skjæringstidspunkt = it)) } }

            override fun sykdomstidslinje() =
                Sykdomstidslinje.feriedager(periode.start, periode.endInclusive, SykdomstidslinjeHendelse.Hendelseskilde.INGEN)

            override fun append(oldtid: Historie.Historikkbøtte) {
                oldtid.add(tidslinje = tidslinje())
                oldtid.add(tidslinje = sykdomstidslinje())
            }
        }

        abstract class IgnorertPeriode(fom: LocalDate, tom: LocalDate) : Infotrygdperiode(fom, tom)

        class KontertRegnskap(fom: LocalDate, tom: LocalDate) : IgnorertPeriode(fom, tom)
        class Etterbetaling(fom: LocalDate, tom: LocalDate) : IgnorertPeriode(fom, tom)
        class Tilbakeført(fom: LocalDate, tom: LocalDate) : IgnorertPeriode(fom, tom)
        class Konvertert(fom: LocalDate, tom: LocalDate) : IgnorertPeriode(fom, tom)
        class Sanksjon(fom: LocalDate, tom: LocalDate) : IgnorertPeriode(fom, tom)
        class Opphold(fom: LocalDate, tom: LocalDate) : IgnorertPeriode(fom, tom)
        class Ukjent(fom: LocalDate, tom: LocalDate) : IgnorertPeriode(fom, tom) {
            override fun valider(
                aktivitetslogg: IAktivitetslogg,
                bareOverlappende: Boolean,
                other: Periode,
                organisasjonsnummer: String
            ) {
                if (bareOverlappende) return
                if (periode.endInclusive < other.start.minusDays(18)) return
                aktivitetslogg.warn(
                    "Perioden er lagt inn i Infotrygd, men ikke utbetalt. Fjern fra Infotrygd hvis det utbetales via speil.",
                    this::class.simpleName
                )
            }
        }

        class Ugyldig(private val fom: LocalDate?, private val tom: LocalDate?) :
            IgnorertPeriode(LocalDate.MIN, LocalDate.MAX) {
            override fun valider(
                aktivitetslogg: IAktivitetslogg,
                bareOverlappende: Boolean,
                other: Periode,
                organisasjonsnummer: String
            ) {
                if (bareOverlappende) return
                val tekst = when {
                    fom == null || tom == null -> "mangler fom- eller tomdato"
                    fom > tom -> "fom er nyere enn tom"
                    else -> null
                }
                aktivitetslogg.error(
                    "Det er en ugyldig utbetalingsperiode i Infotrygd%s",
                    tekst?.let { " ($it)" } ?: "")
            }
        }
    }
}
