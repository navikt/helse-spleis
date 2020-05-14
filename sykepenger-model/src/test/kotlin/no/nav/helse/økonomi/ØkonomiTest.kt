package no.nav.helse.økonomi

import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class ØkonomiTest {  // bare arbeidsgiver betalte

    @Test
    internal fun minimumssyke() {
        assertFalse(25.prosent.sykdomsgrad.erUnderGrensen())
        assertFalse(20.prosent.sykdomsgrad.erUnderGrensen())
        assertTrue(15.prosent.sykdomsgrad.erUnderGrensen())

        assertFalse(Økonomi.arbeidshelse(75.prosent).erUnderGrensen())
        assertFalse(Økonomi.arbeidshelse(80.prosent).erUnderGrensen())
        assertTrue(Økonomi.arbeidshelse(85.prosent).erUnderGrensen())
    }

    @Test internal fun `kan ikke sette lønn mer enn en gang`() {
        assertThrows<IllegalStateException>{ 25.prosent.sykdomsgrad.lønn(1200).lønn(800) }
    }

    @Test internal fun singelsykegrad() {
        assertEquals(
            75.prosent,
            listOf(
                75.prosent.sykdomsgrad.lønn(1200)
            ).samletGrad()
        )
    }

    @Test internal fun `to arbeidsgivere`() {
        assertEquals(
            38.prosent,
            listOf(
                50.prosent.sykdomsgrad.lønn(1200),
                20.prosent.sykdomsgrad.lønn(800)
            ).samletGrad()
        )
    }

    @Test internal fun `flere arbeidsgivere`() {
        assertEquals(
            49.prosent,
            listOf(
                50.prosent.sykdomsgrad.lønn(1200),
                20.prosent.sykdomsgrad.lønn(800),
                60.prosent.sykdomsgrad.lønn(2000)
            ).samletGrad()
        )
    }

    @Test internal fun `toMap uten lønn`() {
        79.5.prosent.sykdomsgrad.toMap().apply {
            assertEquals(79.5, this["grad"])
            assertEquals(100.0, this["arbeidsgiverBetalingProsent"])
            assertNull(this["lønn"])
        }
    }

    @Test internal fun `toMap med lønn`() {
        79.5.prosent.sykdomsgrad.lønn(1200.4).toMap().apply {
            assertEquals(79.5, this["grad"])
            assertEquals(100.0, this["arbeidsgiverBetalingProsent"])
            assertEquals(1200.4, this["lønn"])
        }
    }

    @Test internal fun `toIntMap med lønn`() {
        79.5.prosent.sykdomsgrad.lønn(1200.4).toIntMap().apply {
            assertEquals(80, this["grad"])
            assertEquals(100, this["arbeidsgiverBetalingProsent"])
            assertEquals(1200, this["lønn"])
        }
    }

    @Test internal fun `Beregn utbetaling når mindre enn 6G`() {
        80.prosent.sykdomsgrad.lønn(1200).also {
            listOf(it).betale(1.januar)
            it.toMap().apply {
                assertEquals(80.0, this["grad"])
                assertEquals(100.0, this["arbeidsgiverBetalingProsent"])
                assertEquals(1200.0, this["lønn"])
                assertEquals(960, this["arbeidsgiversutbetaling"])
                assertEquals(0, this["personUtbetaling"])
            }
            it.toIntMap().apply {
                assertEquals(80, this["grad"])
                assertEquals(100, this["arbeidsgiverBetalingProsent"])
                assertEquals(1200, this["lønn"])
                assertEquals(960, this["arbeidsgiversutbetaling"])
                assertEquals(0, this["personUtbetaling"])
            }
        }
    }

    @Test internal fun `arbeidsgiver og person splittes tilsvarer totalt`() {
        Økonomi.sykdomsgrad(100.prosent, 50.prosent).lønn(999).also {
            listOf(it).betale(1.januar)
            it.toMap().apply {
                assertEquals(100.0, this["grad"])
                assertEquals(50.0, this["arbeidsgiverBetalingProsent"])
                assertEquals(999.0, this["lønn"])
                assertEquals(500, this["arbeidsgiversutbetaling"])
                assertEquals(499, this["personUtbetaling"])
            }
        }
    }
}

internal val Prosentdel.sykdomsgrad get() = Økonomi.sykdomsgrad(this, 100.prosent)
