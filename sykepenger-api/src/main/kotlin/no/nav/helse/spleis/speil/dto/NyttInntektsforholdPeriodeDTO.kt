package no.nav.helse.spleis.speil.dto

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.forrigeDag
import no.nav.helse.nesteDag

data class NyttInntektsforholdPeriodeDTO(
    val id: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val vilkårsgrunnlagId: UUID,
    val skjæringstidspunkt: LocalDate
) {
    internal fun brytOpp(tidslinjeperiode: ClosedRange<LocalDate>) = brytOpp(this, fom, tom, tidslinjeperiode, NyttInntektsforholdPeriodeDTO::til, NyttInntektsforholdPeriodeDTO::fra)

    internal fun til(other: ClosedRange<LocalDate>) = copy(
        id = UUID.randomUUID(),
        tom = other.start.forrigeDag
    )
    internal fun fra(other: ClosedRange<LocalDate>) = copy(
        id = UUID.randomUUID(),
        fom = other.endInclusive.nesteDag
    )
}