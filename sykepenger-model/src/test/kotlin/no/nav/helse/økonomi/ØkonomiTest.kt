package no.nav.helse.økonomi

import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class ØkonomiTest {

    @Test fun minimumssyke() {
        assertFalse(25.prosent.sykdomsgrad.erUnderGrensen())
        assertFalse(20.prosent.sykdomsgrad.erUnderGrensen())
        assertTrue(15.prosent.sykdomsgrad.erUnderGrensen())

        assertFalse(Økonomi.arbeidshelse(75.prosent).erUnderGrensen())
        assertFalse(Økonomi.arbeidshelse(80.prosent).erUnderGrensen())
        assertTrue(Økonomi.arbeidshelse(85.prosent).erUnderGrensen())
    }

    @Test fun `kan ikke sette lønn mer enn en gang`() {
        assertThrows<IllegalStateException>{ 25.prosent.sykdomsgrad.lønn(1200).lønn(800) }
    }

    @Test fun singelsykegrad() {
        assertEquals(
            75.prosent,
            listOf(
                75.prosent.sykdomsgrad.lønn(1200)
            ).samletGrad()
        )
    }

    @Test fun `to arbeidsgivere`() {
        assertEquals(
            38.prosent,
            listOf(
                50.prosent.sykdomsgrad.lønn(1200),
                20.prosent.sykdomsgrad.lønn(800)
            ).samletGrad()
        )
    }

    @Test fun `flere arbeidsgivere`() {
        assertEquals(
            49.prosent,
            listOf(
                50.prosent.sykdomsgrad.lønn(1200),
                20.prosent.sykdomsgrad.lønn(800),
                60.prosent.sykdomsgrad.lønn(2000)
            ).samletGrad()
        )
    }

    @Test fun `toMap uten lønn`() {
        79.5.prosent.sykdomsgrad.toMap().apply {
            assertEquals(79.5, this["grad"])
            assertEquals(100.0, this["arbeidsgiverBetalingProsent"])
            assertNull(this["lønn"])
        }
    }

    @Test fun `toMap med lønn`() {
        79.5.prosent.sykdomsgrad.lønn(1200.4).toMap().apply {
            assertEquals(79.5, this["grad"])
            assertEquals(100.0, this["arbeidsgiverBetalingProsent"])
            assertEquals(1200.4, this["lønn"])
        }
    }

    @Test fun `toIntMap med lønn`() {
        79.5.prosent.sykdomsgrad.lønn(1200.4).toIntMap().apply {
            assertEquals(80, this["grad"])
            assertEquals(100, this["arbeidsgiverBetalingProsent"])
            assertEquals(1200, this["lønn"])
        }
    }

    @Test fun `kan beregne betaling bare en gang`() {
        assertThrows<IllegalStateException> {
            listOf(80.prosent.sykdomsgrad.lønn(1200)).betale(1.januar).betale(1.januar)
        }
    }

    @Test fun `Beregn utbetaling når mindre enn 6G`() {
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

    @Test fun `arbeidsgiver og person splittes tilsvarer totalt`() {
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

    @Test fun `tre arbeidsgivere uten grenser`() {
        val a =  Økonomi.sykdomsgrad(50.prosent, 50.prosent).lønn(600)
        val b =  Økonomi.sykdomsgrad(20.prosent, 100.prosent).lønn(400)
        val c =  Økonomi.sykdomsgrad(60.prosent, 0.prosent).lønn(1000)
        listOf(a, b, c).betale(1.januar).also {
            assertEquals(49.prosent, it.samletGrad())
        }
        assertUtbetaling(a, 150, 150)
        assertUtbetaling(b, 80, 0)
        assertUtbetaling(c, 0, 600)
    }

    @Test fun `tre arbeidsgivere med persongrense`() {
        val a =  Økonomi.sykdomsgrad(50.prosent, 50.prosent).lønn(1200)
        val b =  Økonomi.sykdomsgrad(20.prosent, 100.prosent).lønn(800)
        val c =  Økonomi.sykdomsgrad(60.prosent, 0.prosent).lønn(2000)
        listOf(a, b, c).betale(1.januar).also {
            assertEquals(49.prosent, it.samletGrad())
            // grense = 1059
        }
        assertUtbetaling(a, 300, 120)
        assertUtbetaling(b, 160, 0)
        assertUtbetaling(c, 0, 479)
    }

    @Test fun `tre arbeidsgivere med arbeidsgivere`() {
        val a =  Økonomi.sykdomsgrad(50.prosent, 50.prosent).lønn(4800)
        val b =  Økonomi.sykdomsgrad(20.prosent, 100.prosent).lønn(3200)
        val c =  Økonomi.sykdomsgrad(60.prosent, 0.prosent).lønn(8000)
        listOf(a, b, c).betale(1.januar).also {
            assertEquals(49.prosent, it.samletGrad())
            // grense = 1059
        }
        assertUtbetaling(a, 691, 0)  // (1059 / (1200 + 640)) * 1200
        assertUtbetaling(b, 368, 0)
        assertUtbetaling(c, 0, 0)
    }

    private fun assertUtbetaling(økonomi: Økonomi, expectedArbeidsgiver: Int, expectedPerson: Int) {
        økonomi.toMap().also { map ->
            assertEquals(expectedArbeidsgiver, map["arbeidsgiversutbetaling"], "arbeidsgiver utbetaling problem")
            assertEquals(expectedPerson, map["personUtbetaling"], "person utbetaling problem")
        }
    }
}

internal val Prosentdel.sykdomsgrad get() = Økonomi.sykdomsgrad(this, 100.prosent)
