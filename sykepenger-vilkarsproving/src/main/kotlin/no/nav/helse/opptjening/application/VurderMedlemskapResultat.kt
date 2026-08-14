package no.nav.helse.opptjening.application

import java.time.LocalDate
import no.nav.helse.opptjening.domain.VurderingId

internal sealed class VurderMedlemskapResultat {
    data class HarVurdering(val fødselsnummer: String, val skjæringstidspunkt: LocalDate, val vurderingId: VurderingId) : VurderMedlemskapResultat()
    data class TrengerMedlemskap(val fødselsnummer: String, val skjæringstidspunkt: LocalDate) : VurderMedlemskapResultat()
}
