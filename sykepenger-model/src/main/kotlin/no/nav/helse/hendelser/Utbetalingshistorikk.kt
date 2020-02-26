package no.nav.helse.hendelser

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.Inntekthistorikk
import no.nav.helse.sykdomstidslinje.dag.erHelg
import no.nav.helse.utbetalingstidslinje.Utbetalingslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.time.LocalDate
import java.util.*

class Utbetalingshistorikk(
    private val utbetalinger: List<Periode>,
    private val ukjentePerioder: List<JsonNode>,
    private val inntektshistorikk: List<Inntektsopplysning>,
    private val graderingsliste: List<Graderingsperiode>,
    private val aktivitetslogger: Aktivitetslogger,
    private val aktivitetslogg: Aktivitetslogg
) {

    private val sisteFraværsdag: LocalDate? = utbetalinger.maxBy { it.tom }?.tom


    internal fun utbetalingstidslinje() = this.utbetalinger
        .map { it.toTidslinje(graderingsliste, aktivitetslogger) }
        .fold(Utbetalingstidslinje(), Utbetalingstidslinje::plus)

    internal fun sisteFraværsdag() = sisteFraværsdag

    internal fun valider(): Aktivitetslogger {
        utbetalinger.forEach { it.valider(this, aktivitetslogger) }
        inntektshistorikk.forEach { it.valider(aktivitetslogger) }
        if (ukjentePerioder.isNotEmpty()) {
            aktivitetslogger.errorOld("Utbetalingshistorikk inneholder ukjente perioder")
        }
        return aktivitetslogger
    }

    internal fun kopierAktiviteterTil(aktivitetslogger: Aktivitetslogger) {
        aktivitetslogger.addAll(this.aktivitetslogger, "Sykepengehistorikk")
    }

    internal fun addInntekter(hendelseId: UUID, inntekthistorikk: Inntekthistorikk) {
        this.inntektshistorikk.forEach { it.addInntekter(hendelseId, inntekthistorikk) }

    }

    class Inntektsopplysning(
        private val sykepengerFom: LocalDate,
        private val inntektPerMåned: Int,
        private val orgnummer: String
    ) {

        internal fun valider(aktivitetslogger: Aktivitetslogger) {
            if (orgnummer.isBlank()) {
                aktivitetslogger.errorOld("Organisasjonsnummer for inntektsopplysning mangler")
            }
        }

        internal fun addInntekter(hendelseId: UUID, inntekthistorikk: Inntekthistorikk) {
            inntekthistorikk.add(sykepengerFom, hendelseId, inntektPerMåned.toBigDecimal())
        }
    }

    class Graderingsperiode(private val fom: LocalDate, private val tom: LocalDate, internal val grad: Double) {
        internal fun datoIPeriode(dato: LocalDate) =
            dato.isAfter(fom.minusDays(1)) && dato.isBefore(tom.plusDays(1))
    }

    sealed class Periode(internal val fom: LocalDate, internal val tom: LocalDate, internal val dagsats: Int) {
        internal open fun toTidslinje(graderingsliste: List<Graderingsperiode>, aktivitetslogger: Aktivitetslogger): Utbetalingstidslinje {
            aktivitetslogger.severeOld("Kan ikke hente ut utbetalingslinjer for perioden %s", this::class.simpleName)
        }

        open fun valider(historikk: Utbetalingshistorikk, aktivitetslogger: Aktivitetslogger) {
            aktivitetslogger.errorOld("Utbetalingsperioden %s (fra Infotrygd) er ikke støttet", this::class.simpleName)
        }

        class RefusjonTilArbeidsgiver(
            fom: LocalDate,
            tom: LocalDate,
            dagsats: Int
        ) : Periode(fom, tom, dagsats) {

            private fun List<Graderingsperiode>.finnGradForUtbetalingsdag(dag: LocalDate) =
                this.find { it.datoIPeriode(dag) }?.grad ?: Double.NaN

            override fun toTidslinje(graderingsliste: List<Graderingsperiode>, aktivitetslogger: Aktivitetslogger) = Utbetalingstidslinje().apply {
                fom.datesUntil(tom.plusDays(1)).forEach {
                    if (it.erHelg()) this.addHelg(
                        0.0,
                        it,
                        graderingsliste.finnGradForUtbetalingsdag(it)
                    ) else this.addNAVdag(dagsats.toDouble(), it, graderingsliste.finnGradForUtbetalingsdag(it))
                }
            }

            override fun valider(historikk: Utbetalingshistorikk, aktivitetslogger: Aktivitetslogger) {
                if (fom > tom) aktivitetslogger.errorOld("Utbetalingsperioder kan ikke ha en FOM etter TOM")
            }
        }

        class Utbetaling(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats)
        class ReduksjonMedlem(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats)
        class Etterbetaling(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats)
        class KontertRegnskap(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats)
        class ReduksjonArbeidsgiverRefusjon(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats)
        class Tilbakeført(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats)
        class Konvertert(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats)
        class Ferie(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats)
        class Opphold(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats)
        class Sanksjon(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats)
        class Ukjent(fom: LocalDate, tom: LocalDate, dagsats: Int) : Periode(fom, tom, dagsats)
    }
}
