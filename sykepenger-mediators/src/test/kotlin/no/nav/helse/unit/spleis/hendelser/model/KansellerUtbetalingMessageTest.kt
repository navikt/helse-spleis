package no.nav.helse.unit.spleis.hendelser.model

import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.spleis.hendelser.model.KansellerUtbetalingMessage
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

internal class KansellerUtbetalingMessageTest {

    @Test fun `Kan mappe om message til modell uten feil`() {
        val problems = MessageProblems(json)
        assertDoesNotThrow { KansellerUtbetalingMessage(json, problems).asKansellerUtbetaling() }
        assertFalse(problems.hasErrors())
    }

    @Test fun `Får problems når vi mangler påkrevde felt`() {
        val problems = MessageProblems(badJson)
        assertDoesNotThrow { KansellerUtbetalingMessage(badJson, problems) }
        assertTrue(problems.hasErrors())
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
