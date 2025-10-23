package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.dto.PeriodeUtenNavAnsvarDto
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.nesteDag

data class PeriodeUtenNavAnsvar(
    // perioden som dekker første kjente dag til siste kjente dag
    val omsluttendePeriode: Periode,
    // dager som tolkes som dager nav ikke har ansvar (arbeidsgiverperiode / ventetid)
    val dagerUtenAnsvar: List<Periode>,
    // hvorvidt perioden nav ikke har ansvar for er ferdig avklart
    val ferdigAvklart: Boolean
) {
    fun utvideMed(
        dato: LocalDate,
        dagUtenAnsvar: LocalDate? = null,
        ferdigAvklart: Boolean? = null
    ): PeriodeUtenNavAnsvar {
        return this.copy(
            omsluttendePeriode = this.omsluttendePeriode.oppdaterTom(dato),
            dagerUtenAnsvar = this.dagerUtenAnsvar.leggTil(dagUtenAnsvar),
            ferdigAvklart = ferdigAvklart ?: this.ferdigAvklart
        )
    }

    fun dto() = PeriodeUtenNavAnsvarDto(
        omsluttendePeriode = omsluttendePeriode.dto(),
        dagerUtenAnsvar = dagerUtenAnsvar.map { it.dto() },
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

        internal fun Iterable<PeriodeUtenNavAnsvar>.finn(periode: Periode) = lastOrNull { periodeUtenNavAnsvar ->
            periode.overlapperMed(periodeUtenNavAnsvar.omsluttendePeriode)
        }

        internal fun gjenopprett(dto: PeriodeUtenNavAnsvarDto) =
            PeriodeUtenNavAnsvar(
                omsluttendePeriode = Periode.gjenopprett(dto.omsluttendePeriode),
                dagerUtenAnsvar = dto.dagerUtenAnsvar.map { Periode.gjenopprett(it) },
                ferdigAvklart = dto.ferdigAvklart
            )
    }
}
