package no.nav.helse.unit.spleis.hendelser.model

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.spleis.hendelser.MessageProblems
import no.nav.helse.spleis.hendelser.model.SendtSøknadMessage
import no.nav.syfo.kafka.sykepengesoknad.dto.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class SendtSøknadMessageTest {

    private val InvalidJson = "foo"
    private val UnknownJson = "{\"foo\": \"bar\"}"
    private val ValidSøknad = SykepengesoknadDTO(
        id = UUID.randomUUID().toString(),
        type = SoknadstypeDTO.ARBEIDSTAKERE,
        status = SoknadsstatusDTO.SENDT,
        aktorId = "aktørId",
        fnr = "fødselsnummer",
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
        soknadsperioder = listOf(SoknadsperiodeDTO(
            fom = LocalDate.now(),
            tom = LocalDate.now(),
            sykmeldingsgrad = 100,
            faktiskGrad = 100,
            avtaltTimer = Double.MIN_VALUE,
            faktiskTimer = Double.MAX_VALUE,
            sykmeldingstype = SykmeldingstypeDTO.AKTIVITET_IKKE_MULIG
        )),
        fravar = listOf(FravarDTO(fom = LocalDate.now(), tom = LocalDate.now()))
    )

    private val ValidSendtSøknad = ValidSøknad.copy(status = SoknadsstatusDTO.SENDT).toJson()
    private val ValidAvbruttSøknad = ValidSøknad.copy(status = SoknadsstatusDTO.AVBRUTT).toJson()
    private val ValidSendtSøknadWithUnknownFieldsJson = ValidSøknad.copy(status = SoknadsstatusDTO.SENDT).asJsonNode().let {
        it as ObjectNode
        it.put(UUID.randomUUID().toString(), "foobar")
    }.toJson()

    @Test
    internal fun `invalid messages`() {
        assertInvalidMessage(InvalidJson)
        assertInvalidMessage(UnknownJson)
        assertInvalidMessage(ValidAvbruttSøknad)
    }

    @Test
    internal fun `valid søknader`() {
        assertValidSøknadMessage(ValidSendtSøknadWithUnknownFieldsJson)
        assertValidSøknadMessage(ValidSendtSøknad)
    }

    private fun assertValidSøknadMessage(message: String) {
        val problems = MessageProblems(message)
        SendtSøknadMessage(message, problems)
        assertFalse(problems.hasErrors()) { "was supposed to recognize $message: $problems" }
    }

    private fun assertInvalidMessage(message: String) {
        MessageProblems(message).also {
            SendtSøknadMessage(message, it)
            assertTrue(it.hasErrors()) { "was not supposed to recognize $message" }
        }
    }

    private var recognizedSøknad = false
    @BeforeEach
    fun reset() {
        recognizedSøknad = false
    }
}

private val objectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

private fun SykepengesoknadDTO.asJsonNode(): JsonNode = objectMapper.valueToTree(this)
private fun SykepengesoknadDTO.toJson(): String = objectMapper.writeValueAsString(this)
private fun JsonNode.toJson(): String = objectMapper.writeValueAsString(this)
