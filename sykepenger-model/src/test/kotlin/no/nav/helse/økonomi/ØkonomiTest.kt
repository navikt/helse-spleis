package no.nav.helse.økonomi

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.Double.Companion.NEGATIVE_INFINITY
import kotlin.Double.Companion.NaN
import kotlin.Double.Companion.POSITIVE_INFINITY

internal class ØkonomiTest {

    @Test internal fun `ugyldige verdier`() {
        listOf<Double>(
            -0.01,
            POSITIVE_INFINITY,
            NEGATIVE_INFINITY,
            NaN
        ).forEach {
            assertThrows<IllegalArgumentException> { Økonomi.lønn(it, Grad.sykdomsgrad(100)) }
        }
    }

    @Test internal fun toMap() {
        Økonomi.lønn(1200.4, Grad.sykdomsgrad(79.5)).toMap().apply {
            assertEquals(1200.4, this["lønn"])
            assertEquals(79.5, this["grad"])
        }
    }

    @Test internal fun toIntMap() {
        Økonomi.lønn(1200.4, Grad.sykdomsgrad(79.5)).toIntMap().apply {
            assertEquals(1200, this["lønn"])
            assertEquals(80, this["grad"])
        }
    }

    @Test internal fun `arbeidsgiver bare utbetaling`() {
        Økonomi
            .lønn(1200, Grad.sykdomsgrad(80))
            .betalte(100.prosentdel).apply {
                assertEquals(960, this.arbeidsgiverutbetaling())
                this.toMap().apply {
                        assertEquals(1200.0, this["lønn"])
                        assertEquals(960, this["arbeidsgiverutbetaling"])
                        assertEquals(0, this["personutbetaling"])
                        assertEquals(80.0, this["grad"])
                    }
                this.toIntMap().apply {
                        assertEquals(1200, this["lønn"])
                        assertEquals(960, this["arbeidsgiverutbetaling"])
                        assertEquals(0, this["personutbetaling"])
                        assertEquals(80, this["grad"])
                    }
            }
    }
}
