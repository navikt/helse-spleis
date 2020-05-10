package no.nav.helse.økonomi

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SamletSykdomsgradTest {

    @Test internal fun singelsykegrad() {
        assertEquals(
            Grad.sykdomsgrad(75), listOf(
                Grad.sykdomsgrad(75).lønn(1200)).samletGrad())
    }

    @Test internal fun `to arbeidsgivere`() {
        assertEquals(
            Grad.sykdomsgrad(38),
            listOf(
                Grad.sykdomsgrad(50).lønn(1200), Grad.sykdomsgrad(
                    20
                ).lønn(800)).samletGrad()
        )
    }

    @Test internal fun `flere arbeidsgivere`() {
        assertEquals(
            Grad.sykdomsgrad(49),
            listOf(
                Grad.sykdomsgrad(50).lønn(1200),
                Grad.sykdomsgrad(20).lønn(800),
                Grad.sykdomsgrad(60).lønn(2000)
            ).samletGrad()
        )
    }
}
