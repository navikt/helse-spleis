package no.nav.helse.unit.spleis.hendelser

import com.fasterxml.jackson.databind.node.*
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.spleis.hendelser.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeParseException

internal class JsonMessageTest {

    private val ValidJson = "{\"foo\": \"bar\"}"
    private val InvalidJson = "foo"

    @Test
    internal fun `invalid json`() {
        Aktivitetslogger(InvalidJson).also {
            assertThrows<Aktivitetslogger.AktivitetException> {
                JsonMessage(InvalidJson, it, Aktivitetslogg())
            }
            assertTrue(it.hasErrorsOld()) { "was not supposed to recognize $InvalidJson" }
        }
    }

    @Test
    internal fun `valid json`() {
        val problems = Aktivitetslogger(ValidJson)
        JsonMessage(ValidJson, problems, Aktivitetslogg())
        assertFalse(problems.hasErrorsOld())
    }

    @Test
    internal fun `extended message`() {
        "not_valid_json".also { json ->
            Aktivitetslogger(json).also {
                assertThrows<Aktivitetslogger.AktivitetException> {
                    ExtendedMessage(json, it, Aktivitetslogg())
                }
                assertTrue(it.hasErrorsOld()) { "was not supposed to recognize $json" }
            }
        }

        "{}".also { json ->
            val problems = Aktivitetslogger(json)
            val aktivitetslogg = Aktivitetslogg()
            ExtendedMessage(json, problems, aktivitetslogg)
            assertTrue(problems.hasErrorsOld())
        }

        "{\"required_key\": \"foo\"}".also { json ->
            val problems = Aktivitetslogger(json)
            val aktivitetslogg = Aktivitetslogg()
            ExtendedMessage(json, problems, aktivitetslogg)
            assertFalse(problems.hasErrorsOld())
        }
    }

    @Test
    internal fun `keys must be declared before use`() {
        "{\"foo\": \"bar\"}".also { json ->
            message(json).also {
                assertThrows<IllegalArgumentException> { it["foo"] }
                it.requiredKey("foo")
                assertEquals("bar", it["foo"].textValue())
            }
            message(json).also {
                assertThrows<IllegalArgumentException> { it["foo"] }
                it.interestedIn("foo")
                assertEquals("bar", it["foo"].textValue())
            }
        }
    }

    @Test
    internal fun `nested keys`() {
        "{\"foo\": { \"bar\": \"baz\" }}".also { json ->
            message(json).also {
                assertThrows<IllegalArgumentException> { it["foo.bar"] }
                it.requiredKey("foo.bar")
                assertEquals("baz", it["foo.bar"].textValue())
            }
            message(json).also {
                assertThrows<IllegalArgumentException> { it["foo.bar"] }
                it.interestedIn("foo.bar")
                assertEquals("baz", it["foo.bar"].textValue())
            }
        }
    }

    @Test
    internal fun requiredValue() {
        "{}".also {
            assertThrows(it, "foo", "bar")
            assertThrows(it, "foo", false)
        }
        "{\"foo\": null}".also {
            assertThrows(it, "foo", "bar")
            assertThrows(it, "foo", false)
        }
        assertThrows("{\"foo\": \"baz\"}", "foo", "bar")
        assertThrows("{\"foo\": true}", "foo", false)

        assertEquals("{\"foo\": \"bar\"}", "foo", "bar")
        assertEquals("{\"foo\": false}", "foo", false)
    }

    @Test
    internal fun requiredNestedValue() {
        assertEquals("{\"foo\": { \"bar\": \"baz\" } }", "foo.bar", "baz")
        assertEquals("{\"foo\": { \"bar\": true } }", "foo.bar", true)
    }

    @Test
    internal fun requiredValues() {
        assertThrows("{}", "foo", listOf("bar"))
        assertThrows("{\"foo\": null}", "foo", listOf("bar"))
        assertThrows("{\"foo\": [\"bar\"]}", "foo", listOf("bar","foo"))

        assertEquals("{\"foo\": [\"bar\", \"foo\"]}", "foo", listOf("bar","foo"))
        assertEquals("{\"foo\": [\"bar\", \"foo\", \"bla\"]}", "foo", listOf("bar","foo"))
    }

    @Test
    internal fun requiredValueOneOf() {
        "{\"foo\": \"bar\" }".also { json ->
            message(json).also {
                it.requiredValueOneOf("foo", listOf("foo"))
                assertTrue(aktivitetslogger.hasErrorsOld())
                assertThrows<IllegalArgumentException> { it["foo"] }
            }
            message(json).also {
                it.requiredValueOneOf("foo", listOf("bar", "foobar"))
                assertFalse(aktivitetslogger.hasErrorsOld()) { "did not expect errors: $aktivitetslogger" }
                assertDoesNotThrow { it["foo"] }
            }
        }
    }

    @Test
    internal fun requiredNestedValues() {
        assertEquals("{\"foo\": { \"bar\": [ \"baz\", \"foobar\" ] }}", "foo.bar", listOf("baz","foobar"))
    }

    @Test
    internal fun `requiredKey can not return null`() {
        val message = message("{\"foo\": null}").apply {
            requiredKey("foo")
        }

        assertThrows<IllegalArgumentException> {
            message["foo"]
        }
    }

    @Test
    internal fun `interestedIn can return null`() {
        val message = message("{\"foo\": null}").apply {
            interestedIn("foo")
        }
        assertNull(message["foo"].textValue())
    }

    @Test
    internal fun asLocalDate() {
        assertThrows<DateTimeParseException> { MissingNode.getInstance().asLocalDate() }
        assertThrows<DateTimeParseException> { NullNode.instance.asLocalDate() }
        assertThrows<DateTimeParseException> { BooleanNode.TRUE.asLocalDate() }
        assertThrows<DateTimeParseException> { IntNode(0).asLocalDate() }
        assertThrows<DateTimeParseException> { TextNode.valueOf("").asLocalDate() }
        with ("2020-01-01") {
            assertEquals(LocalDate.parse(this), TextNode.valueOf(this).asLocalDate())
        }
    }

    @Test
    internal fun asYearMonth() {
        assertThrows<DateTimeParseException> { MissingNode.getInstance().asYearMonth() }
        assertThrows<DateTimeParseException> { NullNode.instance.asYearMonth() }
        assertThrows<DateTimeParseException> { BooleanNode.TRUE.asYearMonth() }
        assertThrows<DateTimeParseException> { IntNode(0).asYearMonth() }
        assertThrows<DateTimeParseException> { TextNode.valueOf("").asYearMonth() }
        with ("2020-01") {
            assertEquals(YearMonth.parse(this), TextNode.valueOf(this).asYearMonth())
        }
    }

    @Test
    internal fun asOptionalLocalDate() {
        assertNull(MissingNode.getInstance().asOptionalLocalDate())
        assertNull(NullNode.instance.asOptionalLocalDate())
        assertNull(BooleanNode.TRUE.asOptionalLocalDate())
        assertNull(IntNode(0).asOptionalLocalDate())
        assertNull(TextNode.valueOf("").asOptionalLocalDate())
        with ("2020-01-01") {
            assertEquals(LocalDate.parse(this), TextNode.valueOf(this).asOptionalLocalDate())
        }
    }


    @Test
    internal fun asLocalDateTime() {
        assertThrows<DateTimeParseException> { MissingNode.getInstance().asLocalDateTime() }
        assertThrows<DateTimeParseException> { NullNode.instance.asLocalDateTime() }
        assertThrows<DateTimeParseException> { BooleanNode.TRUE.asLocalDateTime() }
        assertThrows<DateTimeParseException> { IntNode(0).asLocalDateTime() }
        assertThrows<DateTimeParseException> { TextNode.valueOf("").asLocalDateTime() }
        with("2020-01-01T00:00:00.000000") {
            assertEquals(LocalDateTime.parse(this), TextNode.valueOf(this).asLocalDateTime())
        }
    }

    private fun assertEquals(msg: String, key: String, expectedValue: String) {
        val problems = Aktivitetslogger(msg)
        val aktivitetslogg = Aktivitetslogg()
        JsonMessage(msg, problems, aktivitetslogg).also {
            it.requiredValue(key, expectedValue)
            assertFalse(problems.hasErrorsOld())
            assertEquals(expectedValue, it[key].textValue())
        }
    }

    private fun assertEquals(msg: String, key: String, expectedValue: Boolean) {
        val problems = Aktivitetslogger(msg)
        val aktivitetslogg = Aktivitetslogg()
        JsonMessage(msg, problems, aktivitetslogg).also {
            it.requiredValue(key, expectedValue)
            assertFalse(problems.hasErrorsOld())
            assertEquals(expectedValue, it[key].booleanValue())
        }
    }

    private fun assertEquals(msg: String, key: String, expectedValues: List<String>) {
        val problems = Aktivitetslogger(msg)
        val aktivitetslogg = Aktivitetslogg()
        JsonMessage(msg, problems, aktivitetslogg).also {
            it.requiredValues(key, expectedValues)
            assertFalse(problems.hasErrorsOld())
        }
    }

    private fun assertThrows(msg: String, key: String, expectedValues: List<String>) {
        val problems = Aktivitetslogger(msg)
        val aktivitetslogg = Aktivitetslogg()
        JsonMessage(msg, problems, aktivitetslogg).also {
            it.requiredValues(key, expectedValues)
            assertTrue(problems.hasErrorsOld())
        }
    }

    private fun assertThrows(msg: String, key: String, expectedValue: Boolean) {
        val problems = Aktivitetslogger(msg)
        val aktivitetslogg = Aktivitetslogg()
        JsonMessage(msg, problems, aktivitetslogg).also {
            it.requiredValue(key, expectedValue)
            assertTrue(problems.hasErrorsOld())
            assertThrows(it, key)
        }
    }

    private fun assertThrows(msg: String, key: String, expectedValue: String) {
        val problems = Aktivitetslogger(msg)
        val aktivitetslogg = Aktivitetslogg()
        JsonMessage(msg, problems, aktivitetslogg).also {
            it.requiredValue(key, expectedValue)
            assertTrue(problems.hasErrorsOld())
            assertThrows(it, key)
        }
    }

    private fun assertThrows(message: JsonMessage, key: String) {
        assertThrows<IllegalArgumentException>({ "should throw exception, instead returned ${message[key]}" }) {
            message[key]
        }
    }

    private lateinit var aktivitetslogger: Aktivitetslogger
    private lateinit var aktivitetslogg: Aktivitetslogg

    private fun message(json: String): JsonMessage {
        aktivitetslogger = Aktivitetslogger(json)
        aktivitetslogg = Aktivitetslogg()
        return JsonMessage(json, aktivitetslogger, aktivitetslogg)
    }

    private class ExtendedMessage(originalMessage: String, problems: Aktivitetslogger, aktivitetslogg: Aktivitetslogg) :
        JsonMessage(originalMessage, problems, aktivitetslogg) {
        init {
            requiredKey("required_key")
        }
    }
}
