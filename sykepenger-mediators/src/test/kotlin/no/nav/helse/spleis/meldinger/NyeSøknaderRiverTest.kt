package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.desember
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.syfo.kafka.felles.ArbeidsgiverDTO
import no.nav.syfo.kafka.felles.ArbeidsgiverForskuttererDTO
import no.nav.syfo.kafka.felles.ArbeidssituasjonDTO
import no.nav.syfo.kafka.felles.FravarDTO
import no.nav.syfo.kafka.felles.PeriodeDTO
import no.nav.syfo.kafka.felles.SkjultVerdi
import no.nav.syfo.kafka.felles.SoknadsperiodeDTO
import no.nav.syfo.kafka.felles.SoknadsstatusDTO
import no.nav.syfo.kafka.felles.SoknadstypeDTO
import no.nav.syfo.kafka.felles.SykepengesoknadDTO
import no.nav.syfo.kafka.felles.SykmeldingstypeDTO
import org.junit.jupiter.api.Test

internal class NyeSøknaderRiverTest : RiverTest() {

    private val fødselsdato = 12.desember(1995)
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
    private fun SykepengesoknadDTO.toJson(): String = asObjectNode().medFødselsdato().toString()
    private fun ObjectNode.toJson(): String = medFødselsdato().toString()
    private fun ObjectNode.medFødselsdato() = put("fødselsdato", "$fødselsdato")
}
private val objectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
private fun SykepengesoknadDTO.asObjectNode(): ObjectNode = objectMapper.valueToTree<ObjectNode>(this).apply {
    put("@id", UUID.randomUUID().toString())
    put("@event_name", if (this["status"].asText() == "NY") "ny_søknad" else "ukjent")
    put("@opprettet", LocalDateTime.now().toString())
}
