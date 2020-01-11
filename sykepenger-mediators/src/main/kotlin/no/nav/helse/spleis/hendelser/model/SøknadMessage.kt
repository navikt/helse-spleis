package no.nav.helse.spleis.hendelser.model

import no.nav.helse.spleis.hendelser.JsonMessage
import no.nav.helse.spleis.hendelser.MessageFactory
import no.nav.helse.spleis.hendelser.MessageProblems
import no.nav.helse.spleis.hendelser.MessageProcessor

// Understands a JSON message representing a Søknad
internal class SøknadMessage(originalMessage: String, private val problems: MessageProblems) :
    JsonMessage(originalMessage, problems) {
    init {
        requiredKey("id", "type", "status",
            "fnr", "aktorId", "arbeidsgiver", "fom", "tom", "startSyketilfelle",
            "opprettet", "sendtNav", "egenmeldinger",
            "fravar", "soknadsperioder")
    }

    override fun accept(processor: MessageProcessor) {
        processor.process(this, problems)
    }

    object Factory : MessageFactory<SøknadMessage> {

        override fun createMessage(message: String, problems: MessageProblems) =
            SøknadMessage(message, problems)
    }
}
