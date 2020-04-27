package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Inntekthistorikk
import no.nav.helse.sykdomstidslinje.dag.erHelg
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

class Utbetalingshistorikk(
    private val utbetalinger: List<Periode>,
    private val inntektshistorikk: List<Inntektsopplysning>,
    private val graderingsliste: List<Graderingsperiode>,
    private val aktivitetslogg: Aktivitetslogg
) {
    private companion object {
        private const val TILSTREKKELIG_OPPHOLD_FOR_NY_248_GRENSE = 26 * 7
    }

    internal fun utbetalingstidslinje(førsteFraværsdag: LocalDate) = Periode.trim(this.utbetalinger, førsteFraværsdag)
        .map { it.tidslinje(graderingsliste, aktivitetslogg) }
        .fold(Utbetalingstidslinje(), Utbetalingstidslinje::plus)

    internal fun valider(periode: no.nav.helse.hendelser.Periode): Aktivitetslogg {
        utbetalinger.onEach { it.valider(aktivitetslogg, periode) }
        inntektshistorikk.forEach { it.valider(aktivitetslogg) }
        return aktivitetslogg
    }

    internal fun addInntekter(hendelseId: UUID, inntekthistorikk: Inntekthistorikk) {
        this.inntektshistorikk.forEach { it.addInntekter(hendelseId, inntekthistorikk) }
    }

    class Inntektsopplysning(
        private val sykepengerFom: LocalDate,
        private val inntektPerMåned: Int,
        private val orgnummer: String
    ) {

        internal fun valider(aktivitetslogg: Aktivitetslogg) {
            if (orgnummer.isBlank()) {
                aktivitetslogg.error("Organisasjonsnummer for inntektsopplysning fra Infotrygd mangler")
            }
        }

        internal fun addInntekter(hendelseId: UUID, inntekthistorikk: Inntekthistorikk) {
            inntekthistorikk.add(sykepengerFom, hendelseId, inntektPerMåned.toBigDecimal())
        }
    }

    class Graderingsperiode(fom: LocalDate, tom: LocalDate, internal val grad: Double) {
        internal companion object {
            fun gradForDag(liste: List<Graderingsperiode>, dag: LocalDate) = liste
                .find { dag in it.periode }
                ?.grad ?: Double.NaN
        }

        private val periode = no.nav.helse.hendelser.Periode(fom, tom)
    }

    sealed class Periode(fom: LocalDate, tom: LocalDate, internal val dagsats: Int) {
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
        }

        protected val periode = no.nav.helse.hendelser.Periode(fom, tom)

        internal open fun tidslinje(
            graderinger: List<Graderingsperiode>,
            aktivitetslogg: Aktivitetslogg
        ) = Utbetalingstidslinje().apply {
            periode.forEach { dag(this, it, Graderingsperiode.gradForDag(graderinger, it)) }
        }

        private fun dag(utbetalingstidslinje: Utbetalingstidslinje, dato: LocalDate, grad: Double) {
            if (dato.erHelg()) utbetalingstidslinje.addHelg(0.0, dato, grad)
            else utbetalingstidslinje.addNAVdag(dagsats.toDouble(), dato, grad)
        }

        open fun valider(aktivitetslogg: Aktivitetslogg, other: no.nav.helse.hendelser.Periode) {
            if (periode.overlapperMed(other)) aktivitetslogg.error("Hele eller deler av perioden er utbetalt i Infotrygd")
            if (periode.endInclusive >= other.start.minusDays(18)) aktivitetslogg.warn("Har utbetalt periode i Infotrygd nærmere enn 18 dager fra første dag")
        }

        class RefusjonTilArbeidsgiver(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats)
        class ReduksjonArbeidsgiverRefusjon(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats)
        class Utbetaling(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats)
        class ReduksjonMedlem(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats)

        class Ferie(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats) {
            override fun tidslinje(graderinger: List<Graderingsperiode>, aktivitetslogg: Aktivitetslogg) =
                Utbetalingstidslinje().apply { periode.forEach { addFridag(dagsats.toDouble(), it) } }
        }

        abstract class IgnorertPeriode(fom: LocalDate, tom: LocalDate) : Periode(fom, tom, Int.MIN_VALUE) {
            override fun tidslinje(graderinger: List<Graderingsperiode>, aktivitetslogg: Aktivitetslogg) =
                Utbetalingstidslinje()

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
            override fun tidslinje(graderinger: List<Graderingsperiode>, aktivitetslogg: Aktivitetslogg): Utbetalingstidslinje {
                aktivitetslogg.severe("Kan ikke hente ut utbetalingslinjer for perioden %s", this::class.simpleName)
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
