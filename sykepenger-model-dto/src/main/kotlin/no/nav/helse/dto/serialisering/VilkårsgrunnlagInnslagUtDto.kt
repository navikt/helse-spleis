package no.nav.helse.dto.serialisering

import java.time.LocalDateTime
import java.util.UUID

data class VilkårsgrunnlagInnslagUtDto(
    val id: UUID,
    val opprettet: LocalDateTime,
    val vilkårsgrunnlag: List<VilkårsgrunnlagUtDto>,
)
