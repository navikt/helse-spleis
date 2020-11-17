package no.nav.helse.hendelser

import no.nav.helse.Grunnbeløp
import no.nav.helse.hendelser.Periode.Companion.slåSammen
import no.nav.helse.person.*
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingstidslinje.Historie
import no.nav.helse.utbetalingstidslinje.Oldtidsutbetalinger
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*
import kotlin.math.roundToInt
import no.nav.helse.hendelser.Periode as ModellPeriode

class Utbetalingshistorikk(
    meldingsreferanseId: UUID,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    internal val vedtaksperiodeId: String,
    utbetalinger: List<Periode>,
    private val inntektshistorikk: List<Inntektsopplysning>,
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : ArbeidstakerHendelse(meldingsreferanseId, aktivitetslogg) {
    private val utbetalinger = Periode.sorter(utbetalinger)

    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    override fun organisasjonsnummer() = organisasjonsnummer

    internal fun valider(periode: no.nav.helse.hendelser.Periode, periodetype: Periodetype): Aktivitetslogg {
        Periode.Utbetalingsperiode.valider(utbetalinger, aktivitetslogg, periode, organisasjonsnummer)
        Inntektsopplysning.valider(inntektshistorikk, aktivitetslogg, periode)
        return aktivitetslogg
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

    internal fun historiskeTidslinjer() =
        Periode.Utbetalingsperiode.historiskePerioder(utbetalinger, inntektshistorikk)
            .slåSammen()
            .map { periode ->
                Sykdomstidslinje.sykedager(periode.start, periode.endInclusive, 100, SykdomstidslinjeHendelse.Hendelseskilde.INGEN)
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
                aktivitetslogg: Aktivitetslogg,
                periode: no.nav.helse.hendelser.Periode
            ) {
                liste
                    .filter { it.sykepengerFom >= periode.start.minusMonths(12) }
                    .distinctBy { it.orgnummer }
                    .onEach { it.valider(aktivitetslogg, periode) }
                    .also {
                        if (it.size > 1) aktivitetslogg.error("Har inntekt fra flere arbeidsgivere i Infotrygd innen 12 måneder fra perioden")
                    }
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

            fun finnNærmeste(organisasjonsnummer: String, periode: ModellPeriode, inntektshistorikk: List<Inntektsopplysning>) =
                inntektshistorikk
                    .filter { it.orgnummer == organisasjonsnummer }
                    .filter { it.sykepengerFom <= periode.start }
                    .maxOfOrNull { it.sykepengerFom }
                    .also { if (it == null) sikkerLogg.info("Har utbetaling, men ikke inntektsopplysning, for $organisasjonsnummer") }
        }

        internal fun valider(aktivitetslogg: Aktivitetslogg, periode: no.nav.helse.hendelser.Periode) {
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
            inntektshistorikk.invoke {
                addInfotrygd(
                    sykepengerFom,
                    hendelseId,
                    inntektPerMåned
                )
            }
        }
    }

    internal fun append(oldtid: Oldtidsutbetalinger) {
        utbetalinger.forEach {
            it.append(oldtid)
        }
    }

    internal fun append(oldtid: Historie.Historikkbøtte) {
        utbetalinger.forEach {
            it.append(oldtid)
        }
    }

    sealed class Periode(fom: LocalDate, tom: LocalDate) {
        internal companion object {
            fun sorter(liste: List<Periode>) = liste.sortedBy { it.periode.start }
        }

        protected val periode = ModellPeriode(fom, tom)

        internal open fun tidslinje() = Utbetalingstidslinje()
        internal open fun sykdomstidslinje() = Sykdomstidslinje()

        internal open fun append(oldtid: Oldtidsutbetalinger) {}
        internal open fun append(oldtid: Historie.Historikkbøtte) {}

        internal open fun valider(
            aktivitetslogg: Aktivitetslogg,
            other: no.nav.helse.hendelser.Periode,
            organisasjonsnummer: String
        ) {}

        abstract class Utbetalingsperiode(
            fom: LocalDate,
            tom: LocalDate,
            private val beløp: Int,
            private val grad: Int,
            private val orgnr: String
        ) : Periode(fom, tom) {
            private val ugradertBeløp = ((beløp * 100) / grad.toDouble()).roundToInt().daglig
            private val maksDagsats = Grunnbeløp.`6G`.dagsats(fom, LocalDate.now()) == ugradertBeløp

            override fun tidslinje() = Utbetalingstidslinje().apply {
                periode.forEach { dag(this, it, grad.toDouble()) }
            }

            override fun append(oldtid: Historie.Historikkbøtte) {
                oldtid.add(orgnr, tidslinje())
                oldtid.add(orgnr, sykdomstidslinje())
            }

            override fun valider(
                aktivitetslogg: Aktivitetslogg,
                other: no.nav.helse.hendelser.Periode,
                organisasjonsnummer: String
            ) {
                if (organisasjonsnummer != orgnr) return
                if (periode.overlapperMed(other)) aktivitetslogg.error("Hele eller deler av perioden er utbetalt i Infotrygd")
            }

            override fun sykdomstidslinje() =
                Sykdomstidslinje.sykedager(periode.start, periode.endInclusive, grad, SykdomstidslinjeHendelse.Hendelseskilde.INGEN)

            private fun dag(utbetalingstidslinje: Utbetalingstidslinje, dato: LocalDate, grad: Double) {
                if (dato.erHelg()) utbetalingstidslinje.addHelg(dato, Økonomi.sykdomsgrad(grad.prosent).inntekt(Inntekt.INGEN))
                else utbetalingstidslinje.addNAVdag(dato, Økonomi.sykdomsgrad(grad.prosent).inntekt(ugradertBeløp))
            }

            internal companion object {

                fun historiskePerioder(perioder: List<Periode>, inntektshistorikk: List<Inntektsopplysning>) =
                    perioder.filterIsInstance<Utbetalingsperiode>()
                        .map {
                            it.periode.oppdaterFom(
                                Inntektsopplysning.finnNærmeste(
                                    it.orgnr,
                                    it.periode,
                                    inntektshistorikk
                                ) ?: it.periode.start
                            )
                        }

                fun valider(
                    liste: List<Periode>,
                    aktivitetslogg: Aktivitetslogg,
                    periode: no.nav.helse.hendelser.Periode,
                    organisasjonsnummer: String
                ): Aktivitetslogg {
                    if (liste.harForegåendeFraAnnenArbeidsgiver(periode, organisasjonsnummer)) {
                        aktivitetslogg.error("Det finnes en tilstøtende utbetalt periode i Infotrygd med et annet organisasjonsnummer enn denne vedtaksperioden.")
                        return aktivitetslogg
                    }

                    liste.onEach { it.valider(aktivitetslogg, periode, organisasjonsnummer) }
                    if (liste.harHistoriskeSammenhengendePerioderMedEndring())
                        aktivitetslogg.info("Dagsatsen har endret seg minst én gang i en historisk, sammenhengende periode i Infotrygd")
                    return aktivitetslogg
                }

                private fun List<Periode>.harForegåendeFraAnnenArbeidsgiver(
                    periode: no.nav.helse.hendelser.Periode,
                    organisasjonsnummer: String
                ) =
                    this
                        .filterIsInstance<Utbetalingsperiode>()
                        .filter { it.periode.erRettFør(periode) }
                        .any { it.orgnr != organisasjonsnummer }

                private fun List<Periode>.harHistoriskeSammenhengendePerioderMedEndring() =
                    this
                        .filterIsInstance<Utbetalingsperiode>()
                        .zipWithNext { left, right -> erTilstøtendeMedEndring(left, right) }
                        .any { it }

                private fun erTilstøtendeMedEndring(left: Utbetalingsperiode, right: Utbetalingsperiode) =
                    left.periode.erRettFør(right.periode) && left.ugradertBeløp != right.ugradertBeløp && !left.maksDagsats && !right.maksDagsats
            }
        }

        class RefusjonTilArbeidsgiver(
            fom: LocalDate,
            tom: LocalDate,
            dagsats: Int,
            grad: Int,
            private val orgnummer: String
        ) : Utbetalingsperiode(fom, tom, dagsats, grad, orgnummer) {
            override fun append(oldtid: Oldtidsutbetalinger) {
                oldtid.add(orgnummer, tidslinje())
            }
        }

        class ReduksjonArbeidsgiverRefusjon(
            fom: LocalDate,
            tom: LocalDate,
            dagsats: Int,
            grad: Int,
            private val orgnummer: String
        ) : Utbetalingsperiode(fom, tom, dagsats, grad, orgnummer) {
            override fun append(oldtid: Oldtidsutbetalinger) {
                oldtid.add(orgnummer, tidslinje())
            }
        }

        class Utbetaling(fom: LocalDate, tom: LocalDate, dagsats: Int, grad: Int, orgnummer: String) :
            Utbetalingsperiode(fom, tom, dagsats, grad, orgnummer) {
            override fun append(oldtid: Oldtidsutbetalinger) {
                oldtid.add(tidslinje = tidslinje())
            }
        }

        class ReduksjonMedlem(fom: LocalDate, tom: LocalDate, dagsats: Int, grad: Int, orgnummer: String) :
            Utbetalingsperiode(fom, tom, dagsats, grad, orgnummer) {
            override fun append(oldtid: Oldtidsutbetalinger) {
                oldtid.add(tidslinje = tidslinje())
            }
        }

        class Ferie(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun tidslinje() = Utbetalingstidslinje()
                .apply { periode.forEach { addFridag(it, Økonomi.ikkeBetalt().inntekt(Inntekt.INGEN)) } }

            override fun sykdomstidslinje() =
                Sykdomstidslinje.feriedager(periode.start, periode.endInclusive, SykdomstidslinjeHendelse.Hendelseskilde.INGEN)

            override fun append(oldtid: Oldtidsutbetalinger) {
                oldtid.add(tidslinje = tidslinje())
            }

            override fun append(oldtid: Historie.Historikkbøtte) {
                oldtid.add(tidslinje = tidslinje())
                oldtid.add(tidslinje = sykdomstidslinje())
            }
        }

        abstract class IgnorertPeriode(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {}

        class KontertRegnskap(fom: LocalDate, tom: LocalDate) : IgnorertPeriode(fom, tom)
        class Etterbetaling(fom: LocalDate, tom: LocalDate) : IgnorertPeriode(fom, tom)
        class Tilbakeført(fom: LocalDate, tom: LocalDate) : IgnorertPeriode(fom, tom)
        class Konvertert(fom: LocalDate, tom: LocalDate) : IgnorertPeriode(fom, tom)
        class Sanksjon(fom: LocalDate, tom: LocalDate) : IgnorertPeriode(fom, tom)
        class Opphold(fom: LocalDate, tom: LocalDate) : IgnorertPeriode(fom, tom)
        class Ukjent(fom: LocalDate, tom: LocalDate) : IgnorertPeriode(fom, tom) {
            override fun valider(
                aktivitetslogg: Aktivitetslogg,
                other: no.nav.helse.hendelser.Periode,
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
                aktivitetslogg: Aktivitetslogg,
                other: no.nav.helse.hendelser.Periode,
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
