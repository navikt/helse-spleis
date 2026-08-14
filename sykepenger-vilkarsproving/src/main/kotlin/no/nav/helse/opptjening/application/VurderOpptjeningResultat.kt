package no.nav.helse.opptjening.application

import java.time.LocalDate
import java.util.UUID

internal sealed class VurderOpptjeningResultat {
    data class HarVurdering(val fødselsnummer: String, val skjæringstidspunkt: LocalDate, val vurderingId: UUID) : VurderOpptjeningResultat()
    data class TrengerArbeidsforhold(val fødselsnummer: String) : VurderOpptjeningResultat() {
        constructor(fødselsnummer: String, skjæringstidspunkt: LocalDate) : this(fødselsnummer)
    }
}
