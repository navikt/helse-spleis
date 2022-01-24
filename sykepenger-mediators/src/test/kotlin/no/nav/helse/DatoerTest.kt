package no.nav.helse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class DatoerTest {

    @Test
    fun accuracy() {
        assertEquals(LocalDate.of(2018, 1, 1), 1.mandag)
        assertEquals(1.januar, 1.mandag)
        assertEquals(4.mars, 9.søndag)
    }
}
