package no.nav.helse.spleis.speil.dto

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
        // trimmer ingenting
        tidslinjeperiode.erUtenfor(this) -> listOf(this)
        // trimmer i midten
        tidslinjeperiode.erInni(this) -> listOf(this.til(tidslinjeperiode), this.fra(tidslinjeperiode))
        // trimmer i slutten
        tidslinjeperiode.overlapperMedHale(this) -> listOf(this.til(tidslinjeperiode))
        // trimmer i starten
        tidslinjeperiode.overlapperMedSnute(this) -> listOf(this.fra(tidslinjeperiode))
        // trimmer hele
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

    private fun ClosedRange<LocalDate>.erUtenfor(other: GhostPeriodeDTO) =
        maxOf(this.start, other.fom) > minOf(this.endInclusive, other.tom)

    private fun ClosedRange<LocalDate>.erInni(other: GhostPeriodeDTO) =
        this.start > other.fom && this.endInclusive < other.tom
    private fun ClosedRange<LocalDate>.overlapperMedHale(other: GhostPeriodeDTO) =
        this.start > other.fom && this.endInclusive >= other.tom

    private fun ClosedRange<LocalDate>.overlapperMedSnute(other: GhostPeriodeDTO) =
        this.endInclusive < other.tom && this.endInclusive > other.fom
}