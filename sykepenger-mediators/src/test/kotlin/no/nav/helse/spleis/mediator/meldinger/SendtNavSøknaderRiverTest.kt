package no.nav.helse.spleis.mediator.meldinger

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.desember
import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidsgiverDTO
import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidsgiverForskuttererDTO
import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidssituasjonDTO
import no.nav.helse.flex.sykepengesoknad.kafka.FravarDTO
import no.nav.helse.flex.sykepengesoknad.kafka.FravarstypeDTO
import no.nav.helse.flex.sykepengesoknad.kafka.MerknadDTO
import no.nav.helse.flex.sykepengesoknad.kafka.PeriodeDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsstatusDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadstypeDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SykmeldingstypeDTO
import no.nav.helse.januar
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.flex.sykepengesoknad.kafka.InntektFraNyttArbeidsforholdDTO
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.mediator.TestMessageFactory
import no.nav.helse.spleis.meldinger.SendtNavSøknaderRiver
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class SendtNavSøknaderRiverTest : RiverTest() {

    private val fødselsdato = 12.desember(1995)
    private val aktørId = "42"
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
        sykmeldingSkrevet = LocalDateTime.now(),
        sendtArbeidsgiver = LocalDateTime.now(),
        egenmeldinger = emptyList(),
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
    fun `Gyldig søknad med inntekt fra nytt arbeidsforhold`() {
        val factory = TestMessageFactory("1", "2", "3", 31000.0, 1.januar(1990))
        val (_, søknad) = factory.lagSøknadNav(
            perioder = listOf(SoknadsperiodeDTO(1.januar, 31.januar, 100)),
            inntektFraNyttArbeidsforhold = listOf(
                InntektFraNyttArbeidsforholdDTO(
                    fom = 10.januar,
                    tom = 31.januar,
                    forsteArbeidsdag = 10.januar,
                    forstegangssporsmal = true,
                    belopPerDag = 1000,
                    arbeidsstedOrgnummer = "4",
                    opplysningspliktigOrgnummer = "5"
                    )
            ))

        assertNoErrors(søknad)
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
    fun `parser søknad med egenmeldingsdager`() {
        assertNoErrors(validSøknad().copy(egenmeldingsdagerFraSykmelding = emptyList()).toJson())
        assertNoErrors(validSøknad().copy(egenmeldingsdagerFraSykmelding = null).toJson())
        assertNoErrors(validSøknad().copy(egenmeldingsdagerFraSykmelding = listOf(1.januar, 2.januar)).toJson())
        assertNoErrors(validSøknad().copy(egenmeldingsdagerFraSykmelding = listOf()).toJson())
        assertIgnored(søknadMedRareEgenmeldinger)
    }

    @Test
    fun `parser søknad med merknader (fra sykmelding)`() {
        assertNoErrors(validSøknad().copy(merknaderFraSykmelding = emptyList()).toJson())
        assertNoErrors(validSøknad().copy(merknaderFraSykmelding = null).toJson())
        assertNoErrors(validSøknad().copy(merknaderFraSykmelding = listOf(MerknadDTO("UGYLDIG_TILBAKEDATERING", null))).toJson())
        assertNoErrors(validSøknad().copy(merknaderFraSykmelding = listOf(MerknadDTO("TILBAKEDATERING_KREVER_FLERE_OPPLYSNINGER", "En beskrivelse"))).toJson())
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
    put("@event_name", if (this["status"].asText() == "SENDT") "sendt_søknad_nav" else "ukjent")
    put("@opprettet", LocalDateTime.now().toString())
}

@Language("JSON")
private val søknadMedRareEgenmeldinger = """
    {
      "id": "36275131-8fea-4173-aee3-94ca91dd69c0",
      "type": "ARBEIDSTAKERE",
      "status": "SENDT",
      "fnr": "fødselsnummer",
      "sykmeldingId": "a7d705ad-7c30-4d0a-b7e5-a80621801735",
      "arbeidsgiver": {
        "navn": "arbeidsgiver",
        "orgnummer": "orgnr"
      },
      "arbeidssituasjon": "ARBEIDSTAKER",
      "korrigerer": "korrigerer",
      "korrigertAv": null,
      "soktUtenlandsopphold": null,
      "arbeidsgiverForskutterer": "JA",
      "fom": "2023-06-05",
      "tom": "2023-06-05",
      "dodsdato": null,
      "startSyketilfelle": "2023-06-05",
      "arbeidGjenopptatt": "2023-06-05",
      "sykmeldingSkrevet": "2023-06-05T10:57:56.15885",
      "opprettet": "2023-06-05T10:57:56.158845",
      "opprinneligSendt": null,
      "sendtNav": "2023-06-05T10:57:56.158849",
      "sendtArbeidsgiver": "2023-06-05T10:57:56.158856",
      "egenmeldinger": null,
      "fravarForSykmeldingen": null,
      "papirsykmeldinger": [],
      "fravar": [],
      "andreInntektskilder": null,
      "soknadsperioder": [
        {
          "fom": "2023-06-05",
          "tom": "2023-06-05",
          "sykmeldingsgrad": 100,
          "faktiskGrad": 100,
          "avtaltTimer": 4.9E-324,
          "faktiskTimer": 1.7976931348623157E308,
          "sykmeldingstype": "AKTIVITET_IKKE_MULIG",
          "grad": null
        }
      ],
      "sporsmal": null,
      "avsendertype": null,
      "ettersending": false,
      "mottaker": null,
      "egenmeldtSykmelding": null,
      "yrkesskade": null,
      "arbeidUtenforNorge": null,
      "harRedusertVenteperiode": null,
      "behandlingsdager": null,
      "permitteringer": null,
      "merknaderFraSykmelding": null,
      "egenmeldingsdagerFraSykmelding": ["første januar", "andre januar"],
      "merknader": null,
      "sendTilGosys": null,
      "utenlandskSykmelding": null,
      "@id": "19b951d6-d24e-4b99-8963-b25fbe714b3b",
      "@event_name": "sendt_søknad_nav",
      "@opprettet": "2023-06-05T10:57:56.171373",
      "fødselsdato": "1995-12-12",
      "aktorId": "42"
    }
"""

