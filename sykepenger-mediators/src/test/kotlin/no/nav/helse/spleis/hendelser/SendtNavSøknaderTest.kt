package no.nav.helse.spleis.hendelser

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.testhelpers.januar
import no.nav.syfo.kafka.felles.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class SendtNavSøknaderTest : RiverTest() {

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

    override fun river(rapidsConnection: RapidsConnection, mediator: IMessageMediator) {
        SendtNavSøknader(rapidsConnection, mediator)
    }

    @Test
    internal fun `invalid messages`() {
        assertSevere(invalidJson)
        assertSevere(unknownJson)
        assertSevere(validAvbruttSøknad)
    }

    @Test
    internal fun `ukjent fraværskode`() {
        assertErrors(ukjentFraværskode)
    }

    @Test
    internal fun `valid søknader`() {
        assertNoErrors(validSendtSøknadWithUnknownFieldsJson)
        assertNoErrors(validSendtSøknad)
    }

    @Test
    internal fun `søknad med utlandsopphold`() {
        assertNoErrors(søknadMedUtlandsopphold)
    }

    @Test
    internal fun `søknad med faktisk grad større enn 100 gir en gyldig sykdomsgrad`() {
        assertNoErrors(validSendtSøknadMedFaktiskGradStørreEnn100)
    }

    @Test
    internal fun `parser søknad med permitteringer`() {
        assertNoErrors(validSøknad().copy(permitteringer = emptyList()).toJson())
        assertNoErrors(validSøknad().copy(permitteringer = null).toJson())
        assertNoErrors(validSøknad().copy(permitteringer = listOf(PermitteringDTO(1.januar, 31.januar))).toJson())
        assertNoErrors(validSøknad().copy(permitteringer = listOf(PermitteringDTO(1.januar, null))).toJson())
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
