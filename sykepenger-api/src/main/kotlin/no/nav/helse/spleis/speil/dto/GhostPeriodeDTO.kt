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

    internal fun brytOpp(tidslinjeperiode: ClosedRange<LocalDate>) = brytOpp(this, fom, tom, tidslinjeperiode, GhostPeriodeDTO::til, GhostPeriodeDTO::fra)

    internal fun til(other: ClosedRange<LocalDate>) = copy(
        id = UUID.randomUUID(),
        tom = other.start.forrigeDag
    )
    internal fun fra(other: ClosedRange<LocalDate>) = copy(
        id = UUID.randomUUID(),
        fom = other.endInclusive.nesteDag
    )
}

internal fun <Ting> brytOpp(ting: Ting, fom: LocalDate, tom: LocalDate, tidslinjeperiode: ClosedRange<LocalDate>, tilFunksjon: Ting.(ClosedRange<LocalDate>) -> Ting, fraFunksjon: Ting.(ClosedRange<LocalDate>) -> Ting): List<Ting> = when {
    // trimmer ingenting
    tidslinjeperiode.erUtenfor(fom, tom) -> listOf(ting)
    // trimmer i midten
    tidslinjeperiode.erInni(fom, tom) -> listOf(ting.tilFunksjon(tidslinjeperiode), ting.fraFunksjon(tidslinjeperiode))
    // trimmer i slutten
    tidslinjeperiode.overlapperMedHale(fom, tom) -> listOf(ting.tilFunksjon(tidslinjeperiode))
    // trimmer i starten
    tidslinjeperiode.overlapperMedSnute(fom, tom) -> listOf(ting.fraFunksjon(tidslinjeperiode))
    // trimmer hele
    else -> emptyList()
}

internal fun ClosedRange<LocalDate>.erUtenfor(fom: LocalDate, tom: LocalDate) =
    maxOf(this.start, fom) > minOf(this.endInclusive, tom)


internal fun ClosedRange<LocalDate>.erInni(fom: LocalDate, tom: LocalDate) =
    this.start > fom && this.endInclusive < tom
internal fun ClosedRange<LocalDate>.overlapperMedHale(fom: LocalDate, tom: LocalDate) =
    this.start > fom && this.endInclusive >= tom

internal fun ClosedRange<LocalDate>.overlapperMedSnute(fom: LocalDate, tom: LocalDate) =
    this.start <= fom && this.endInclusive < tom