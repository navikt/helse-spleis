package no.nav.helse.sykdomstidslinje

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SamletSykdomsgradTest {

    @Test internal fun singelsykegrad() {
        assertEquals(Grad.sykdom(75), listOf(Grad.sykdom(75).lønn(1200)).samletGrad())
    }

    @Test internal fun `to arbeidsgivere`() {
        assertEquals(
            Grad.sykdom(38),
            listOf(Grad.sykdom(50).lønn(1200), Grad.sykdom(20).lønn(800)).samletGrad()
        )
    }

    @Test internal fun `flere arbeidsgivere`() {
        assertEquals(
            Grad.sykdom(49),
            listOf(
                Grad.sykdom(50).lønn(1200),
                Grad.sykdom(20).lønn(800),
                Grad.sykdom(60).lønn(2000)
            ).samletGrad()
        )
    }
}
