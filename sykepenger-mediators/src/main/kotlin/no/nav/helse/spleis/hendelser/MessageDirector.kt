package no.nav.helse.spleis.hendelser

internal interface MessageDirector<MessageType: JsonMessage> {
    fun onMessage(message: MessageType, warnings: MessageProblems)
}
