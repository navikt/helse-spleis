package no.nav.helse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class MemoizedTest {
    @Test
    fun `regner verdi kun én og bare én gang 1`() {
        val verdier = mutableListOf(1.januar)
        val memoized = Memoized {
            verdier.removeAt(0)
        }

        assertEquals(1.januar, memoized.invoke())
        assertEquals(0, verdier.size)
        assertEquals(1.januar, memoized.invoke())
    }

    @Test
    fun `regner verdi kun én og bare én gang 2`() {
        val verdier = mutableListOf(1.januar)
        val memoized = { verdier.removeAt(0) }.memoized()

        assertEquals(1.januar, memoized())
        assertEquals(0, verdier.size)
        assertEquals(1.januar, memoized())
    }
}
