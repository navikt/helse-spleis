package no.nav.helse.person

import java.time.LocalDate
import java.util.*

class GhostPerioder(val historikkInnslagId: UUID, val ghostPerioder: List<GhostPeriode>) {
    class GhostPeriode(val fom: LocalDate, val tom: LocalDate, val skjæringstidspunkt: LocalDate, val deaktivert: Boolean)
}
