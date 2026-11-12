package no.nav.helse.dto

import java.util.UUID

data class VurderingerAvLøpendeVilkårDto(
    val perioder: List<VurdertPeriodeDto>,
) {
    data class VurdertPeriodeDto(
        val periode: PeriodeDto,
        val vurderinger: List<VurderingDto>,
    )

    data class VurderingDto(
        val vilkår: LøpendeVilkårDto,
        val vurderingId: UUID,
        val utfall: UtfallAvLøpendeVilkårDto,
    )
}

enum class LøpendeVilkårDto {
    Oppholdskrav,
}

enum class UtfallAvLøpendeVilkårDto {
    Oppfylt,
    IkkeOppfylt,
}
