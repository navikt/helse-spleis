package no.nav.helse.opptjening.infra.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.opptjening.application.Meldingssender
import no.nav.helse.opptjening.domain.Opptjening

class KafkaMeldingssender(private val messageContext: MessageContext): Meldingssender {
    override fun sendArbeidsforholdBehov(fødselsnummer: String) {
        TODO("Not yet implemented")
    }

    override fun sendOpptjeningsvurderingReferanse(fødselsnummer: String, skjæringstidspunkt: LocalDate, opptjeningsvurderingId: UUID) {
        TODO("Not yet implemented")
    }

    override fun sendOpptjeningsvurderingResultat(fødselsnummer: String, skjæringstidspunkt: LocalDate, opptjeningsvurdering: Opptjening) {
        TODO("Not yet implemented")
    }
}
