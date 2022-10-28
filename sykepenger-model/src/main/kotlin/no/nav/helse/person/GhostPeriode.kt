package no.nav.helse.person

import java.time.LocalDate
import java.util.UUID

class GhostPeriode(
    val fom: LocalDate,
    val tom: LocalDate,
    val skjæringstidspunkt: LocalDate,
    val vilkårsgrunnlagHistorikkInnslagId: UUID?,
    val vilkårsgrunnlagId: UUID?,
    val deaktivert: Boolean
)
