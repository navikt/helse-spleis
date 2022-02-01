package no.nav.helse.serde.api.sporing

import no.nav.helse.hendelser.Periode
import java.util.*

internal class VedtaksperiodeBuilder(private val id: UUID, private val periode: Periode, private var periodetype: PeriodetypeDTO = PeriodetypeDTO.GAP, private val forkastet: Boolean = false) {
    private val perioder = mutableListOf<VedtaksperiodeDTO>()

    fun periodetype(type: PeriodetypeDTO) = apply {
        periodetype = type
    }
    fun build() = VedtaksperiodeDTO(id, periode.start, periode.endInclusive, periodetype, forkastet)

}
