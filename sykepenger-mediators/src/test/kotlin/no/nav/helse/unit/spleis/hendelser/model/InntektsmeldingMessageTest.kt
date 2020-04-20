package no.nav.helse.unit.spleis.hendelser.model

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.ConstantAnswer
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.spleis.MessageMediator
import no.nav.helse.spleis.hendelser.Inntektsmeldinger
import no.nav.helse.unit.spleis.hendelser.TestRapid
import no.nav.inntektsmeldingkontrakt.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
    ).asObjectNode()
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
    ).asObjectNode().toJson()

    private val ValidInntektsmeldingJson = ValidInntektsmelding.toJson()
    private val ValidInntektsmeldingWithUnknownFieldsJson =
        ValidInntektsmelding.put(UUID.randomUUID().toString(), "foobar").toJson()

    @Test
    internal fun `invalid messages`() {
        assertThrows(InvalidJson)
        assertThrows(UnknownJson)
    }

    @Test
    internal fun `valid inntektsmelding`() {
        assertValidMessage(ValidInntektsmeldingWithUnknownFieldsJson)
        assertValidMessage(ValidInntektsmeldingJson)
        assertValidMessage(ValidInntektsmeldingUtenRefusjon)
    }

    private fun assertValidMessage(message: String) {
        recognizedMessage = false
        rapid.sendTestMessage(message)
        assertTrue(recognizedMessage)
    }

    private fun assertInvalidMessage(message: String) {
        riverError = false
        rapid.sendTestMessage(message)
        assertTrue(riverError)
    }

    private fun assertThrows(message: String) {
        riverSevere = false
        rapid.sendTestMessage(message)
        assertTrue(riverSevere)
    }

    private var riverError = false
    private var riverSevere = false
    private var recognizedMessage = false
    @BeforeEach
    fun reset() {
        recognizedMessage = false
        riverError = false
        riverSevere = false
        rapid.reset()
    }

    private val messageMediator = mockk<MessageMediator>()
    private val rapid = TestRapid().apply {
        Inntektsmeldinger(this, messageMediator)
    }
    init {
        every {
            messageMediator.onRecognizedMessage(any(), any())
        } answers {
            recognizedMessage = true
            ConstantAnswer(Unit)
        }
        every {
            messageMediator.onRiverError(any(), any(), any())
        } answers {
            riverError = true
            ConstantAnswer(Unit)
        }
        every {
            messageMediator.onRiverSevere(any(), any(), any())
        } answers {
            riverSevere = true
            ConstantAnswer(Unit)
        }
    }
}

private val objectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

private fun Inntektsmeldingkontrakt.asObjectNode(): ObjectNode = objectMapper.valueToTree<ObjectNode>(this).apply {
    put("@id", UUID.randomUUID().toString())
    put("@event_name", "inntektsmelding")
    put("@opprettet", LocalDateTime.now().toString())
}
private fun JsonNode.toJson(): String = objectMapper.writeValueAsString(this)
