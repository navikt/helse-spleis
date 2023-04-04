package no.nav.helse.spleis.meldinger

import java.util.UUID
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class AnmodningOmForkastingRiverTest : RiverTest() {
    override fun river(rapidsConnection: RapidsConnection, mediator: IMessageMediator) {
        AnmodningOmForkastingRiver(rapidsConnection, mediator)
    }

    @Test
    fun `Kan mappe om message til modell uten feil`() {
        assertNoErrors(json())
    }


    @Language("JSON")
    private fun json(vedtaksperiodeId: String? = UUID.randomUUID().toString()) = """
      {
          "@event_name": "anmodning_om_forkasting",
          "@id": "${UUID.randomUUID()}",
          "vedtaksperiodeId": "$vedtaksperiodeId",
          "organisasjonsnummer": "orgnummer",
          "@opprettet": "2023-04-03T11:25:00",
          "aktørId": "aktørId",
          "fødselsnummer": "08127411111"
        }
    """
}
