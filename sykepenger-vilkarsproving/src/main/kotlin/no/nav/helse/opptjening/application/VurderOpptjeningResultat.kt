package no.nav.helse.opptjening.application

import java.time.LocalDate
import no.nav.helse.opptjening.domain.VurderingId

internal sealed class VurderOpptjeningResultat {
    data class HarVurdering(val fødselsnummer: String, val skjæringstidspunkt: LocalDate, val vurderingId: VurderingId) : VurderOpptjeningResultat()
    data class TrengerArbeidsforhold(val fødselsnummer: String, val skjæringstidspunkt: LocalDate) : VurderOpptjeningResultat()
}
