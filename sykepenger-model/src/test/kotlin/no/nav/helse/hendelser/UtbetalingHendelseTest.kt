package no.nav.helse.hendelser

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class UtbetalingHendelseTest {

    @Test
    fun `er relevant`() {
        val fagsystemId = "en fagsystem id"
        val utbetalingId = UUID.randomUUID()
        assertTrue(utbetalinghendelse(fagsystemId, utbetalingId).erRelevant(fagsystemId))
        assertTrue(utbetalinghendelse(fagsystemId, utbetalingId).erRelevant(fagsystemId, utbetalingId))
        assertFalse(utbetalinghendelse(fagsystemId, utbetalingId).erRelevant("en annen fagsystem id"))
        assertFalse(utbetalinghendelse(fagsystemId, utbetalingId).erRelevant(fagsystemId, UUID.randomUUID()))
    }

    private fun utbetalinghendelse(fagsystemId: String, utbetalingId: UUID) = UtbetalingHendelse(
        UUID.randomUUID(),
        "vedtaksperiodeId",
        "aktørId",
        "fnr",
        "orgnr",
        fagsystemId,
        "$utbetalingId",
        UtbetalingHendelse.Oppdragstatus.AKSEPTERT,
        "melding",
        LocalDateTime.now(),
        "Z999999",
        "saksbehandler@nav.no",
        false
    )
}
