package no.nav.helse.spleis.mediator.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import java.util.UUID
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.SendtArbeidsgiverSøknaderRiver
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class SendtArbeidsgiverSøknaderRiverTest : RiverTest() {

    @Test
    fun `valid json`() {
        assertNoErrors(validJson)
    }

    override fun river(rapidsConnection: RapidsConnection, mediator: IMessageMediator) {
        SendtArbeidsgiverSøknaderRiver(rapidsConnection, mediator)
    }

    @Language("JSON")
    private val validJson = """
{
  "@event_name": "sendt_søknad_arbeidsgiver",
  "@id": "${UUID.randomUUID()}",
  "@opprettet": "2020-01-01T00:00:00.000",
  "id": "id",
  "fnr": "fnr",
  "fom": "2020-01-01",
  "tom": "2020-01-01",
  "type": "ARBEIDSTAKERE",
  "fravar": [],
  "status": "SENDT",
  "mottaker": "ARBEIDSGIVER",
  "sendtNav": null,
  "opprettet": "2020-01-01T00:00:00.000",
  "korrigerer": null,
  "korrigertAv": null,
  "arbeidsgiver": {
    "navn": "ARBEIDSGIVERNAVN",
    "orgnummer": "orgnr"
  },
  "avsendertype": "BRUKER",
  "ettersending": false,
  "sykmeldingId": "sykmeldingid",
  "soknadsperioder": [
    {
      "fom": "2020-01-01",
      "tom": "2020-01-01",
      "avtaltTimer": null,
      "faktiskGrad": null,
      "faktiskTimer": null,
      "sykmeldingsgrad": 100,
      "sykmeldingstype": "AKTIVITET_IKKE_MULIG"
    }
  ],
  "arbeidssituasjon": "ARBEIDSTAKER",
  "arbeidGjenopptatt": null,
  "papirsykmeldinger": [],
  "sendtArbeidsgiver": "2020-01-01T00:00:00.000",
  "startSyketilfelle": "2020-01-01",
  "sykmeldingSkrevet": "2020-01-01T00:00:00",
  "andreInntektskilder": [],
  "soktUtenlandsopphold": null,
  "arbeidsgiverForskutterer": null,
  "merknaderFraSykmelding": null,
  "fødselsdato": "1992-12-02",
  "egenmeldingsdagerFraSykmelding": ["2019-12-31"]
}""".trimIndent()
}
