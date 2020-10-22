package no.nav.helse.hendelser

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class UtbetalingHendelseTest {

    @Test
    fun `er relevant`() {
        val fagsystemId = "en fagsystem id"
        assertTrue(utbetalinghendelse(fagsystemId).erRelevant(fagsystemId))
        assertFalse(utbetalinghendelse(fagsystemId).erRelevant("en annen fagsystem id"))
    }

    private fun utbetalinghendelse(fagsystemId: String) = UtbetalingHendelse(
        UUID.randomUUID(),
        "vedtaksperiodeId",
        "aktørId",
        "fnr",
        "orgnr",
        fagsystemId,
        UtbetalingHendelse.Oppdragstatus.AKSEPTERT,
        "melding",
        LocalDateTime.now(),
        "Z999999",
        "saksbehandler@nav.no",
        false
    )
}
