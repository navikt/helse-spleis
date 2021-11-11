package no.nav.helse.person.infotrygdhistorikk

import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class UgyldigPeriodeTest {

    @Test
    fun `like verdier`() {
        val første = UgyldigPeriode(1.januar, 2.januar, 100)
        val andre = UgyldigPeriode(1.januar, 2.januar, 100)
        assertEquals(første, andre)
        assertEquals(første.hashCode(), andre.hashCode())
    }

    @Test
    fun `ulik fom`() {
        val første = UgyldigPeriode(1.januar, 2.januar, 100)
        val andre = UgyldigPeriode(3.januar, 2.januar, 100)
        assertNotEquals(første, andre)
        assertNotEquals(første.hashCode(), andre.hashCode())
    }

    @Test
    fun `ulik tom`() {
        val første = UgyldigPeriode(1.januar, 2.januar, 100)
        val andre = UgyldigPeriode(1.januar, 3.januar, 100)
        assertNotEquals(første, andre)
        assertNotEquals(første.hashCode(), andre.hashCode())
    }

    @Test
    fun `ulik grad`() {
        val første = UgyldigPeriode(1.januar, 2.januar, 100)
        val andre = UgyldigPeriode(1.januar, 2.januar, 50)
        assertNotEquals(første, andre)
        assertNotEquals(første.hashCode(), andre.hashCode())
    }
}
