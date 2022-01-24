package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.januar
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.syfo.kafka.felles.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class SendtNavSøknaderRiverTest : RiverTest() {

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
        ),
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
        sykmeldingSkrevet = LocalDateTime.now(),
        sendtArbeidsgiver = LocalDateTime.now(),
        egenmeldinger = listOf(PeriodeDTO(fom = LocalDate.now(), tom = LocalDate.now())),
        soknadsperioder = soknadsperioder,
        papirsykmeldinger = emptyList(),
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
    private val søknadMedPermisjon = validSøknad(
        fravar = listOf(
            FravarDTO(
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                type = FravarstypeDTO.PERMISJON
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
        SendtNavSøknaderRiver(rapidsConnection, mediator)
    }

    @Test
    fun `invalid messages`() {
        assertIgnored(invalidJson)
        assertIgnored(unknownJson)
        assertIgnored(validAvbruttSøknad)
    }

    @Test
    fun `ukjent fraværskode`() {
        assertErrors(ukjentFraværskode)
    }

    @Test
    fun `valid søknader`() {
        assertNoErrors(validSendtSøknadWithUnknownFieldsJson)
        assertNoErrors(validSendtSøknad)
    }

    @Test
    fun `søknad med utlandsopphold`() {
        assertNoErrors(søknadMedUtlandsopphold)
    }

    @Test
    fun `søknad med permisjon`() {
        assertNoErrors(søknadMedPermisjon)
    }

    @Test
    fun `søknad med faktisk grad større enn 100 gir en gyldig sykdomsgrad`() {
        assertNoErrors(validSendtSøknadMedFaktiskGradStørreEnn100)
    }

    @Test
    fun `parser søknad med permitteringer`() {
        assertNoErrors(validSøknad().copy(permitteringer = emptyList()).toJson())
        assertNoErrors(validSøknad().copy(permitteringer = null).toJson())
        assertNoErrors(validSøknad().copy(permitteringer = listOf(PeriodeDTO(1.januar, 31.januar))).toJson())
        assertNoErrors(validSøknad().copy(permitteringer = listOf(PeriodeDTO(1.januar, null))).toJson())
    }

    @Test
    fun `parser søknad med merknader (fra sykmelding)`() {
        assertNoErrors(validSøknad().copy(merknaderFraSykmelding = emptyList()).toJson())
        assertNoErrors(validSøknad().copy(merknaderFraSykmelding = null).toJson())
        assertNoErrors(validSøknad().copy(merknaderFraSykmelding = listOf(MerknadDTO("UGYLDIG_TILBAKEDATERING", null))).toJson())
        assertNoErrors(validSøknad().copy(merknaderFraSykmelding = listOf(MerknadDTO("TILBAKEDATERING_KREVER_FLERE_OPPLYSNINGER", "En beskrivelse"))).toJson())
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
