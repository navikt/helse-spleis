package no.nav.helse.spleis.meldinger.model

import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.TestHendelseMessage.Companion.testPacket
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class BehovMessageTest {
    private val behovtyper = listOf("A", "B", "C")
    private val behov = TestBehov(behovtyper)

    @Test
    fun tracinginfo() {
        assertEquals(behovtyper, behov.tracinginfo().getValue("behov"))
    }

    private class TestBehov(behovtyper: List<String>) : BehovMessage(testPacket(
        fødselsnummer = "fnr",
        id = UUID.randomUUID(),
        opprettet = LocalDateTime.now(),
        extra = mapOf("@behov" to behovtyper)
    )) {
        override fun behandle(mediator: IHendelseMediator) {
            throw NotImplementedError()
        }
    }
}
