package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.inntektsmeldingkontrakt.*
import no.nav.inntektsmeldingkontrakt.Naturalytelse.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.inntektsmeldingkontrakt.Inntektsmelding as Inntektsmeldingkontrakt

internal class InntektsmeldingerRiverTest : RiverTest() {

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

    private val ValidInntektsmeldingMedOpphørAvNaturalytelser = ValidInntektsmelding.set<JsonNode>(
        "opphoerAvNaturalytelser",
        listOf(OpphoerAvNaturalytelse(ELEKTRONISKKOMMUNIKASJON, LocalDate.now(), BigDecimal(589.00))).toJsonNode()
    ).toJson()

    override fun river(rapidsConnection: RapidsConnection, mediator: IMessageMediator) {
        InntektsmeldingerRiver(rapidsConnection, mediator)
    }

    @Test
    fun `invalid messages`() {
        assertIgnored(InvalidJson)
        assertIgnored(UnknownJson)
    }

    @Test
    fun `valid inntektsmelding`() {
        assertNoErrors(ValidInntektsmeldingWithUnknownFieldsJson)
        assertNoErrors(ValidInntektsmeldingJson)
        assertNoErrors(ValidInntektsmeldingUtenRefusjon)
        assertNoErrors(ValidInntektsmeldingMedOpphørAvNaturalytelser)
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

private fun List<OpphoerAvNaturalytelse>.toJsonNode(): JsonNode = objectMapper.valueToTree(this)
