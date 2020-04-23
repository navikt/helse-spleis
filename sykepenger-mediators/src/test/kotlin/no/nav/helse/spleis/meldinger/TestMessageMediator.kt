package no.nav.helse.spleis.meldinger

import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.HendelseMessage

internal class TestMessageMediator : IMessageMediator {

    internal var recognizedMessage = false
        get() = field.also { reset() }
        private set
    internal var riverError = false
        get() = field.also { reset() }
        private set

    internal fun reset() {
        recognizedMessage = false
        riverError = false
    }

    override fun onRecognizedMessage(message: HendelseMessage, context: RapidsConnection.MessageContext) {
        recognizedMessage = true
    }

    override fun onRiverError(riverName: String, problems: MessageProblems, context: RapidsConnection.MessageContext) {
        riverError = true
    }
}

