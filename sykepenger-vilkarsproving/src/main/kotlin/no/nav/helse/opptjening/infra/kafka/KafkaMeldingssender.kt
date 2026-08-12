package no.nav.helse.opptjening.infra.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import java.time.LocalDate
import no.nav.helse.opptjening.application.Meldingssender
import no.nav.helse.opptjening.domain.Opptjening

class KafkaMeldingssender(private val messageContext: MessageContext): Meldingssender {
    override fun sendOpptjeningsgrunnlagBehov(fødselsnummer: String, skjæringstidspunkt: LocalDate) {
        TODO("Not yet implemented")
    }

    override fun sendOpptjeningsløsning(fødselsnummer: String, skjæringstidspunkt: LocalDate, opptjening: Opptjening) {
        TODO("Not yet implemented")
    }
}
