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

    @Test
    fun datoer() {
        assertEquals(LocalDate.of(2018, 1, 10), 10.januar(2018))
        assertEquals(LocalDate.of(2018, 2, 10), 10.februar(2018))
        assertEquals(LocalDate.of(2018, 3, 10), 10.mars(2018))
        assertEquals(LocalDate.of(2018, 4, 10), 10.april(2018))
        assertEquals(LocalDate.of(2018, 5, 10), 10.mai(2018))
        assertEquals(LocalDate.of(2018, 6, 10), 10.juni(2018))
        assertEquals(LocalDate.of(2018, 7, 10), 10.juli(2018))
        assertEquals(LocalDate.of(2018, 8, 10), 10.august(2018))
        assertEquals(LocalDate.of(2018, 9, 10), 10.september(2018))
        assertEquals(LocalDate.of(2018, 10, 10), 10.oktober(2018))
        assertEquals(LocalDate.of(2018, 11, 10), 10.november(2018))
        assertEquals(LocalDate.of(2018, 12, 10), 10.desember(2018))
    }
}
