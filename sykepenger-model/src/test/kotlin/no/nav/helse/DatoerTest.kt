package no.nav.helse

import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDate
import no.nav.helse.hendelser.til
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

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
        assertEquals(5.januar, 5.januar + 0.ukedager)
        assertEquals(8.januar, 3.januar + 3.ukedager)
        assertEquals(8.januar, 5.januar + 1.ukedager)
        assertEquals(8.januar, 6.januar + 0.ukedager)
        assertEquals(9.januar, 6.januar + 1.ukedager)
        assertEquals(8.januar, 7.januar + 0.ukedager)
        assertEquals(8.januar, 8.januar + 0.ukedager)
        assertEquals(9.januar, 7.januar + 1.ukedager)
        assertEquals(15.januar, 5.januar + 6.ukedager)
        assertEquals(28.desember, 16.januar + 248.ukedager)
        assertEquals(19.januar, 1.januar + 14.ukedager)
        assertEquals(22.januar, 2.januar + 14.ukedager)
    }

    @Test
    fun `ukedager mellom eldre dato`() {
        assertEquals(0, (2.januar..1.januar).ukedager())
    }

    @Test
    fun `ukedager mellom seg selv`() {
        assertEquals(0, (1.januar..1.januar).ukedager())
    }

    @Test
    fun `ukedager mellom - start på mandag`() {
        assertEquals(1, (1.januar..2.januar).ukedager())
        assertEquals(2, (1.januar..3.januar).ukedager())
        assertEquals(3, (1.januar..4.januar).ukedager())
        assertEquals(4, (1.januar..5.januar).ukedager())
        assertEquals(5, (1.januar..6.januar).ukedager())
        assertEquals(5, (1.januar..7.januar).ukedager())
        assertEquals(5, (1.januar..8.januar).ukedager())
    }

    @Test
    fun `ukedager mellom - starter på lørdag`() {
        assertEquals(0, (6.januar..6.januar).ukedager())
        assertEquals(0, (6.januar..7.januar).ukedager())
        assertEquals(0, (6.januar..8.januar).ukedager())
        assertEquals(5, (6.januar..13.januar).ukedager())
        assertEquals(5, (6.januar..14.januar).ukedager())
        assertEquals(5, (6.januar..15.januar).ukedager())
    }

    @Test
    fun `ukedager mellom - starter på søndag`() {
        assertEquals(0, (7.januar..7.januar).ukedager())
        assertEquals(5, (7.januar..13.januar).ukedager())
        assertEquals(5, (7.januar..14.januar).ukedager())
        assertEquals(5, (7.januar..15.januar).ukedager())
        assertEquals(10, (6.januar..22.januar).ukedager())
        assertEquals(1326, (1.januar(2016)..31.januar(2021)).ukedager())
    }

    @Test
    fun `ukedager mellom - hensyntar skuddår`() {
        assertEquals(1, (28.februar(2016)..1.mars(2016)).ukedager())
    }

    @Test
    fun `forrige og neste dag`() {
        val mandag = 1.januar
        assertEquals(31.desember(2017), mandag.forrigeDag)
        assertEquals(2.januar, mandag.nesteDag)

        val tirsdag = 2.januar
        assertEquals(1.januar, tirsdag.forrigeDag)
        assertEquals(3.januar, tirsdag.nesteDag)

        val fredag = 5.januar
        assertEquals(4.januar, fredag.forrigeDag)
        assertEquals(6.januar, fredag.nesteDag)
    }

    @Test
    fun `er rett før`() {
        val mandag = 1.januar
        val tirsdag = mandag.nesteDag
        val onsdag = tirsdag.nesteDag
        val torsdag = onsdag.nesteDag
        val fredag = torsdag.nesteDag
        val lørdag = fredag.nesteDag
        val søndag = lørdag.nesteDag

        assertTrue(31.desember(2017).erRettFør(mandag)) { "søndag er rett før mandag" }
        assertFalse(31.desember(2017).erRettFør(tirsdag)) { "søndag er ikke rett før tirsdag" }
        assertTrue(30.desember(2017).erRettFør(mandag)) { "lørdag er rett før mandag" }
        assertTrue(29.desember(2017).erRettFør(mandag)) { "fredag er rett før mandag" }
        assertFalse(28.desember(2017).erRettFør(mandag)) { "forrige torsdag er ikke rett før mandag" }
        assertFalse(mandag.erRettFør(mandag)) { "mandag er ikke rett før seg selv" }

        assertFalse(søndag.erRettFør(tirsdag)) { "søndag er ikke rett før tirsdag" }
        assertTrue(mandag.erRettFør(tirsdag)) { "tirsdag er rett før mandag" }

        assertFalse(torsdag.erRettFør(lørdag)) { "torsdag er ikke rett før lørdag" }
        assertTrue(fredag.erRettFør(lørdag)) { "fredag er rett før lørdag" }
        assertTrue(fredag.erRettFør(søndag)) { "fredag er rett før søndag" }
        assertTrue(lørdag.erRettFør(søndag)) { "lørdag er rett før søndag" }
    }

    @Test
    fun `første, første etter og neste arbeidsdag`() {
        val mandag = mandag(1.januar)
        val tirsdag = tirsdag(2.januar)
        val fredag = fredag(5.januar)
        val lørdag = lørdag(6.januar)
        val søndag = søndag(7.januar)
        val nesteMandag = mandag(8.januar)
        val nesteTirsdag = tirsdag(9.januar)
        assertEquals(mandag, mandag.førsteArbeidsdag())
        assertEquals(tirsdag, mandag.førsteArbeidsdagEtter)
        assertEquals(tirsdag, mandag.nesteArbeidsdag())

        assertEquals(fredag, fredag.førsteArbeidsdag())
        assertEquals(nesteMandag, fredag.førsteArbeidsdagEtter)
        assertEquals(nesteMandag, fredag.nesteArbeidsdag())

        assertEquals(nesteMandag, lørdag.førsteArbeidsdag())
        assertEquals(nesteMandag, lørdag.førsteArbeidsdagEtter)
        assertEquals(nesteTirsdag, lørdag.nesteArbeidsdag())

        assertEquals(nesteMandag, søndag.førsteArbeidsdag())
        assertEquals(nesteMandag, søndag.førsteArbeidsdagEtter)
        assertEquals(nesteTirsdag, søndag.nesteArbeidsdag())
    }

    @Disabled
    @Test
    fun `forskjell mellom ukedager-impl`() {
        val periode = 1.februar(2016) til 31.desember(2020)
        val times = 1000
        val alternative1 = {
            (1.februar(2016) til 31.desember(2020)).ukedager()
        }
        val alternative2 = {
            periode.start.datesUntil(periode.endInclusive).filter { it.dayOfWeek !in setOf(SATURDAY, SUNDAY) }.count()
        }

        tournament(times, alternative1, alternative2)
    }
}
