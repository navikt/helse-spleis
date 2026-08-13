package no.nav.helse.opptjening.application

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.opptjening.domain.Opptjening

interface Meldingssender {
    fun sendArbeidsforholdBehov(fødselsnummer: String)
    fun sendOpptjeningsvurderingReferanse(fødselsnummer: String, skjæringstidspunkt: LocalDate, opptjeningsvurderingId: UUID)
    fun sendOpptjeningsvurderingResultat(fødselsnummer: String, skjæringstidspunkt: LocalDate, opptjeningsvurdering: Opptjening)
}
