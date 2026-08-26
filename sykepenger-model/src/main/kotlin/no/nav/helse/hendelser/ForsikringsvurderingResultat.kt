package no.nav.helse.hendelser

import java.time.LocalDate
import java.util.UUID

data class ForsikringsvurderingResultat(
    val forsikringsvurderingId: UUID,
    val dekning: Dekning?,
    val opphørsdato: LocalDate?,
    val harIndividuellForsikring: Boolean,
    val villeHattForsikringOmDenVarBetalt: Boolean,
    val harForsikringSomIkkePasserMedSøknadstype: Boolean,
) {
    data class Dekning(
        val grad: Int,
        val iVentetid: Boolean,
    )
}
