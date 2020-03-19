package no.nav.helse.unit.spleis.hendelser

import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.hendelser.MessageFactory
import no.nav.helse.spleis.hendelser.Parser
import no.nav.helse.spleis.hendelser.model.HendelseMessage
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class ParserTest : Parser.ParserDirector {

    @Test
    internal fun `invalid json`() {
        parser.register(object : MessageFactory<TestMessage> {
            override fun createMessage(message: String, problems: MessageProblems) =
                TestMessage(message, problems)
        })
        parser.onMessage("foo", context)

        assertTrue(messageWithError)
        assertContains("Invalid JSON per Jackson library", requireNotNull(messageException))
    }

    @Test
    internal fun `severe errors are caught`() {
        parser.register(object : MessageFactory<TestMessage> {
            override fun createMessage(message: String, problems: MessageProblems) =
                TestMessage(message, problems).apply {
                    problems.severe("Severe error!")
                }
        })
        parser.onMessage("{}", context)

        assertTrue(messageWithError)
        assertContains("Severe error!", requireNotNull(messageException))
    }

    @Test
    internal fun `when message is not recognized, errors are accumulated`() {
        messageFactory("key1_not_set")
        messageFactory("key2_not_set")
        parser.onMessage("{\"key\": \"value\"}", context)

        assertTrue(unrecognizedMessage)
        assertEquals(2, messageProblems.size)
        assertEquals(TestMessage::class.simpleName, messageProblems.first().first)
        assertTrue(messageProblems.first().second.hasErrors())
        assertContains("key1_not_set", messageProblems[0].second)
        assertContains("key2_not_set", messageProblems[1].second)
    }

    @Test
    internal fun `when message is recognized, errors are not accumulated`() {
        messageFactory("key1_not_set")
        var message2: TestMessage? = null
        messageFactory {
            requireKey("key")
            message2 = this
        }
        parser.onMessage("{\"key\": \"value\"}", context)

        assertEquals(message2, recognizedMessage)
        assertTrue(messageProblems.isEmpty())
    }

    @Test
    internal fun `stops at first recognizer without error`() {
        var message1: TestMessage? = null
        messageFactory {
            requireKey("key")
            message1 = this
        }
        messageFactory("key")

        parser.onMessage("{\"key\": \"value\"}", context)

        assertEquals(message1, recognizedMessage)
    }

    private fun assertContains(message: String, problems: MessageProblems) {
        assertTrue(problems.toString().contains(message)) { "Expected <$problems> to contain <$message>"}
    }

    private fun assertContains(message: String, problems: MessageProblems.MessageException) {
        assertTrue(problems.toString().contains(message)) { "Expected <$problems> to contain <$message>"}
    }

    private fun assertNotContains(message: String, problems: MessageProblems) {
        assertFalse(problems.toString().contains(message))
    }

    private val testRapid = object : RapidsConnection() {
        override fun publish(message: String) {}
        override fun publish(key: String, message: String) {}
        override fun start() {}
        override fun stop() {}
    }
    private val context = object : RapidsConnection.MessageContext {
        override fun send(message: String) {}
        override fun send(key: String, message: String) {}
    }
    private lateinit var parser: Parser
    private var messageWithError = false
    private var unrecognizedMessage = false
    private var recognizedMessage: HendelseMessage? = null
    private var messageException: MessageProblems.MessageException? = null
    private val messageProblems: MutableList<Pair<String, MessageProblems>> = mutableListOf()

    @BeforeEach
    internal fun setup() {
        parser = Parser(this, testRapid)
        recognizedMessage = null
        messageException = null
        unrecognizedMessage = false
    }

    override fun onRecognizedMessage(message: HendelseMessage, context: RapidsConnection.MessageContext) {
        recognizedMessage = message
        messageProblems.clear()
    }

    override fun onMessageException(exception: MessageProblems.MessageException) {
        messageWithError = true
        messageException = exception
    }

    override fun onUnrecognizedMessage(message: String, problems: List<Pair<String, MessageProblems>>) {
        unrecognizedMessage = true
        messageProblems.clear()
        messageProblems.addAll(problems)
    }

    private fun messageFactory(requiredKey: String) {
        messageFactory {
            requireKey(requiredKey)
        }
    }

    private fun messageFactory(block: TestMessage.() -> Unit) {
        parser.register(object : MessageFactory<TestMessage> {
            override fun createMessage(message: String, problems: MessageProblems): TestMessage {
                return TestMessage(message, problems).apply {
                    block(this)
                }
            }
        })
    }

    private class TestMessage(originalJson: String, problems: MessageProblems) : HendelseMessage(originalJson, problems) {
        override val id = UUID.randomUUID()
        override val fødselsnummer = UUID.randomUUID().toString()
    }
}
