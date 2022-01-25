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

    @Test
    fun ukedager() {
        assertEquals(1.januar, 30.desember(2017) + 0.ukedager)
        assertEquals(1.januar, 31.desember(2017) + 0.ukedager)
        assertEquals(4.januar, 1.januar + 3.ukedager)
        assertEquals(5.januar, 2.januar + 3.ukedager)
        assertEquals(8.januar, 3.januar + 3.ukedager)
        assertEquals(8.januar, 5.januar + 1.ukedager)
        assertEquals(8.januar, 6.januar + 0.ukedager)
        assertEquals(9.januar, 6.januar + 1.ukedager)
        assertEquals(8.januar, 7.januar + 0.ukedager)
        assertEquals(9.januar, 7.januar + 1.ukedager)
        assertEquals(15.januar, 5.januar + 6.ukedager)
        assertEquals(28.desember, 16.januar + 248.ukedager)
        assertEquals(19.januar, 1.januar + 14.ukedager)
        assertEquals(22.januar, 2.januar + 14.ukedager)
    }
}
