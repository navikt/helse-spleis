package no.nav.helse.spleis.mediator.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
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

