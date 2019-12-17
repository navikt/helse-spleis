package no.nav.helse.fixtures

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SykdomstidslinjerTest {

    @BeforeEach
    internal fun reset() {
        resetSeed()
    }

    @Test
    internal fun `simple patterns`() {
        val tidslinje = 10.S
        Assertions.assertEquals(10, tidslinje.size)
        Assertions.assertEquals(10.januar, tidslinje.last().dato)
    }

    @Test
    internal fun `combinations`() {
        val tidslinje = 10.S + 5.F + 3.S
        Assertions.assertEquals(18, tidslinje.size)
        Assertions.assertEquals(18.januar, tidslinje.last().dato)
    }
}
