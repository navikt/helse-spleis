package no.nav.helse.spleis.mediator.meldinger.model

import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.mediator.TestHendelseMessage.Companion.testPacket
import no.nav.helse.spleis.meldinger.model.BehovMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class BehovMessageTest {
    private val behovtyper = listOf("A", "B", "C")
    private val behov = TestBehov(behovtyper)

    @Test
    fun tracinginfo() {
        assertEquals(behovtyper, behov.tracinginfo().getValue("behov"))
    }

    private class TestBehov(behovtyper: List<String>) : BehovMessage(testPacket(
        fødselsnummer = "fnr",
        extra = mapOf("@behov" to behovtyper)
    )) {
        override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
            throw NotImplementedError()
        }
    }
}
