package no.nav.helse.unit.spleis.hendelser

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach

internal abstract class RiverTest {

    @BeforeEach
    fun reset() {
        messageMediator.reset()
        rapid.reset()
    }

    private val messageMediator = TestMessageMediator()
    private val rapid = TestRapid().apply {
        river(this, messageMediator)
    }

    protected abstract fun river(rapidsConnection: RapidsConnection, mediator: IMessageMediator)

    protected fun assertNoErrors(message: String) {
        rapid.sendTestMessage(message)
        assertTrue(messageMediator.recognizedMessage)
    }

    protected fun assertErrors(message: String) {
        rapid.sendTestMessage(message)
        assertTrue(messageMediator.riverError)
    }

    protected fun assertSevere(message: String) {
        rapid.sendTestMessage(message)
        assertTrue(messageMediator.riverSevereError)
    }
}
