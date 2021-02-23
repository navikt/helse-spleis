package no.nav.helse.spleis.meldinger

import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
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

    override fun onRecognizedMessage(message: HendelseMessage, context: MessageContext) {
        recognizedMessage = true
    }

    override fun onRiverError(riverName: String, problems: MessageProblems, context: MessageContext) {
        riverError = true
    }
}

