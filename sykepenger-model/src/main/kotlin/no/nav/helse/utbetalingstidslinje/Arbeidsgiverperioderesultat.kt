package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.dto.ArbeidsgiverperioderesultatDto
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.nesteDag

data class Arbeidsgiverperioderesultat(
    // perioden som dekkes av arbeidsgiverperioden, fra første kjente dag til siste kjente dag
    val omsluttendePeriode: Periode,
    // dager som tolkes som del av arbeidsgiverperioden
    val arbeidsgiverperiode: List<Periode>,
    // hvorvidt arbeidsgiverperioden er ferdig avklart (enten fordi tellingen er fullstendig eller fordi agp er avklart i Infotrygd)
    val ferdigAvklart: Boolean
) {
    fun utvideMed(
        dato: LocalDate,
        arbeidsgiverperiode: LocalDate? = null,
        ferdigAvklart: Boolean? = null
    ): Arbeidsgiverperioderesultat {
        return this.copy(
            omsluttendePeriode = this.omsluttendePeriode.oppdaterTom(dato),
            arbeidsgiverperiode = this.arbeidsgiverperiode.leggTil(arbeidsgiverperiode),
            ferdigAvklart = ferdigAvklart ?: this.ferdigAvklart
        )
    }

    fun dto() = ArbeidsgiverperioderesultatDto(
        omsluttendePeriode = omsluttendePeriode.dto(),
        arbeidsgiverperiode = arbeidsgiverperiode.map { it.dto() },
        ferdigAvklart = ferdigAvklart
    )

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

        internal fun gjenopprett(dto: ArbeidsgiverperioderesultatDto) =
            Arbeidsgiverperioderesultat(
                omsluttendePeriode = Periode.gjenopprett(dto.omsluttendePeriode),
                arbeidsgiverperiode = dto.arbeidsgiverperiode.map { Periode.gjenopprett(it) },
                ferdigAvklart = dto.ferdigAvklart
            )
    }
}
