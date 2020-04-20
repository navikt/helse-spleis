package no.nav.helse.unit.spleis.hendelser.model

import no.nav.helse.spleis.hendelser.KansellerUtbetalinger
import no.nav.helse.unit.spleis.hendelser.TestMessageMediator
import no.nav.helse.unit.spleis.hendelser.TestRapid
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class KansellerUtbetalingMessageTest {

    @Test fun `Kan mappe om message til modell uten feil`() {
        rapid.sendTestMessage(json)
        assertTrue(messageMediator.recognizedMessage)
    }

    @Test fun `Får problems når vi mangler påkrevde felt`() {
        rapid.sendTestMessage(badJson)
        assertTrue(messageMediator.riverSevereError)
    }

    @BeforeEach
    fun reset() {
        messageMediator.reset()
        rapid.reset()
    }

    private val messageMediator = TestMessageMediator()
    private val rapid = TestRapid().apply {
        KansellerUtbetalinger(this, messageMediator)
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
