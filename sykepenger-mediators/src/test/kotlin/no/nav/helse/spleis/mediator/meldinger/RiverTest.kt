package no.nav.helse.spleis.mediator.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import org.junit.jupiter.api.Assertions.assertFalse
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

    protected fun assertNoErrors(message: Pair<*, String>) = assertNoErrors(message.second)

    protected fun assertNoErrors(message: String) {
        rapid.sendTestMessage(message)
        assertTrue(messageMediator.recognizedMessage)
    }

    protected fun assertErrors(message: Pair<*, String>) = assertErrors(message.second)
    protected fun assertErrors(message: String) {
        rapid.sendTestMessage(message)
        assertTrue(messageMediator.riverError)
    }

    protected fun assertIgnored(message: Pair<*, String>) = assertIgnored(message.second)
    protected fun assertIgnored(message: String) {
        rapid.sendTestMessage(message)
        assertFalse(messageMediator.recognizedMessage)
        assertFalse(messageMediator.riverError)
    }
}
