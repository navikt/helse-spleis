package no.nav.helse.person

import java.time.LocalDate
import java.util.*

class GhostPeriode(
    val fom: LocalDate,
    val tom: LocalDate,
    val skjæringstidspunkt: LocalDate,
    val vilkårsgrunnlagHistorikkInnslagId: UUID?,
    val deaktivert: Boolean
)
