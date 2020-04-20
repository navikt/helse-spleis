package no.nav.helse.unit.spleis.hendelser

import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.hendelser.model.HendelseMessage

internal class TestMessageMediator : IMessageMediator {

    internal var recognizedMessage = false
        get() = field.also { reset() }
        private set
    internal var riverError = false
        get() = field.also { reset() }
        private set
    internal var riverSevereError = false
        get() = field.also { reset() }
        private set

    internal fun reset() {
        recognizedMessage = false
        riverError = false
        riverSevereError = false
    }

    override fun onRecognizedMessage(message: HendelseMessage, context: RapidsConnection.MessageContext) {
        recognizedMessage = true
    }

    override fun onRiverError(riverName: String, problems: MessageProblems, context: RapidsConnection.MessageContext) {
        riverError = true
    }

    override fun onRiverSevere(riverName: String, error: MessageProblems.MessageException, context: RapidsConnection.MessageContext) {
        riverSevereError = true
    }
}

