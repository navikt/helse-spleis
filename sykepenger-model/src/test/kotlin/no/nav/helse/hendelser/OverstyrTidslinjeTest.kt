package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class OverstyrTidslinjeTest {

    @Test
    fun `overstyring uten dager`() {
        assertThrows<RuntimeException> {
            OverstyrTidslinje(UUID.randomUUID(), "orgnr", emptyList(), LocalDateTime.now())
        }
    }
}
