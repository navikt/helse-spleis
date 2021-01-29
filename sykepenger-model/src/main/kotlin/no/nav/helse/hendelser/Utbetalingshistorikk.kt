package no.nav.helse.hendelser

import no.nav.helse.Toggles
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

    internal fun valider(periode: Periode, skjæringstidspunkt: LocalDate?): IAktivitetslogg {
        Infotrygdperiode.Utbetalingsperiode.valider(utbetalinger, this, periode, organisasjonsnummer)
        Inntektsopplysning.valider(inntektshistorikk, this, skjæringstidspunkt, periode)
        return this
    }

    internal fun addInntekter(hendelseId: UUID, organisasjonsnummer: String, inntektshistorikk: Inntektshistorikk) {
        this.inntektshistorikk.forEach { it.addInntekter(hendelseId, organisasjonsnummer, inntektshistorikk) }
    }

    internal fun addInntekter(hendelseId: UUID, organisasjonsnummer: String, inntektshistorikk: InntektshistorikkVol2) {
        this.inntektshistorikk.forEach { it.addInntekter(hendelseId, organisasjonsnummer, inntektshistorikk) }
    }

    internal fun addInntekt(organisasjonsnummer: String, inntektshistorikk: Inntektshistorikk) {
        addInntekter(meldingsreferanseId(), organisasjonsnummer, inntektshistorikk)
    }

    fun addInntekter(person: Person, ytelser: Ytelser) {
        Inntektsopplysning.addInntekter(person, ytelser, inntektshistorikk)
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
                if(!Toggles.FlereArbeidsgivereOvergangITEnabled.enabled) {
                    liste.kontrollerAntallArbeidsgivere(periode, aktivitetslogg)
                }
            }

            private fun List<Inntektsopplysning>.kontrollerAntallArbeidsgivere(
                periode: Periode,
                aktivitetslogg: IAktivitetslogg
            ) {
                filter { it.sykepengerFom >= periode.start.minusMonths(12) }
                    .distinctBy { it.orgnummer }
                    .also {
                        if (it.size > 1) aktivitetslogg.error("Har inntekt fra flere arbeidsgivere i Infotrygd innen 12 måneder fra perioden")
                    }
            }

            private fun List<Inntektsopplysning>.validerAlleInntekterForSammenhengendePeriode(
                skjæringstidspunkt: LocalDate?,
                aktivitetslogg: IAktivitetslogg,
                periode: Periode
            ) {
                filter { it.sykepengerFom >= (skjæringstidspunkt ?: periode.start.minusMonths(12)) }
                    .forEach { it.valider(aktivitetslogg, periode) }
                if (this.isNotEmpty() && skjæringstidspunkt == null) sikkerLogg.info("Har inntekt i Infotrygd og skjæringstidspunkt er null")
            }

            internal fun addInntekter(
                person: Person,
                ytelser: Ytelser,
                inntektsopplysninger: List<Inntektsopplysning>
            ) {
                inntektsopplysninger.groupBy { it.orgnummer }
                    .forEach { (orgnummer, opplysninger) ->
                        person.lagreInntekter(orgnummer, opplysninger, ytelser)
                    }
            }

            internal fun List<Inntektsopplysning>.lagreInntekter(
                inntektshistorikk: InntektshistorikkVol2,
                hendelseId: UUID
            ) {
                inntektshistorikk {
                    forEach {
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

        internal fun addInntekter(hendelseId: UUID, organisasjonsnummer: String, inntektshistorikk: Inntektshistorikk) {
            if (organisasjonsnummer != orgnummer) return
            inntektshistorikk.add(
                sykepengerFom.minusDays(1), // Assuming salary is the day before the first sykedag
                hendelseId,
                inntektPerMåned,
                Inntektshistorikk.Inntektsendring.Kilde.INFOTRYGD
            )
        }

        internal fun addInntekter(
            hendelseId: UUID,
            organisasjonsnummer: String,
            inntektshistorikk: InntektshistorikkVol2
        ) {
            if (organisasjonsnummer != orgnummer) return
            inntektshistorikk {
                addInfotrygd(
                    sykepengerFom,
                    hendelseId,
                    inntektPerMåned
                )
            }
        }
    }

    internal fun append(oldtid: Historie.Historikkbøtte) {
        utbetalinger.forEach {
            it.append(oldtid)
        }
    }

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

            override fun valider(aktivitetslogg: IAktivitetslogg, other: Periode, organisasjonsnummer: String) {
                if (periode.overlapperMed(other)) aktivitetslogg.error("Hele eller deler av perioden er utbetalt i Infotrygd")
            }

            override fun sykdomstidslinje() =
                Sykdomstidslinje.sykedager(periode.start, periode.endInclusive, grad, SykdomstidslinjeHendelse.Hendelseskilde.INGEN)

            private fun dag(utbetalingstidslinje: Utbetalingstidslinje, dato: LocalDate, grad: Prosentdel) {
                if (dato.erHelg()) utbetalingstidslinje.addHelg(dato, Økonomi.sykdomsgrad(grad).inntekt(Inntekt.INGEN))
                else utbetalingstidslinje.addNAVdag(dato, Økonomi.sykdomsgrad(grad).inntekt(inntekt))
            }

            internal companion object {
                fun valider(
                    liste: List<Infotrygdperiode>,
                    aktivitetslogg: IAktivitetslogg,
                    periode: Periode,
                    organisasjonsnummer: String
                ): IAktivitetslogg {
                    if (!Toggles.FlereArbeidsgivereOvergangITEnabled.enabled) {
                        if (liste.harForegåendeFraAnnenArbeidsgiver(periode, organisasjonsnummer)) {
                            aktivitetslogg.error("Det finnes en tilstøtende utbetalt periode i Infotrygd med et annet organisasjonsnummer enn denne vedtaksperioden.")
                            return aktivitetslogg
                        }
                    }
                    liste.onEach { it.valider(aktivitetslogg, periode, organisasjonsnummer) }
                    return aktivitetslogg
                }

                private fun List<Infotrygdperiode>.harForegåendeFraAnnenArbeidsgiver(
                    periode: Periode,
                    organisasjonsnummer: String
                ) =
                    this
                        .filterIsInstance<Utbetalingsperiode>()
                        .filter { it.periode.erRettFør(periode) }
                        .any { it.orgnr != organisasjonsnummer }
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
                .apply { periode.forEach { addFridag(it, Økonomi.ikkeBetalt().inntekt(Inntekt.INGEN)) } }

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
                other: Periode,
                organisasjonsnummer: String
            ) {
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
                other: Periode,
                organisasjonsnummer: String
            ) {
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
