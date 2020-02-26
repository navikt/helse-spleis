package no.nav.helse.spleis.hendelser

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems

interface MessageFactory<out Message: JsonMessage> {
    fun createMessage(message: String, problems: MessageProblems): Message
}
