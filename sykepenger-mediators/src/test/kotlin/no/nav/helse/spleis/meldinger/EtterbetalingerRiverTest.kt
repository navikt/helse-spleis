package no.nav.helse.spleis.meldinger

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class EtterbetalingerRiverTest : RiverTest() {
    override fun river(rapidsConnection: RapidsConnection, mediator: IMessageMediator) {
        EtterbetalingerRiver(rapidsConnection, mediator)
    }

    @Test
    fun `Kan mappe om message til modell uten feil`() {
        assertNoErrors(json)
    }

    @Test
    fun `Melding uten fagsystem id feiler`() {
        assertErrors(jsonUtenFagsystemId)
    }

}

@Language("JSON")
private val json = """
{
  "@id":"053d20f5-b881-4afc-99b0-6d8e650d0050",
  "@event_name":"Etterbetalingskandidat_v1",
  "@opprettet":"2020-11-03T11:26:40.6399197",
  "fagsystemId":"YNQXJGM73ZHPBBTM7LVG5RJPYM",
  "aktørId":"1000000000091",
  "fødselsnummer":"22027821111",
  "organisasjonsnummer":"971555001",
  "gyldighetsdato":"2020-11-03"
}
"""

@Language("JSON")
private val jsonUtenFagsystemId = """
{
  "@id":"053d20f5-b881-4afc-99b0-6d8e650d0050",
  "@event_name":"Etterbetalingskandidat_v1",
  "@opprettet":"2020-11-03T11:26:40.6399197",
  "aktørId":"1000000000091",
  "fødselsnummer":"22027821111",
  "organisasjonsnummer":"971555001",
  "gyldighetsdato":"2020-11-03"
}
"""



