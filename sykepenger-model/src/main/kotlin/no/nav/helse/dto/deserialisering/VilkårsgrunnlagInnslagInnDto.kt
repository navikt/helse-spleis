package no.nav.helse.dto.deserialisering

import java.time.LocalDateTime
import java.util.UUID

data class VilkårsgrunnlagInnslagInnDto(
    val id: UUID,
    val opprettet: LocalDateTime,
    val vilkårsgrunnlag: List<VilkårsgrunnlagInnDto>
)