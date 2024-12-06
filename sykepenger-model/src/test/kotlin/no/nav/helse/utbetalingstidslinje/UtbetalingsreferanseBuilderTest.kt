package no.nav.helse.utbetalingstidslinje

import java.util.UUID
import no.nav.helse.utbetalingslinjer.decodeUtbetalingsreferanse
import no.nav.helse.utbetalingslinjer.genererUtbetalingsreferanse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class UtbetalingsreferanseBuilderTest {

    @Test
    fun `encode decode`() {
        val uuid = UUID.randomUUID()
        val encoded = genererUtbetalingsreferanse(uuid)
        val decoded = decodeUtbetalingsreferanse(encoded)
        assertEquals(uuid, decoded)
        assertEquals(26, encoded.length)
    }
}
