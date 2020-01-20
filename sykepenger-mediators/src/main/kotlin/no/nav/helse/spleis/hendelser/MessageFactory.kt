package no.nav.helse.spleis.hendelser

import no.nav.helse.person.Aktivitetslogger

// Acts as a GoF Abstract Factory
// Uses Collecting parameter to collect errors/messages
internal interface MessageFactory {
    fun createMessage(message: String, problems: Aktivitetslogger): JsonMessage
}
