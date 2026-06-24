package no.nav.helse.hendelser

import java.time.LocalDate
import java.util.UUID

data class ForsikringsvurderingResultat(
    val forsikringsvurderingId: UUID,
    val harForsikring: Boolean,
    val dekning: Dekning?,
    val opphørsdato: LocalDate?
) {
    data class Dekning(
        val grad: Int,
        val iVentetid: Boolean,
    )
}
