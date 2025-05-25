package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.nesteDag

data class Arbeidsgiverperioderesultat(
    // perioden som dekkes av arbeidsgiverperioden, fra første kjente dag til siste kjente dag
    val omsluttendePeriode: Periode,
    // dager som tolkes som del av arbeidsgiverperioden
    val arbeidsgiverperiode: List<Periode>,
    // perioder hvor det er registrert utbetaling
    val utbetalingsperioder: List<Periode>,
    // perioder hvor det er registrert oppholdsdager
    val oppholdsperioder: List<Periode>,
    // hvorvidt arbeidsgiverperiodetellingen er komplett, aka. 16 dager.
    // hvis tellingen er fullstendig så er arbeidsgiverperiode.last() dag nr. 16.
    val fullstendig: Boolean
) {

    fun utvideMed(
        dato: LocalDate,
        arbeidsgiverperiode: LocalDate? = null,
        utbetalingsperiode: LocalDate? = null,
        oppholdsperiode: LocalDate? = null,
        fullstendig: Boolean? = null
    ): Arbeidsgiverperioderesultat {
        return this.copy(
            omsluttendePeriode = this.omsluttendePeriode.oppdaterTom(dato),
            arbeidsgiverperiode = this.arbeidsgiverperiode.leggTil(arbeidsgiverperiode),
            utbetalingsperioder = this.utbetalingsperioder.leggTil(utbetalingsperiode),
            oppholdsperioder = this.oppholdsperioder.leggTil(oppholdsperiode),
            fullstendig = fullstendig ?: this.fullstendig
        )
    }

    internal fun somArbeidsgiverperiode(): Arbeidsgiverperiode {
        val agp =
            if (arbeidsgiverperiode.isEmpty() && utbetalingsperioder.isNotEmpty())
                Arbeidsgiverperiode.fiktiv(utbetalingsperioder.first().start)
            else
                Arbeidsgiverperiode(arbeidsgiverperiode)
        utbetalingsperioder.flatten().forEach { agp.utbetalingsdag(it) }
        oppholdsperioder.flatten().forEach { agp.oppholdsdag(it) }
        omsluttendePeriode.forEach { agp.kjentDag(it) }
        return agp
    }

    companion object {
        // utvider liste av perioder med ny dato. antar at listen er sortert i stigende rekkefølge,
        // og at <dato> må være nyere enn forrige periode. strekker altså -ikke- periodene eventuelt tilbake i tid, kun frem
        private fun List<Periode>.leggTil(dato: LocalDate?): List<Periode> = when {
            dato == null -> this
            // tom liste
            isEmpty() -> listOf(dato.somPeriode())
            // dagen er dekket av en tidligere periode
            dato <= last().endInclusive -> this
            // dagen utvider ikke siste datoperiode
            dato > last().endInclusive.nesteDag -> this + listOf(dato.somPeriode())
            // dagen utvider siste periode
            else -> oppdaterSiste(dato)
        }

        private fun List<Periode>.oppdaterSiste(dato: LocalDate) =
            toMutableList().apply { addLast(removeLast().oppdaterTom(dato)) }

        internal fun Iterable<Arbeidsgiverperioderesultat>.finn(periode: Periode) = lastOrNull { arbeidsgiverperiode ->
            periode.overlapperMed(arbeidsgiverperiode.omsluttendePeriode)
        }
    }
}
