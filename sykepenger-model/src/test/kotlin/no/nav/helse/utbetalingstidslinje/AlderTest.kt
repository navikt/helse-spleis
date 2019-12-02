package no.nav.helse.utbetalingstidslinje

import no.nav.helse.sykdomstidslinje.Alder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class AlderTest {

    private val startDato = LocalDate.of(2018, 1, 1)
    private val sluttDato = LocalDate.of(2019, 1, 1)
    @Test
    fun `ung person`() {
        assertTrue("12020052345".navBurdeBetale(247, LocalDate.of(2018, 12, 28)))
        assertFalse("12020052345".navBurdeBetale(248, LocalDate.of(2018, 12, 28)))
    }

    @Test
    fun `ung person med D-nummer`() {
        assertTrue("52029812345".navBurdeBetale(247, LocalDate.of(2018, 12, 28)))
        assertFalse("52029812345".navBurdeBetale(248, LocalDate.of(2018, 12, 28)))
    }

    @Test
    fun `person som fyller 70 i perioden`() {
        assertTrue("12024812345".navBurdeBetale(2, LocalDate.of(2018, 2, 11)))
        assertFalse("12024812345".navBurdeBetale(2, LocalDate.of(2018, 2, 12)))
    }

    @Test
    fun `person som er 70 før sykeperioden starter`() {
        assertFalse("12024712345".navBurdeBetale(2, LocalDate.of(2018, 2, 12)))
    }

    @Test
    fun `person som fyller 67 i perioden`() {
        assertTrue("12025112345".navBurdeBetale(2, LocalDate.of(2018, 2, 11)))
        assertTrue("12025112345".navBurdeBetale(100, LocalDate.of(2018, 2, 12)))
        assertTrue("12025112345".navBurdeBetale(100, LocalDate.of(2018, 5, 12), 59))
        assertFalse("12025112345".navBurdeBetale(100, LocalDate.of(2018, 5, 12), 60))
        assertFalse("12025112345".navBurdeBetale(248, LocalDate.of(2018, 5, 12), 59))
    }

    @Test
    fun `person som er mellom 67 and 70 i hele perioden`() {
        assertTrue("12025012345".navBurdeBetale(2, LocalDate.of(2018, 2, 11)))
        assertTrue("12025012345".navBurdeBetale(59, LocalDate.of(2018, 2, 11)))
        assertFalse("12025012345".navBurdeBetale(60, LocalDate.of(2018, 2, 11)))
    }

    private fun String.navBurdeBetale(antallDager: Int, dagen: LocalDate, antallDagerEtter67: Int = 0) = Alder(this, startDato, sluttDato).navBurdeBetale(antallDager, antallDagerEtter67, dagen)
}