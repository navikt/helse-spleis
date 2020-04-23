package no.nav.helse.spleis.meldinger

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import org.junit.jupiter.api.Test
import java.util.*

internal class KansellerUtbetalingerRiverRiverTest : RiverTest() {
    override fun river(rapidsConnection: RapidsConnection, mediator: IMessageMediator) {
        KansellerUtbetalingerRiver(rapidsConnection, mediator)
    }

    @Test
    fun `Kan mappe om message til modell uten feil`() {
        assertNoErrors(json)
    }

    @Test
    fun `Får problems når vi mangler påkrevde felt`() {
        assertIgnored(badJson)
    }

    private val json = """
    {
        "@id": "${UUID.randomUUID()}",
        "@opprettet": "2020-01-24T11:25:00",
        "@event_name": "kanseller_utbetaling",
        "aktørId": "999",
        "fødselsnummer": "08127411111",
        "organisasjonsnummer": "orgnummer",
        "fagsystemId": "ABCD1234",
        "saksbehandler": "Ola Nordmann"
    }
""".trimIndent()

    private val badJson = """
    {
        "@id": "${UUID.randomUUID()}",
        "@opprettet": "2020-01-24T11:25:00",
        "@event_name": "KansellerUtbetaling",
        "aktørId": "999",
        "fødselsnummer": "08127411111",
        "organisasjonsnummer": "orgnummer",
        "fagsystemId": "ABCD1234",
        "saksbehandler": "Ola Nordmann"
    }
""".trimIndent()
}
