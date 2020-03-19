package no.nav.helse.unit.spleis.hendelser.model

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.spleis.hendelser.model.InntektsmeldingMessage
import no.nav.inntektsmeldingkontrakt.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.inntektsmeldingkontrakt.Inntektsmelding as Inntektsmeldingkontrakt

internal class InntektsmeldingMessageTest {

    private val InvalidJson = "foo"
    private val UnknownJson = "{\"foo\": \"bar\"}"
    private val ValidInntektsmelding = Inntektsmeldingkontrakt(
        inntektsmeldingId = "",
        arbeidstakerFnr = "fødselsnummer",
        arbeidstakerAktorId = "aktørId",
        virksomhetsnummer = "virksomhetsnummer",
        arbeidsgiverFnr = null,
        arbeidsgiverAktorId = null,
        arbeidsgivertype = Arbeidsgivertype.VIRKSOMHET,
        arbeidsforholdId = null,
        beregnetInntekt = BigDecimal.ONE,
        refusjon = Refusjon(beloepPrMnd = BigDecimal.ONE, opphoersdato = LocalDate.now()),
        endringIRefusjoner = listOf(EndringIRefusjon(endringsdato = LocalDate.now(), beloep = BigDecimal.ONE)),
        opphoerAvNaturalytelser = emptyList(),
        gjenopptakelseNaturalytelser = emptyList(),
        arbeidsgiverperioder = listOf(Periode(fom = LocalDate.now(), tom = LocalDate.now())),
        status = Status.GYLDIG,
        arkivreferanse = "",
        ferieperioder = listOf(Periode(fom = LocalDate.now(), tom = LocalDate.now())),
        foersteFravaersdag = LocalDate.now(),
        mottattDato = LocalDateTime.now()
    ).asJsonNode()
    private val ValidInntektsmeldingUtenRefusjon = Inntektsmeldingkontrakt(
        inntektsmeldingId = "",
        arbeidstakerFnr = "fødselsnummer",
        arbeidstakerAktorId = "aktørId",
        virksomhetsnummer = "virksomhetsnummer",
        arbeidsgiverFnr = null,
        arbeidsgiverAktorId = null,
        arbeidsgivertype = Arbeidsgivertype.VIRKSOMHET,
        arbeidsforholdId = null,
        beregnetInntekt = BigDecimal.ONE,
        refusjon = Refusjon(beloepPrMnd = null, opphoersdato = null),
        endringIRefusjoner = listOf(EndringIRefusjon(endringsdato = LocalDate.now(), beloep = BigDecimal.ONE)),
        opphoerAvNaturalytelser = emptyList(),
        gjenopptakelseNaturalytelser = emptyList(),
        arbeidsgiverperioder = listOf(Periode(fom = LocalDate.now(), tom = LocalDate.now())),
        status = Status.GYLDIG,
        arkivreferanse = "",
        ferieperioder = listOf(Periode(fom = LocalDate.now(), tom = LocalDate.now())),
        foersteFravaersdag = LocalDate.now(),
        mottattDato = LocalDateTime.now()
    ).asJsonNode().toJson()

    private val ValidInntektsmeldingJson = ValidInntektsmelding.toJson()
    private val ValidInntektsmeldingWithUnknownFieldsJson = ValidInntektsmelding.let {
        it as ObjectNode
        it.put(UUID.randomUUID().toString(), "foobar")
    }.toJson()

    @Test
    internal fun `invalid messages`() {
        assertThrows(InvalidJson)
        assertInvalidMessage(UnknownJson)
    }

    @Test
    internal fun `valid inntektsmelding`() {
        assertValidInntektsmeldingMessage(ValidInntektsmeldingWithUnknownFieldsJson)
        assertValidInntektsmeldingMessage(ValidInntektsmeldingJson)
        assertValidInntektsmeldingMessage(ValidInntektsmeldingUtenRefusjon)
    }

    private fun assertValidInntektsmeldingMessage(message: String) {
        val problems = MessageProblems(message)
        InntektsmeldingMessage(message, problems)
        assertFalse(problems.hasErrors()) { problems.toExtendedReport() }
    }

    private fun assertInvalidMessage(message: String) {
        val problems = MessageProblems(message)
        InntektsmeldingMessage(message, problems)
        assertTrue(problems.hasErrors()) { "was not supposes to recognize $message" }
    }

    private fun assertThrows(message: String) {
        MessageProblems(message).also {
            assertThrows<MessageProblems.MessageException> {
                InntektsmeldingMessage(message, it)
            }
            assertTrue(it.hasErrors()) { "was not supposed to recognize $message" }
        }
    }

    private var recognizedInntektsmelding = false
    @BeforeEach
    fun reset() {
        recognizedInntektsmelding = false
    }
}

private val objectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

private fun Inntektsmeldingkontrakt.asJsonNode(): JsonNode = objectMapper.valueToTree<JsonNode>(this).apply {
    this as ObjectNode
    put("@id", UUID.randomUUID().toString())
    put("@event_name", "inntektsmelding")
    put("@opprettet", LocalDateTime.now().toString())
}
private fun JsonNode.toJson(): String = objectMapper.writeValueAsString(this)
