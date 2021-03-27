package no.nav.helse.spleis.meldinger.model

import no.nav.helse.spleis.TestHendelseMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class HendelseMessageTest {

    private val id = UUID.randomUUID()
    private val opprettet = LocalDateTime.now()
    private val additionalTracing = mapOf(
        "foo" to "bar"
    )
    private val message = TestHendelseMessage("fnr", id, opprettet, additionalTracing)

    @Test
    fun tracinginfo() {
        assertEquals(additionalTracing + mapOf(
            "event_name" to "test_event",
            "id" to id,
            "opprettet" to opprettet
        ), message.tracinginfo())
    }
}
