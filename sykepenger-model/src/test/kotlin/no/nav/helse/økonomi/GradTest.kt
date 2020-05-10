package no.nav.helse.økonomi

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class GradTest {

    @Test
    internal fun `opprette med Int`() {
        assertEquals(
            Grad.sykdomsgrad(20.0),
            Grad.sykdomsgrad(20)
        )
    }

    @Test
    internal fun `arbeid helse og sykdom karakter er motsetninger`() {
        assertEquals(
            Grad.sykdomsgrad(25),
            Grad.arbeidshelse(75)
        )
    }

    @Test
    internal fun minimumssyke() {
        assertTrue(Grad.sykdomsgrad(25) > Grad.GRENSE)
        assertTrue(Grad.sykdomsgrad(20) == Grad.GRENSE)
        assertTrue(Grad.sykdomsgrad(15) < Grad.GRENSE)
    }

    @Test
    internal fun `parameterskontroll av sykdomsgrad`() {
        assertThrows<IllegalArgumentException> {
            Grad.sykdomsgrad(
                -0.001
            )
        }
        assertThrows<IllegalArgumentException> {
            Grad.arbeidshelse(
                100.001
            )
        }
    }
}
