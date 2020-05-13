package no.nav.helse.økonomi

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class GradØkonomiTest {  // bare arbeidsgiver betalte

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

//    @Test internal fun toIntMap() {
//        Økonomi.lønn(1200.4, Grad.sykdomsgrad(79.5)).toIntMap().apply {
//            assertEquals(1200, this["lønn"])
//            assertEquals(80, this["grad"])
//        }
//    }
//
//    @Test internal fun `arbeidsgiver bare utbetaling`() {
//        Økonomi
//            .lønn(1200, Grad.sykdomsgrad(80))
//            .betalte(100.prosentdel).apply {
//                assertEquals(960, this.arbeidsgiverutbetaling())
//                assertEquals(0, this.personUtbetaling())
//                this.toMap().apply {
//                        assertEquals(1200.0, this["lønn"])
//                        assertEquals(960, this["arbeidsgiverutbetaling"])
//                        assertEquals(0, this["personutbetaling"])
//                        assertEquals(80.0, this["grad"])
//                    }
//                this.toIntMap().apply {
//                        assertEquals(1200, this["lønn"])
//                        assertEquals(960, this["arbeidsgiverutbetaling"])
//                        assertEquals(0, this["personutbetaling"])
//                        assertEquals(80, this["grad"])
//                    }
//            }
//    }
//
//    @Test internal fun `arbeidsgiver og person splittes tilsvarer totalt`() {
//        Økonomi
//            .lønn(999, Grad.sykdomsgrad(100))
//            .betalte(50.prosentdel).apply {
//                assertEquals(500, this.arbeidsgiverutbetaling())
//                assertEquals(499, this.personUtbetaling())
//            }
//    }
}

internal val Prosentdel.sykdomsgrad get() = Økonomi.sykdomsgrad(this, 100.prosent)
