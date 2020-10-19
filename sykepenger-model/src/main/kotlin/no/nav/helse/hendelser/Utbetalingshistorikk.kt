package no.nav.helse.hendelser

import no.nav.helse.Grunnbeløp
import no.nav.helse.hendelser.Periode.Companion.slåSammen
import no.nav.helse.hendelser.Utbetalingshistorikk.Inntektsopplysning.Companion.utvidTilbake
import no.nav.helse.hendelser.Utbetalingshistorikk.Periode.Companion.tilModellPerioder
import no.nav.helse.person.*
import no.nav.helse.person.Periodetype.FORLENGELSE
import no.nav.helse.person.Periodetype.INFOTRYGDFORLENGELSE
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse.Hendelseskilde.Companion.INGEN
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingstidslinje.Oldtidsutbetalinger
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
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
        Periode.Utbetalingsperiode.valider(utbetalinger, aktivitetslogg, periode, organisasjonsnummer, periodetype)
        Inntektsopplysning.valider(inntektshistorikk, aktivitetslogg, periode)
        return aktivitetslogg
    }

    internal fun addInntekter(hendelseId: UUID, organisasjonsnummer: String, inntektshistorikk: Inntektshistorikk) {
        this.inntektshistorikk.forEach { it.addInntekter(hendelseId, organisasjonsnummer, inntektshistorikk) }
    }

    internal fun addInntekter(hendelseId: UUID, organisasjonsnummer: String, inntektshistorikk: InntektshistorikkVol2) {
        this.inntektshistorikk.forEach { it.addInntekter(hendelseId, organisasjonsnummer, inntektshistorikk) }
    }

    fun addInntekter(person: Person, ytelser: Ytelser) {
        Inntektsopplysning.addInntekter(person, ytelser, inntektshistorikk)
    }

    internal fun historiskeTidslinjer(organisasjonsnummer: String? = null) =
        Periode.Utbetalingsperiode.historiskePerioder(organisasjonsnummer, utbetalinger, inntektshistorikk).map { periode ->
            Sykdomstidslinje.sykedager(periode.start, periode.endInclusive, 100, SykdomstidslinjeHendelse.Hendelseskilde.INGEN)
        }

    internal fun oppdaterBeregningsdatoer(perioder: List<LocalDate>, sluttdato: LocalDate): List<LocalDate> {
        val infotrygdperioder = utbetalinger.tilModellPerioder().utvidTilbake(inntektshistorikk)
        return (infotrygdperioder + perioder.map { it til sluttdato })
            .slåSammen() // TODO: hva gjør slåSammen, og trengs Periode-objekter (siden sluttdato er alltid samme, og man uansett bare vil ha startdatoen)?
            .map { it.start }
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

            internal fun List<ModellPeriode>.utvidTilbake(perioder: List<Inntektsopplysning>) =
                map {
                    it.oppdaterFom(it.start.nærmesteFom(perioder) ?: it.start)
                }

            private fun LocalDate.nærmesteFom(perioder: List<Inntektsopplysning>) =
                perioder
                    .maxBy { it.sykepengerFom <= this }
                    ?.sykepengerFom
                    .also { if (it == null) sikkerLogg.info("Har utbetaling, men ikke inntektsopplysning, for orgnumre ${perioder.map { it.orgnummer }}") }

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
                    .maxBy { it.sykepengerFom }
                    ?.sykepengerFom
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

    sealed class Periode(fom: LocalDate, tom: LocalDate) {
        internal companion object {
            fun sorter(liste: List<Periode>) = liste.sortedBy { it.periode.start }
            internal fun List<Periode>.tilModellPerioder() =
                map { it.periode }
        }

        protected val periode = ModellPeriode(fom, tom)

        internal open fun tidslinje() = Utbetalingstidslinje()

        internal open fun append(oldtid: Oldtidsutbetalinger) {}

        internal open fun valider(aktivitetslogg: Aktivitetslogg, other: no.nav.helse.hendelser.Periode) {
            if (periode.overlapperMed(other)) aktivitetslogg.error("Hele eller deler av perioden er utbetalt i Infotrygd")
        }

        abstract class Utbetalingsperiode(
            fom: LocalDate,
            tom: LocalDate,
            private val beløp: Int,
            private val grad: Int,
            private val orgnr: String
        ) : Periode(fom, tom) {
            private val ugradertBeløp = ((beløp * 100) / grad.toDouble()).roundToInt().daglig
            private val maksDagsats = Grunnbeløp.`6G`.dagsats(fom) == ugradertBeløp

            override fun tidslinje() = Utbetalingstidslinje().apply {
                periode.forEach { dag(this, it, grad.toDouble()) }
            }

            private fun dag(utbetalingstidslinje: Utbetalingstidslinje, dato: LocalDate, grad: Double) {
                if (dato.erHelg()) utbetalingstidslinje.addHelg(dato, Økonomi.sykdomsgrad(grad.prosent).inntekt(Inntekt.INGEN))
                else utbetalingstidslinje.addNAVdag(dato, Økonomi.sykdomsgrad(grad.prosent).inntekt(ugradertBeløp))
            }

            internal companion object {

                fun historiskePerioder(organisasjonsnummer: String?, perioder: List<Periode>, inntektshistorikk: List<Inntektsopplysning>) =
                    perioder.filterIsInstance<Utbetalingsperiode>()
                        .filter { if (organisasjonsnummer == null) true else it.orgnr == organisasjonsnummer }
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
                    organisasjonsnummer: String,
                    periodetype: Periodetype
                ): Aktivitetslogg {
                    if (liste.harForegåendeFraAnnenArbeidsgiver(periode, organisasjonsnummer)) {
                        aktivitetslogg.error("Det finnes en tilstøtende utbetalt periode i Infotrygd med et annet organisasjonsnummer enn denne vedtaksperioden.")
                        return aktivitetslogg
                    }

                    liste.onEach { it.valider(aktivitetslogg, periode) }
                    if (liste.harHistoriskeSammenhengendePerioderMedEndring()) {
                        val melding = String.format(
                            "Dagsatsen har endret seg minst én gang i en historisk, sammenhengende periode i Infotrygd.%s",
                            if (liste.harForegående(periode)) " Direkte overgang fra Infotrygd; kontroller at sykepengegrunnlaget er riktig." else ""
                        )
                        if (periodetype in listOf(FORLENGELSE, INFOTRYGDFORLENGELSE)) aktivitetslogg.info(melding)
                        else aktivitetslogg.warn(melding)
                    }
                    return aktivitetslogg
                }

                private fun List<Periode>.harForegående(periode: no.nav.helse.hendelser.Periode) =
                    this
                        .filterIsInstance<Utbetalingsperiode>()
                        .any { it.periode.erRettFør(periode) }

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

            override fun append(oldtid: Oldtidsutbetalinger) {
                oldtid.add(tidslinje = tidslinje())
            }
        }

        abstract class IgnorertPeriode(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun valider(aktivitetslogg: Aktivitetslogg, other: no.nav.helse.hendelser.Periode) {}
        }

        class KontertRegnskap(fom: LocalDate, tom: LocalDate) : IgnorertPeriode(fom, tom)
        class Etterbetaling(fom: LocalDate, tom: LocalDate) : IgnorertPeriode(fom, tom)
        class Tilbakeført(fom: LocalDate, tom: LocalDate) : IgnorertPeriode(fom, tom)
        class Konvertert(fom: LocalDate, tom: LocalDate) : IgnorertPeriode(fom, tom)
        class Sanksjon(fom: LocalDate, tom: LocalDate) : IgnorertPeriode(fom, tom)
        class Opphold(fom: LocalDate, tom: LocalDate) : IgnorertPeriode(fom, tom)
        class Ukjent(fom: LocalDate, tom: LocalDate) : IgnorertPeriode(fom, tom) {
            override fun valider(aktivitetslogg: Aktivitetslogg, other: no.nav.helse.hendelser.Periode) {
                if (periode.endInclusive < other.start.minusDays(18)) return
                aktivitetslogg.warn(
                    "Perioden er lagt inn i Infotrygd, men ikke utbetalt. Fjern fra Infotrygd hvis det utbetales via speil.",
                    this::class.simpleName
                )
            }
        }

        class Ugyldig(private val fom: LocalDate?, private val tom: LocalDate?) :
            IgnorertPeriode(LocalDate.MIN, LocalDate.MAX) {
            override fun valider(aktivitetslogg: Aktivitetslogg, other: no.nav.helse.hendelser.Periode) {
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
