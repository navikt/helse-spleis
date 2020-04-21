package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.syfo.kafka.felles.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class NyeSøknaderRiverTest : RiverTest() {

    private val InvalidJson = "foo"
    private val UnknownJson = "{\"foo\": \"bar\"}"
    private val ValidSøknad = SykepengesoknadDTO(
        id = UUID.randomUUID().toString(),
        type = SoknadstypeDTO.ARBEIDSTAKERE,
        status = SoknadsstatusDTO.NY,
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
        sendtNav = null,
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

    private val ValidNySøknad = ValidSøknad.copy(status = SoknadsstatusDTO.NY).toJson()
    private val ValidAvbruttSøknad = ValidSøknad.copy(status = SoknadsstatusDTO.AVBRUTT).toJson()
    private val ValidNySøknadWithUnknownFieldsJson = ValidSøknad.copy(status = SoknadsstatusDTO.NY).asObjectNode()
        .put(UUID.randomUUID().toString(), "foobar").toJson()

    override fun river(rapidsConnection: RapidsConnection, mediator: IMessageMediator) {
        NyeSøknaderRiver(rapidsConnection, mediator)
    }

    @Test
    internal fun `invalid messages`() {
        assertSevere(InvalidJson)
        assertSevere(UnknownJson)
        assertSevere(ValidAvbruttSøknad)
    }

    @Test
    internal fun `valid søknader`() {
        assertNoErrors(ValidNySøknadWithUnknownFieldsJson)
        assertNoErrors(ValidNySøknad)
    }
}

private val objectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

private fun SykepengesoknadDTO.asObjectNode(): ObjectNode = objectMapper.valueToTree<ObjectNode>(this).apply {
    put("@id", UUID.randomUUID().toString())
    put("@event_name", if (this["status"].asText() == "NY") "ny_søknad" else "ukjent")
    put("@opprettet", LocalDateTime.now().toString())
}
private fun SykepengesoknadDTO.toJson(): String = asObjectNode().toString()
private fun JsonNode.toJson(): String = objectMapper.writeValueAsString(this)
