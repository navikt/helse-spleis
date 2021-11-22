package no.nav.helse.hendelser

import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.utbetalingslinjer.Oppdragstatus
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
        assertTrue(utbetalinghendelse(fagsystemId, utbetalingId).erRelevant(fagsystemId, "ikke relevant", utbetalingId))
        assertTrue(utbetalinghendelse(fagsystemId, utbetalingId).erRelevant("ikke relevant", fagsystemId, utbetalingId))
        assertFalse(utbetalinghendelse(fagsystemId, utbetalingId).erRelevant("en annen fagsystem id"))
        assertFalse(utbetalinghendelse(fagsystemId, utbetalingId).erRelevant(fagsystemId, "ikke relevant", UUID.randomUUID()))
    }

    private fun utbetalinghendelse(fagsystemId: String, utbetalingId: UUID) = UtbetalingHendelse(
        UUID.randomUUID(),
        "aktørId",
        "fnr",
        "orgnr",
        fagsystemId,
        "$utbetalingId",
        Oppdragstatus.AKSEPTERT,
        "melding",
        1L,
        LocalDateTime.now()
    )
}
