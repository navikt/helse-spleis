package no.nav.helse.person

import no.nav.helse.dto.deserialisering.MinimumSykdomsgradVurderingInnDto
import no.nav.helse.dto.serialisering.MinimumSykdomsgradVurderingUtDto
import no.nav.helse.hendelser.Periode

/**
 * Perioder saksbehandler har vurdert dithet at bruker har tapt nok arbeidstid til å ha rett på sykepenger,
 * tross < 20% tapt inntekt
 */
internal class MinimumSykdomsgradsvurdering(private val perioderMedMinimumSykdomsgradVurdertOK: MutableSet<Periode> = mutableSetOf()) {
    internal fun leggTil(nyePerioder: Set<Periode>) {
        val ny = perioderMedMinimumSykdomsgradVurdertOK.fjernPerioder(nyePerioder) + nyePerioder
        perioderMedMinimumSykdomsgradVurdertOK.clear()
        perioderMedMinimumSykdomsgradVurdertOK.addAll(ny)
    }

    internal fun trekkFra(perioderSomIkkeErOkLikevel: Set<Periode>) {
        val ny = perioderMedMinimumSykdomsgradVurdertOK.fjernPerioder(perioderSomIkkeErOkLikevel)
        perioderMedMinimumSykdomsgradVurdertOK.clear()
        perioderMedMinimumSykdomsgradVurdertOK.addAll(ny)
    }

    private fun Collection<Periode>.fjernPerioder(nyePerioder: Set<Periode>) =
        this.flatMap { gammelPeriode ->
            nyePerioder.fold(listOf(gammelPeriode)) { result, nyPeriode ->
                result.flatMap { it.uten(nyPeriode) }
            }
        }

    internal fun fjernDagerSomSkalUtbetalesLikevel(tentativtAvslåtteDager: List<Periode>) =
        tentativtAvslåtteDager.fjernPerioder(perioderMedMinimumSykdomsgradVurdertOK)

    internal fun dto() = MinimumSykdomsgradVurderingUtDto(
        perioder = perioderMedMinimumSykdomsgradVurdertOK.map {
            it.dto()
        }
    )

    internal companion object {
        fun gjenopprett(dto: MinimumSykdomsgradVurderingInnDto) = MinimumSykdomsgradsvurdering(
            perioderMedMinimumSykdomsgradVurdertOK = dto.perioder.map { Periode.gjenopprett(it) }.toMutableSet()
        )
    }
}
