package no.nav.helse.spleis.mediator.meldinger

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.desember
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidsgiverDTO
import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidsgiverForskuttererDTO
import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidssituasjonDTO
import no.nav.helse.flex.sykepengesoknad.kafka.FravarDTO
import no.nav.helse.flex.sykepengesoknad.kafka.PeriodeDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsstatusDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadstypeDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SykmeldingstypeDTO
import no.nav.helse.spleis.meldinger.NyeSøknaderRiver
import org.junit.jupiter.api.Test

internal class NyeSøknaderRiverTest : RiverTest() {

    private val fødselsdato = 12.desember(1995)
    private val aktørId = "42"
    private val InvalidJson = "foo"
    private val UnknownJson = "{\"foo\": \"bar\"}"
    private val ValidSøknad = SykepengesoknadDTO(
        id = UUID.randomUUID().toString(),
        type = SoknadstypeDTO.ARBEIDSTAKERE,
        status = SoknadsstatusDTO.NY,
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
        sykmeldingSkrevet = LocalDateTime.now(),
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
    private val ValidNySøknadUtenPerioder = ValidSøknad.copy(soknadsperioder = emptyList()).toJson()
    private val ValidNySøknadWithUnknownFieldsJson = ValidSøknad.copy(status = SoknadsstatusDTO.NY).asObjectNode()
        .put(UUID.randomUUID().toString(), "foobar").toJson()

    override fun river(rapidsConnection: RapidsConnection, mediator: IMessageMediator) {
        NyeSøknaderRiver(rapidsConnection, mediator)
    }

    @Test
    fun `invalid messages`() {
        assertIgnored(InvalidJson)
        assertIgnored(UnknownJson)
        assertIgnored(ValidAvbruttSøknad)
    }

    @Test
    fun `valid søknader`() {
        assertNoErrors(ValidNySøknadWithUnknownFieldsJson)
        assertNoErrors(ValidNySøknad)
        assertNoErrors(ValidNySøknadUtenPerioder)
    }
    private fun SykepengesoknadDTO.toJson(): String = asObjectNode().medFelterFraSpedisjon().toString()
    private fun ObjectNode.toJson(): String = medFelterFraSpedisjon().toString()
    private fun ObjectNode.medFelterFraSpedisjon() = put("fødselsdato", "$fødselsdato").put("aktorId", "$aktørId")
}
private val objectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
private fun SykepengesoknadDTO.asObjectNode(): ObjectNode = objectMapper.valueToTree<ObjectNode>(this).apply {
    put("@id", UUID.randomUUID().toString())
    put("@event_name", if (this["status"].asText() == "NY") "ny_søknad" else "ukjent")
    put("@opprettet", LocalDateTime.now().toString())
}
