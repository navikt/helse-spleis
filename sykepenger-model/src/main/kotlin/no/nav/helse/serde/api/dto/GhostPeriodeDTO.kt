package no.nav.helse.serde.api.dto

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.forrigeDag
import no.nav.helse.nesteDag

data class GhostPeriodeDTO(
    val id: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val skjæringstidspunkt: LocalDate,
    val vilkårsgrunnlagId: UUID,
    val deaktivert: Boolean
) {

    internal fun brytOpp(tidslinjeperiode: ClosedRange<LocalDate>) = when {
        tidslinjeperiode.erInni(this) -> listOf(this.til(tidslinjeperiode), this.fra(tidslinjeperiode))
        tidslinjeperiode.overlapperMedHale(this) -> listOf(this.til(tidslinjeperiode))
        tidslinjeperiode.overlapperMedSnute(this) -> listOf(this.fra(tidslinjeperiode))
        else -> emptyList()
    }

    internal fun til(other: ClosedRange<LocalDate>) = copy(
        id = UUID.randomUUID(),
        tom = other.start.forrigeDag
    )
    internal fun fra(other: ClosedRange<LocalDate>) = copy(
        id = UUID.randomUUID(),
        fom = other.endInclusive.nesteDag
    )

    private fun ClosedRange<LocalDate>.erInni(other: GhostPeriodeDTO) =
        this.start > other.fom && this.endInclusive < other.tom
    private fun ClosedRange<LocalDate>.overlapperMedHale(other: GhostPeriodeDTO) =
        this.start > other.fom && this.endInclusive >= other.tom

    private fun ClosedRange<LocalDate>.overlapperMedSnute(other: GhostPeriodeDTO) =
        this.start <= other.fom && this.endInclusive < other.tom
}