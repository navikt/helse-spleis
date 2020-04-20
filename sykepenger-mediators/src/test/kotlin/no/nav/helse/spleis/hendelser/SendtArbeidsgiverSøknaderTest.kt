package no.nav.helse.spleis.hendelser

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import org.junit.jupiter.api.Test
import java.util.*

internal class SendtArbeidsgiverSøknaderTest : RiverTest() {

    @Test
    fun `valid json`() {
        assertNoErrors(validJson)
    }

    override fun river(rapidsConnection: RapidsConnection, mediator: IMessageMediator) {
        SendtArbeidsgiverSøknader(rapidsConnection, mediator)
    }

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
  "aktorId": "aktørid",
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
  "egenmeldinger": [
    {
      "fom": "2020-01-01",
      "tom": "2020-01-01"
    }
  ],
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
  "arbeidsgiverForskutterer": null
}""".trimIndent()
}
