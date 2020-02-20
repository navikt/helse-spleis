package no.nav.helse.rapids_rivers

import io.ktor.util.KtorExperimentalAPI
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@KtorExperimentalAPI
internal class InMemoryRapidTest {

    @Test
    internal fun test() {
        val rapid = inMemoryRapid {  }
        InMemoryRiver(rapid)

        rapid.sendToListeners("sldjjfnqaolsdjcb")
        rapid.sendToListeners("""{"@behov":"hei"}""")

        assertEquals(listOf("""{"@behov":"hei"} ut"""), rapid.outgoingMessages.map { it.value })
    }

    internal class InMemoryRiver(rapidsConnection: RapidsConnection) : River.PacketListener {
        init {
            River(rapidsConnection).apply {
                validate { it.requireKey("@behov") }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            context.send("${packet.toJson()} ut")
        }

        override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {}
    }

}
