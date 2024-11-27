package no.nav.helse.spleis.mediator.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.AnmodningOmForkastingRiver
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.util.UUID

internal class AnmodningOmForkastingRiverTest : RiverTest() {
    override fun river(
        rapidsConnection: RapidsConnection,
        mediator: IMessageMediator,
    ) {
        AnmodningOmForkastingRiver(rapidsConnection, mediator)
    }

    @Test
    fun `Kan mappe om message til modell uten feil`() {
        assertNoErrors(json())
    }

    @Language("JSON")
    private fun json(vedtaksperiodeId: String? = UUID.randomUUID().toString()) =
        """
      {
          "@event_name": "anmodning_om_forkasting",
          "@id": "${UUID.randomUUID()}",
          "vedtaksperiodeId": "$vedtaksperiodeId",
          "organisasjonsnummer": "orgnummer",
          "@opprettet": "2023-04-03T11:25:00",
          "fødselsnummer": "08127411111"
        }
    """
}
