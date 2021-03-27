package no.nav.helse.spleis

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import java.time.LocalDateTime
import java.util.*

internal class TestHendelseMessage(
    fnr: String,
    id: UUID = UUID.randomUUID(),
    opprettet: LocalDateTime = LocalDateTime.now(),
    private val tracinginfo: Map<String, Any> = emptyMap(),
    packet: JsonMessage = testPacket(id, opprettet)
) : HendelseMessage(packet) {
    override val fødselsnummer = fnr

    override fun behandle(mediator: IHendelseMediator) {
        throw NotImplementedError()
    }

    override fun additionalTracinginfo(packet: JsonMessage) =
        tracinginfo

    private companion object {
        fun testPacket(id: UUID, opprettet: LocalDateTime) = JsonMessage.newMessage(mapOf(
            "@id" to id,
            "@opprettet" to opprettet,
            "@event_name" to "test_event"
        )).apply {
            requireKey("@opprettet", "@id", "@event_name")
        }
    }
}
