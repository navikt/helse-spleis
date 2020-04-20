package no.nav.helse.unit.spleis.hendelser.model

import io.mockk.ConstantAnswer
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.spleis.MessageMediator
import no.nav.helse.spleis.hendelser.KansellerUtbetalinger
import no.nav.helse.unit.spleis.hendelser.TestRapid
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class KansellerUtbetalingMessageTest {

    @Test fun `Kan mappe om message til modell uten feil`() {
        rapid.sendTestMessage(json)
        assertTrue(recognizedMessage)
    }

    @Test fun `Får problems når vi mangler påkrevde felt`() {
        rapid.sendTestMessage(badJson)
        assertTrue(riverSevere)
    }

    private var riverError = false
    private var riverSevere = false
    private var recognizedMessage = false
    @BeforeEach
    fun reset() {
        recognizedMessage = false
        riverError = false
        riverSevere = false
        rapid.reset()
    }

    private val messageMediator = mockk<MessageMediator>()
    private val rapid = TestRapid().apply {
        KansellerUtbetalinger(this, messageMediator)
    }
    init {
        every {
            messageMediator.onRecognizedMessage(any(), any())
        } answers {
            recognizedMessage = true
            ConstantAnswer(Unit)
        }
        every {
            messageMediator.onRiverError(any(), any(), any())
        } answers {
            riverError = true
            ConstantAnswer(Unit)
        }
        every {
            messageMediator.onRiverSevere(any(), any(), any())
        } answers {
            riverSevere = true
            ConstantAnswer(Unit)
        }
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
