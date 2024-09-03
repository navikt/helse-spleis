package no.nav.helse.spleis.speil.dto

import java.time.LocalDate
import java.util.UUID

data class NyttInntektsforholdPeriodeDTO(
    val id: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val vilkårsgrunnlagId: UUID,
)