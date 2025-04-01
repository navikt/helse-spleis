package no.nav.helse.spleis.mediator.meldinger

import no.nav.inntektsmeldingkontrakt.Inntektsmelding as Inntektsmeldingkontrakt
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.mai
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.NavNoInntektsmeldingerRiver
import no.nav.inntektsmeldingkontrakt.Arbeidsgivertype
import no.nav.inntektsmeldingkontrakt.ArsakTilInnsending
import no.nav.inntektsmeldingkontrakt.AvsenderSystem
import no.nav.inntektsmeldingkontrakt.EndringIRefusjon
import no.nav.inntektsmeldingkontrakt.Naturalytelse.ELEKTRONISKKOMMUNIKASJON
import no.nav.inntektsmeldingkontrakt.OpphoerAvNaturalytelse
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.inntektsmeldingkontrakt.Refusjon
import no.nav.inntektsmeldingkontrakt.Status
import org.junit.jupiter.api.Test

internal class NavNoInntektsmeldingerRiverTest : RiverTest() {

    private val fødselsdato = 17.mai(1995)
    private val im = Inntektsmeldingkontrakt(
        inntektsmeldingId = UUID.randomUUID().toString(),
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
        mottattDato = LocalDateTime.now(),
        naerRelasjon = null,
        avsenderSystem = AvsenderSystem("NAV_NO", "1.0"),
        innsenderFulltNavn = "SPLEIS MEDIATOR",
        innsenderTelefon = "tlfnr",
        inntektsdato = null,
        vedtaksperiodeId = UUID.randomUUID(),
        arsakTilInnsending = ArsakTilInnsending.Ny
    )
    private val InvalidJson = "foo"
    private val UnknownJson = "{\"foo\": \"bar\"}"
    private val ValidInntektsmelding = im.asObjectNode()
    private val ValidInntektsmeldingUtenBeregnetInntekt = im.copy(beregnetInntekt = null).asObjectNode().toJson()
    private val ValidInntektsmeldingUtenRefusjon = im.asObjectNode().toJson()

    private val ValidInntektsmeldingJson = ValidInntektsmelding.toJson()
    private val ValidInntektsmeldingWithUnknownFieldsJson =
        ValidInntektsmelding.put(UUID.randomUUID().toString(), "foobar").toJson()

    private val ValidInntektsmeldingMedOpphørAvNaturalytelser = im.copy(
        opphoerAvNaturalytelser = listOf(OpphoerAvNaturalytelse(ELEKTRONISKKOMMUNIKASJON, LocalDate.now(), BigDecimal(589.00)))
    ).asObjectNode().toJson()


    private val ValidInntektsmeldingUtenVedtaksperiodeId = ValidInntektsmelding.apply { this.remove("vedtaksperiodeId") }.toJson()

    override fun river(rapidsConnection: RapidsConnection, mediator: IMessageMediator) {
        NavNoInntektsmeldingerRiver(rapidsConnection, mediator)
    }

    @Test
    fun `invalid messages`() {
        assertIgnored(InvalidJson)
        assertIgnored(UnknownJson)
        assertIgnored(ValidInntektsmeldingUtenVedtaksperiodeId)
    }

    @Test
    fun `valid inntektsmelding`() {
        assertNoErrors(ValidInntektsmeldingWithUnknownFieldsJson)
        assertNoErrors(ValidInntektsmeldingJson.also { println(it) })
        assertNoErrors(ValidInntektsmeldingUtenRefusjon)
        assertNoErrors(ValidInntektsmeldingUtenBeregnetInntekt)
        assertNoErrors(ValidInntektsmeldingMedOpphørAvNaturalytelser)
        assertNoErrors(im.asObjectNode("arbeidsgiveropplysninger").toJson())
    }

    private fun JsonNode.toJson(): String = (this as ObjectNode).put("fødselsdato", "$fødselsdato").toString()
}

private val objectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

private fun Inntektsmeldingkontrakt.asObjectNode(eventname: String = "inntektsmelding"): ObjectNode = objectMapper.valueToTree<ObjectNode>(this).apply {
    put("@id", UUID.randomUUID().toString())
    put("@event_name", eventname)
    put("@opprettet", LocalDateTime.now().toString())
}
