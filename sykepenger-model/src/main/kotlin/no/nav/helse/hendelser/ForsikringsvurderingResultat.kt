package no.nav.helse.hendelser

import java.time.LocalDate
import java.util.UUID

data class ForsikringsvurderingResultat(
    val forsikringsvurderingId: UUID,
    val harForsikring: Boolean,
    val villeHattForsikringOmDenVarBetalt: Boolean,
    val harForsikringSomIkkePasserMedSøknadstype: Boolean,
    val dekning: Dekning?,
    val opphørsdato: LocalDate?,
    val harIndividuellForsikring: Boolean,
) {
    data class Dekning(
        val grad: Int,
        val iVentetid: Boolean,
    )
}
