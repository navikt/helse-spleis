package no.nav.helse.spleis.mediator.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import java.util.UUID
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.AnnullerUtbetalingerRiver
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class AnnullerUtbetalingerRiverRiverTest : RiverTest() {
    override fun river(rapidsConnection: RapidsConnection, mediator: IMessageMediator) {
        AnnullerUtbetalingerRiver(rapidsConnection, mediator)
    }

    @Test
    fun `Kan mappe om message til modell uten feil`() {
        assertNoErrors(json)
    }

    @Test
    fun `Får problems når vi mangler påkrevde felt`() {
        assertIgnored(badJson)
    }

    @Language("JSON")
    private val json = """{
    "@id": "${UUID.randomUUID()}",
    "@opprettet": "2020-01-24T11:25:00",
    "@event_name": "annullering",
    "fødselsnummer": "08127411111",
    "organisasjonsnummer": "orgnummer",
    "vedtaksperiodeId": "${UUID.randomUUID()}",
    "saksbehandler": {
        "navn": "Siri Saksbhandler",
        "epostaddresse": "siri.saksbehandler@nav.no",
        "oid": "${UUID.randomUUID()}",
        "ident": "S1234567"
    },
    "begrunnelser": ["Annet"]
}
""".trimIndent()

    @Language("JSON")
    private val badJson = """
    {
        "@id": "${UUID.randomUUID()}",
        "@opprettet": "2020-01-24T11:25:00",
        "@event_name": "KansellerUtbetaling",
        "fødselsnummer": "08127411111",
        "organisasjonsnummer": "orgnummer",
        "fagsystemId": "ABCD1234",
        "saksbehandler": "Ola Nordmann",
        "saksbehandlerEpost": "tbd@nav.no"
    }
""".trimIndent()
}
