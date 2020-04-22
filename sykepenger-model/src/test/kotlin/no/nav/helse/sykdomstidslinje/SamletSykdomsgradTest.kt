package no.nav.helse.sykdomstidslinje

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SamletSykdomsgradTest {

    @Test internal fun singelsykegrad() {
        assertEquals(Grad.sykdom(75), listOf(Grad.sykdom(75).lønn(1200)).samletGrad())
    }
}
