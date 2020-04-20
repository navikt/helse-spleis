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
import no.nav.helse.spleis.hendelser.SendtNavSøknader
import no.nav.helse.testhelpers.januar
import no.nav.helse.unit.spleis.hendelser.TestRapid
import no.nav.syfo.kafka.felles.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class SendtSøknadNavMessageTest {

    private val invalidJson = "foo"
    private val unknownJson = "{\"foo\": \"bar\"}"
    private fun validSøknad(
        status: SoknadsstatusDTO = SoknadsstatusDTO.SENDT,
        soknadsperioder: List<SoknadsperiodeDTO> = listOf(
            SoknadsperiodeDTO(
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                sykmeldingsgrad = 100,
                faktiskGrad = 100,
                avtaltTimer = Double.MIN_VALUE,
                faktiskTimer = Double.MAX_VALUE,
                sykmeldingstype = SykmeldingstypeDTO.AKTIVITET_IKKE_MULIG
            )
        ),
        fravar: List<FravarDTO> = listOf(
            FravarDTO(
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                type = FravarstypeDTO.FERIE
            )
        )
    ) = SykepengesoknadDTO(
        id = UUID.randomUUID().toString(),
        type = SoknadstypeDTO.ARBEIDSTAKERE,
        status = status,
        aktorId = "aktørId",
        fodselsnummer = SkjultVerdi("fødselsnummer"),
        sykmeldingId = UUID.randomUUID().toString(),
        arbeidsgiver = ArbeidsgiverDTO(navn = "arbeidsgiver", orgnummer = "orgnr"),
        arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER,
        arbeidsgiverForskutterer = ArbeidsgiverForskuttererDTO.JA,
        fom = LocalDate.now(),
        tom = LocalDate.now(),
        startSyketilfelle = LocalDate.now(),
        arbeidGjenopptatt = LocalDate.now(),
        korrigerer = "korrigerer",
        opprettet = LocalDateTime.now(),
        sendtNav = LocalDateTime.now(),
        sendtArbeidsgiver = LocalDateTime.now(),
        egenmeldinger = listOf(PeriodeDTO(fom = LocalDate.now(), tom = LocalDate.now())),
        soknadsperioder = soknadsperioder,
        fravar = fravar
    )

    private val validSendtSøknad = validSøknad().toJson()
    private val validAvbruttSøknad = validSøknad(status = SoknadsstatusDTO.AVBRUTT).toJson()
    private val validSendtSøknadWithUnknownFieldsJson =
        validSøknad()
            .asObjectNode()
            .put(UUID.randomUUID().toString(), "foobar")
            .toJson()
    private val ukjentFraværskode = validSøknad().asObjectNode().also {
        (it.path("fravar").first() as ObjectNode).put("type", "INVALID_FRAVÆRSTYPE")
    }.toJson()
    private val søknadMedUtlandsopphold = validSøknad(
        fravar = listOf(
            FravarDTO(
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                type = FravarstypeDTO.UTLANDSOPPHOLD
            )
        )
    ).toJson()
    private val validSendtSøknadMedFaktiskGradStørreEnn100 = validSøknad(
        soknadsperioder = listOf(
            SoknadsperiodeDTO(
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                sykmeldingsgrad = 100,
                faktiskGrad = 150,
                avtaltTimer = 40.0,
                faktiskTimer = 12.0,
                sykmeldingstype = SykmeldingstypeDTO.AKTIVITET_IKKE_MULIG
            )
        )
    ).toJson()

    @Test
    internal fun `invalid messages`() {
        assertThrows(invalidJson)
        assertThrows(unknownJson)
        assertThrows(validAvbruttSøknad)
    }

    @Test
    internal fun `ukjent fraværskode`() {
        assertInvalidMessage(ukjentFraværskode)
    }

    @Test
    internal fun `valid søknader`() {
        assertValidMessage(validSendtSøknadWithUnknownFieldsJson)
        assertValidMessage(validSendtSøknad)
    }

    @Test
    internal fun `søknad med utlandsopphold`() {
        assertValidMessage(søknadMedUtlandsopphold)
    }

    @Test
    internal fun `søknad med faktisk grad større enn 100 gir en gyldig sykdomsgrad`() {
        assertValidMessage(validSendtSøknadMedFaktiskGradStørreEnn100)
    }

    @Test
    internal fun `parser søknad med permitteringer`() {
        assertValidMessage(validSøknad().copy(permitteringer = emptyList()).toJson())
        assertValidMessage(validSøknad().copy(permitteringer = null).toJson())
        assertValidMessage(validSøknad().copy(permitteringer = listOf(PermitteringDTO(1.januar, 31.januar))).toJson())
        assertValidMessage(validSøknad().copy(permitteringer = listOf(PermitteringDTO(1.januar, null))).toJson())
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
        SendtNavSøknader(this, messageMediator)
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

private fun SykepengesoknadDTO.asObjectNode(): ObjectNode = objectMapper.valueToTree<ObjectNode>(this).apply {
    put("@id", UUID.randomUUID().toString())
    put("@event_name", if (this["status"].asText() == "SENDT") "sendt_søknad_nav" else "ukjent")
    put("@opprettet", LocalDateTime.now().toString())
}

private fun SykepengesoknadDTO.toJson(): String = asObjectNode().toString()
private fun JsonNode.toJson(): String = objectMapper.writeValueAsString(this)
