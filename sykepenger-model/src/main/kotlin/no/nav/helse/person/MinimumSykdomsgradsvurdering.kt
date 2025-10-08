package no.nav.helse.person

import no.nav.helse.dto.deserialisering.MinimumSykdomsgradVurderingInnDto
import no.nav.helse.dto.serialisering.MinimumSykdomsgradVurderingUtDto
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.utenPerioder

/**
 * Perioder saksbehandler har vurdert dithet at bruker har tapt nok arbeidstid til å ha rett på sykepenger,
 * tross < 20% tapt inntekt
 */
internal class MinimumSykdomsgradsvurdering(private val perioderMedMinimumSykdomsgradVurdertOK: MutableSet<Periode> = mutableSetOf()) {
    val perioder: Set<Periode> get() = perioderMedMinimumSykdomsgradVurdertOK.toSet()

    internal fun leggTil(nyePerioder: Set<Periode>) {
        val ny = perioderMedMinimumSykdomsgradVurdertOK.utenPerioder(nyePerioder) + nyePerioder
        perioderMedMinimumSykdomsgradVurdertOK.clear()
        perioderMedMinimumSykdomsgradVurdertOK.addAll(ny)
    }

    internal fun trekkFra(perioderSomIkkeErOkLikevel: Set<Periode>) {
        val ny = perioderMedMinimumSykdomsgradVurdertOK.utenPerioder(perioderSomIkkeErOkLikevel)
        perioderMedMinimumSykdomsgradVurdertOK.clear()
        perioderMedMinimumSykdomsgradVurdertOK.addAll(ny)
    }

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
