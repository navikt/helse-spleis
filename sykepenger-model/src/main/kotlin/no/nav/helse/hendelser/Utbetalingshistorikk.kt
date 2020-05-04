package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Inntekthistorikk
import no.nav.helse.sykdomstidslinje.dag.erHelg
import no.nav.helse.sykdomstidslinje.dag.harTilstøtende
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.math.roundToInt

class Utbetalingshistorikk(
    utbetalinger: List<Periode>,
    private val inntektshistorikk: List<Inntektsopplysning>,
    private val aktivitetslogg: Aktivitetslogg
) {
    private companion object {
        private const val TILSTREKKELIG_OPPHOLD_FOR_NY_248_GRENSE = 26 * 7
    }

    private val utbetalinger = Periode.sorter(utbetalinger)

    internal fun utbetalingstidslinje(førsteFraværsdag: LocalDate) = Periode.trim(this.utbetalinger, førsteFraværsdag)
        .map { it.tidslinje() }
        .fold(Utbetalingstidslinje(), Utbetalingstidslinje::plus)

    internal fun arbeidsgiverperiodeGjennomført(førsteDag: LocalDate) =
            utbetalingstidslinje(førsteDag)
            .sisteSykepengedag()
            ?.harTilstøtende(førsteDag) // checking for adjacency first; generalize more later
            ?: false

    internal fun valider(periode: no.nav.helse.hendelser.Periode): Aktivitetslogg {
        utbetalinger.onEach { it.valider(aktivitetslogg, periode) }
        Periode.Utbetalingsperiode.valider(utbetalinger, aktivitetslogg)
        if (inntektshistorikk.size > 1) aktivitetslogg.error("Har inntekt fra flere arbeidsgivere i Infotrygd")
        inntektshistorikk.forEach { it.valider(aktivitetslogg) }
        return aktivitetslogg
    }

    internal fun addInntekter(hendelseId: UUID, inntekthistorikk: Inntekthistorikk) {
        this.inntektshistorikk.forEach { it.addInntekter(hendelseId, inntekthistorikk) }
    }

    class Inntektsopplysning(
        private val sykepengerFom: LocalDate,
        private val inntektPerMåned: Int,
        private val orgnummer: String,
        private val refusjonTilArbeidsgiver: Boolean
    ) {

        internal fun valider(aktivitetslogg: Aktivitetslogg) {
            if (orgnummer.isBlank()) {
                aktivitetslogg.error("Organisasjonsnummer for inntektsopplysning fra Infotrygd mangler")
            }
            if (!refusjonTilArbeidsgiver) aktivitetslogg.error("Utbetaling skal gå rett til bruker")
        }

        internal fun addInntekter(hendelseId: UUID, inntekthistorikk: Inntekthistorikk) {
            inntekthistorikk.add(sykepengerFom, hendelseId, inntektPerMåned.toBigDecimal())
        }
    }

    sealed class Periode(fom: LocalDate, tom: LocalDate) {
        internal companion object {
            fun trim(liste: List<Periode>, dato: LocalDate): List<Periode> {
                var forrigePeriodeFom: LocalDate = dato
                return liste
                    .sortedByDescending { it.periode.start }
                    .filter { periode ->
                        (ChronoUnit.DAYS.between(periode.periode.start, forrigePeriodeFom) <= TILSTREKKELIG_OPPHOLD_FOR_NY_248_GRENSE).also {
                            if (it && periode.periode.start < forrigePeriodeFom) forrigePeriodeFom = periode.periode.start
                        }
                    }
            }

            fun sorter(liste: List<Periode>) = liste.sortedBy { it.periode.start }
        }

        protected val periode = no.nav.helse.hendelser.Periode(fom, tom)

        internal open fun tidslinje() = Utbetalingstidslinje()

        internal open fun valider(aktivitetslogg: Aktivitetslogg, other: no.nav.helse.hendelser.Periode) {
            if (periode.overlapperMed(other)) aktivitetslogg.error("Hele eller deler av perioden er utbetalt i Infotrygd")
        }

        abstract class Utbetalingsperiode(fom: LocalDate, tom: LocalDate, internal val dagsats: Int, internal val grad: Int) : Periode(fom, tom) {
            private val gradertSats = ((dagsats * 100) / grad.toDouble()).roundToInt()
            override fun tidslinje() = Utbetalingstidslinje().apply {
                periode.forEach { dag(this, it, grad.toDouble()) }
            }

            private fun dag(utbetalingstidslinje: Utbetalingstidslinje, dato: LocalDate, grad: Double) {
                if (dato.erHelg()) utbetalingstidslinje.addHelg(0.0, dato, grad)
                else utbetalingstidslinje.addNAVdag(dagsats.toDouble(), dato, grad)
            }

            internal companion object {
                fun valider(liste: List<Periode>, aktivitetslogg: Aktivitetslogg): Aktivitetslogg {
                    liste
                        .filterIsInstance<Utbetalingsperiode>()
                        .zipWithNext { left, right ->
                            if (left.periode.endInclusive.harTilstøtende(right.periode.start) && left.gradertSats != right.gradertSats) {
                                aktivitetslogg.warn("Infotrygd inneholder utbetalinger med varierende dagsats for en sammenhengende periode")
                            }
                        }
                    return aktivitetslogg
                }
            }
        }

        class RefusjonTilArbeidsgiver(fom: LocalDate, tom: LocalDate, dagsats: Int, grad: Int) : Utbetalingsperiode(fom, tom, dagsats, grad)
        class ReduksjonArbeidsgiverRefusjon(fom: LocalDate, tom: LocalDate, dagsats: Int, grad: Int) : Utbetalingsperiode(fom, tom, dagsats, grad)
        class Utbetaling(fom: LocalDate, tom: LocalDate, dagsats: Int, grad: Int) : Utbetalingsperiode(fom, tom, dagsats, grad)
        class ReduksjonMedlem(fom: LocalDate, tom: LocalDate, dagsats: Int, grad: Int) : Utbetalingsperiode(fom, tom, dagsats, grad)

        class Ferie(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun tidslinje() = Utbetalingstidslinje().apply { periode.forEach { addFridag(it) } }
        }

        abstract class IgnorertPeriode(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun valider(aktivitetslogg: Aktivitetslogg, other: no.nav.helse.hendelser.Periode) {}
        }
        class Etterbetaling(fom: LocalDate, tom: LocalDate) : IgnorertPeriode(fom, tom)
        class KontertRegnskap(fom: LocalDate, tom: LocalDate) : IgnorertPeriode(fom, tom)
        class Tilbakeført(fom: LocalDate, tom: LocalDate) : IgnorertPeriode(fom, tom)
        class Konvertert(fom: LocalDate, tom: LocalDate) : IgnorertPeriode(fom, tom)
        class Opphold(fom: LocalDate, tom: LocalDate) : IgnorertPeriode(fom, tom)
        class Sanksjon(fom: LocalDate, tom: LocalDate) : IgnorertPeriode(fom, tom)
        class Ukjent(fom: LocalDate, tom: LocalDate) : IgnorertPeriode(fom, tom) {
            override fun valider(aktivitetslogg: Aktivitetslogg, other: no.nav.helse.hendelser.Periode) {
                if (periode.endInclusive < other.start.minusDays(18)) return
                aktivitetslogg.warn(
                    "Det er en utbetalingsperiode som er lagt inn i Infotrygd uten at inntektsopplysninger er registrert.",
                    this::class.simpleName
                )
            }
        }
        class Ugyldig(private val fom: LocalDate?, private val tom: LocalDate?) : IgnorertPeriode(LocalDate.MIN, LocalDate.MAX) {
            override fun tidslinje(): Utbetalingstidslinje {
                throw IllegalStateException("Kan ikke hente ut utbetalingslinjer for en Ugyldig periode")
            }

            override fun valider(aktivitetslogg: Aktivitetslogg, other: no.nav.helse.hendelser.Periode) {
                val tekst = when {
                    fom == null || tom == null -> "mangler fom- eller tomdato"
                    fom > tom -> "fom er nyere enn tom"
                    else -> null
                }
                aktivitetslogg.error("Det er en ugyldig utbetalingsperiode i Infotrygd%s", tekst?.let { " ($it)" } ?: "")
            }
        }
    }
}
