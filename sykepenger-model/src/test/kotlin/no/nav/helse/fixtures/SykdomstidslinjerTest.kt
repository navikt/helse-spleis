package no.nav.helse.fixtures

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SykdomstidslinjerTest {

    @BeforeEach
    fun reset() {
        resetSeed()
    }

    @Test
    fun `tidslinje med kun sykedager`() {
        val tidslinje = 10.S
        assertEquals(10, tidslinje.flatten().size)
        assertEquals(10.januar, tidslinje.flatten().last().sisteDag())
    }

    @Test
    fun `tidslinje med sykedager`() {
        val tidslinje = 10.S + 4.F + 4.A
        assertEquals(18, tidslinje.flatten().size)
        assertEquals(18.januar, tidslinje.flatten().last().sisteDag())
    }
}
