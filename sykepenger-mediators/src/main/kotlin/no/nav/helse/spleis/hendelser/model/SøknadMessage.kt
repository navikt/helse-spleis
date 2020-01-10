package no.nav.helse.spleis.hendelser.model

import no.nav.helse.spleis.hendelser.JsonMessage
import no.nav.helse.spleis.hendelser.MessageDirector
import no.nav.helse.spleis.hendelser.MessageProblems
import no.nav.helse.spleis.hendelser.MessageRecognizer

// Understands a JSON message representing a Søknad
internal class SøknadMessage(originalMessage: String, problems: MessageProblems) :
    JsonMessage(originalMessage, problems) {
    init {
        requiredKey("id", "type", "status",
            "fnr", "aktorId", "arbeidsgiver", "fom", "tom", "startSyketilfelle",
            "opprettet", "sendtNav", "egenmeldinger",
            "fravar", "soknadsperioder")
    }

    class Recognizer(director: MessageDirector<SøknadMessage>) :
        MessageRecognizer<SøknadMessage>(director) {

        override fun createMessage(message: String, problems: MessageProblems) =
            SøknadMessage(message, problems)
    }
}
