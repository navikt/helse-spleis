package no.nav.helse.utbetalingstidslinje

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

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
