package no.nav.helse.spleis.hendelser

// Acts as a GoF Abstract Factory
// Uses Collecting parameter to collect errors/messages
internal interface MessageFactory<MessageType: JsonMessage> {
    fun createMessage(message: String, problems: MessageProblems): MessageType
}
