package no.nav.helse.økonomi

import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class ØkonomiTest {

    @Test
    fun `kan ikke sette dagsats mer enn en gang`() {
        assertThrows<IllegalStateException> {
            25.prosent.sykdomsgrad.inntekt(1200.daglig, 1200.daglig, skjæringstidspunkt = 1.januar).inntekt(
                800.daglig,
                800.daglig,
                skjæringstidspunkt = 1.januar
            )
        }
    }

    @Test
    fun singelsykegrad() {
        assertEquals(
            75.prosent,
            listOf(
                75.prosent.sykdomsgrad.inntekt(1200.daglig, 1200.daglig, skjæringstidspunkt = 1.januar)
            ).totalSykdomsgrad()
        )
    }

    @Test
    fun singelsykegradUtenInntekt() {
        assertEquals(
            75.prosent,
            listOf(
                75.prosent.sykdomsgrad.inntekt(0.daglig, 0.daglig, skjæringstidspunkt = 1.januar)
            ).totalSykdomsgrad()
        )
    }

    @Test
    fun `to arbeidsgivere`() {
        assertEquals(
            38.prosent,
            listOf(
                50.prosent.sykdomsgrad.inntekt(1200.daglig, 1200.daglig, skjæringstidspunkt = 1.januar),
                20.prosent.sykdomsgrad.inntekt(800.daglig, 800.daglig, skjæringstidspunkt = 1.januar)
            ).totalSykdomsgrad().also {
                assertFalse(it.erUnderGrensen())
            }
        )
    }

    @Test
    fun `to arbeidsgivereUtenInntekt`() {
        assertEquals(
            35.prosent,
            listOf(
                50.prosent.sykdomsgrad.inntekt(0.daglig, 0.daglig, skjæringstidspunkt = 1.januar),
                20.prosent.sykdomsgrad.inntekt(0.daglig, 0.daglig, skjæringstidspunkt = 1.januar)
            ).totalSykdomsgrad().also {
                assertFalse(it.erUnderGrensen())
            }
        )
    }

    @Test
    fun `flere arbeidsgivere`() {
        assertEquals(
            49.prosent,
            listOf(
                50.prosent.sykdomsgrad.inntekt(1200.daglig, 1200.daglig, skjæringstidspunkt = 1.januar),
                20.prosent.sykdomsgrad.inntekt(800.daglig, 800.daglig, skjæringstidspunkt = 1.januar),
                60.prosent.sykdomsgrad.inntekt(2000.daglig, 2000.daglig, skjæringstidspunkt = 1.januar)
            ).totalSykdomsgrad()
        )
    }

    @Test
    fun `låst økonomi fungerer som arbeidsdag`() {
        assertEquals(
            19.prosent,
            listOf(
                50.prosent.sykdomsgrad.inntekt(1200.daglig, 1200.daglig, skjæringstidspunkt = 1.januar),
                20.prosent.sykdomsgrad.inntekt(800.daglig, 800.daglig, skjæringstidspunkt = 1.januar),
                60.prosent.sykdomsgrad.inntekt(2000.daglig, 2000.daglig, skjæringstidspunkt = 1.januar).lås()
            ).totalSykdomsgrad().also {
                assertTrue(it.erUnderGrensen())
            }
        )
    }

    @Test
    fun `kan låse igjen hvis allerede låst`() {
        assertDoesNotThrow { 50.prosent.sykdomsgrad.inntekt(1200.daglig, 1200.daglig, skjæringstidspunkt = 1.januar).lås().lås() }
    }

    @Test
    fun `kan ikke låses etter betaling`() {
        50.prosent.sykdomsgrad.inntekt(1200.daglig, 1200.daglig, skjæringstidspunkt = 1.januar).also { økonomi ->
            listOf(økonomi).betal(1.januar)
            assertUtbetaling(økonomi, 600.0, 0.0)
            assertThrows<IllegalStateException> { økonomi.lås() }
        }
    }

    @Test
    fun `opplåsing tillater betaling`() {
        50.prosent.sykdomsgrad.inntekt(1200.daglig, 1200.daglig, skjæringstidspunkt = 1.januar).lås().låsOpp().also { økonomi ->
            listOf(økonomi).betal(1.januar)
            assertUtbetaling(økonomi, 600.0, 0.0)
        }
    }

    @Test
    fun `kan ikke låses opp med mindre den er låst`() {
        assertThrows<IllegalStateException> { 50.prosent.sykdomsgrad.låsOpp() }
        50.prosent.sykdomsgrad.inntekt(1200.daglig, 1200.daglig, skjæringstidspunkt = 1.januar).also { økonomi ->
            assertThrows<IllegalStateException> { økonomi.låsOpp() }
            listOf(økonomi).betal(1.januar)
            assertUtbetaling(økonomi, 600.0, 0.0)
            assertThrows<IllegalStateException> { økonomi.låsOpp() }
        }
    }

    @Test
    fun `dekningsgrunnlag returns clone`() {
        50.prosent.sykdomsgrad.also { original ->
            assertNotSame(original, original.inntekt(1200.daglig, 1200.daglig, skjæringstidspunkt = 1.januar))
        }
    }

    @Test
    fun `betal 0 hvis låst`() {
        50.prosent.sykdomsgrad.inntekt(1200.daglig, 1200.daglig, skjæringstidspunkt = 1.januar).lås().also { økonomi ->
            listOf(økonomi).betal(1.januar)
            assertUtbetaling(økonomi, 0.0, 0.0)
            økonomi.låsOpp()
            listOf(økonomi).betal(1.januar)
            assertUtbetaling(økonomi, 600.0, 0.0)
        }
    }

    @Test
    fun `kan ikke låses etter utbetaling`() {
        50.prosent.sykdomsgrad.inntekt(1200.daglig, 1200.daglig, skjæringstidspunkt = 1.januar).also { økonomi ->
            listOf(økonomi).betal(1.januar)
            assertThrows<IllegalStateException> { økonomi.lås() }
        }
    }

    @Test
    fun `toMap uten dekningsgrunnlag`() {
        79.5.prosent.sykdomsgrad
            .medData { grad, _, dekningsgrunnlag, _, _, _, _, _, _ ->
                assertEquals(79.5, grad)
                assertNull(dekningsgrunnlag)
            }
    }

    @Test
    fun `toMap med dekningsgrunnlag`() {
        79.5.prosent.sykdomsgrad.inntekt(1200.4.daglig, 1200.4.daglig, skjæringstidspunkt = 1.januar)
            .medData { grad, _, dekningsgrunnlag, _, _, _, _, _, _ ->
                assertEquals(79.5, grad)
                assertEquals(1200.4, dekningsgrunnlag)
            }
    }

    @Test
    fun `toIntMap med dekningsgrunnlag`() {
        79.5.prosent.sykdomsgrad.inntekt(1200.4.daglig, 1200.4.daglig, skjæringstidspunkt = 1.januar)
            .medAvrundetData { grad, _, dekningsgrunnlag, _, _, _, _ ->
                assertEquals(80, grad)
                assertEquals(1200, dekningsgrunnlag)
            }
    }

    @Test
    fun `kan beregne betaling bare en gang`() {
        assertDoesNotThrow {
            listOf(80.prosent.sykdomsgrad.inntekt(1200.daglig, 1200.daglig, skjæringstidspunkt = 1.januar)).betal(1.januar).betal(1.januar)
        }
    }

    @Test
    fun `Beregn utbetaling når mindre enn 6G`() {
        80.prosent.sykdomsgrad.inntekt(1200.daglig, 1200.daglig, skjæringstidspunkt = 1.januar).also {
            listOf(it).betal(1.januar)
            it.medData { grad, _, dekningsgrunnlag, _, _, _, arbeidsgiverbeløp, personbeløp, _ ->
                assertEquals(80.0, grad)
                assertEquals(1200.0, dekningsgrunnlag)
                assertEquals(960.0, arbeidsgiverbeløp)
                assertEquals(0.0, personbeløp)
            }
            it.medAvrundetData { grad, _, dekningsgrunnlag, _, arbeidsgiverbeløp, personbeløp, _ ->
                assertEquals(80, grad)
                assertEquals(1200, dekningsgrunnlag)
                assertEquals(960, arbeidsgiverbeløp)
                assertEquals(0, personbeløp)
            }
        }
    }

    @Test
    fun `arbeidsgiver og person splittes tilsvarer totalt`() {
        Økonomi.sykdomsgrad(100.prosent)
            .inntekt(999.daglig, skjæringstidspunkt = 1.januar)
            .arbeidsgiverRefusjon(499.5.daglig)
            .also {
                listOf(it).betal(1.januar)
                it.medData { grad, arbeidsgiverBetalingProsent, dekningsgrunnlag, _, _, _, arbeidsgiverbeløp, personbeløp, _ ->
                    assertEquals(100.0, grad)
                    assertEquals(999.0, dekningsgrunnlag)
                    assertEquals(500.0, arbeidsgiverbeløp)
                    assertEquals(499.0, personbeløp)
                }
            }
    }

    @Test
    fun `tre arbeidsgivere uten grenser`() {
        val a = Økonomi.sykdomsgrad(50.prosent).inntekt(600.daglig, skjæringstidspunkt = 1.januar).arbeidsgiverRefusjon(150.daglig)
        val b = Økonomi.sykdomsgrad(20.prosent).inntekt(400.daglig, skjæringstidspunkt = 1.januar).arbeidsgiverRefusjon(80.daglig)
        val c = Økonomi.sykdomsgrad(60.prosent).inntekt(1000.daglig, skjæringstidspunkt = 1.januar).arbeidsgiverRefusjon(0.daglig)
        listOf(a, b, c).betal(1.januar).also {
            assertEquals(49.prosent, it.totalSykdomsgrad())
        }
        listOf(a, b, c).forEach {
            assertFalse(it.er6GBegrenset())
        }
        assertUtbetaling(a, 150.0, 150.0)
        assertUtbetaling(b, 80.0, 0.0)
        assertUtbetaling(c, 0.0, 600.0)
    }

    @Test
    fun `tre arbeidsgivere med persongrense`() {
        val a = Økonomi.sykdomsgrad(50.prosent).inntekt(1200.daglig, skjæringstidspunkt = 1.januar).arbeidsgiverRefusjon(600.daglig * 50.prosent)
        val b = Økonomi.sykdomsgrad(20.prosent).inntekt(800.daglig, skjæringstidspunkt = 1.januar).arbeidsgiverRefusjon(800.daglig * 20.prosent)
        val c = Økonomi.sykdomsgrad(60.prosent).inntekt(2000.daglig, skjæringstidspunkt = 1.januar).arbeidsgiverRefusjon(0.daglig)
        listOf(a, b, c).betal(1.januar).also {
            assertEquals(49.prosent, it.totalSykdomsgrad())
            // grense = 1059
        }
        listOf(a, b, c).forEach {
            assertTrue(it.er6GBegrenset())
        }
        assertUtbetaling(a, 300.0, 120.0)
        assertUtbetaling(b, 160.0, 0.0)
        assertUtbetaling(c, 0.0, 479.0)
    }

    @Test
    fun `tre arbeidsgivere med arbeidsgivere`() {
        val a = Økonomi.sykdomsgrad(50.prosent).inntekt(4800.daglig, skjæringstidspunkt = 1.januar).arbeidsgiverRefusjon(2400.daglig * 50.prosent)
        val b = Økonomi.sykdomsgrad(20.prosent).inntekt(3200.daglig, skjæringstidspunkt = 1.januar).arbeidsgiverRefusjon(3200.daglig * 20.prosent)
        val c = Økonomi.sykdomsgrad(60.prosent).inntekt(8000.daglig, skjæringstidspunkt = 1.januar).arbeidsgiverRefusjon(0.daglig)
        listOf(a, b, c).betal(1.januar).also {
            assertEquals(49.prosent, it.totalSykdomsgrad())
            // grense = 1059
        }
        listOf(a, b, c).forEach {
            assertTrue(it.er6GBegrenset())
        }
        assertUtbetaling(
            a,
            691.0, 0.0
        )  // (1059 / (1200 + 640)) * 1200
        assertUtbetaling(b, 368.0, 0.0)
        assertUtbetaling(c, 0.0, 0.0)
    }

    @Test
    fun `eksempel fra regneark`() {
        val a = Økonomi.sykdomsgrad(50.prosent).inntekt(21000.månedlig, skjæringstidspunkt = 1.januar).arbeidsgiverRefusjon(10500.månedlig)
        val b = Økonomi.sykdomsgrad(80.prosent).inntekt(10000.månedlig, skjæringstidspunkt = 1.januar).arbeidsgiverRefusjon(7200.månedlig)
        val c = Økonomi.sykdomsgrad(20.prosent).inntekt(31000.månedlig, skjæringstidspunkt = 1.januar).arbeidsgiverRefusjon(1550.månedlig)
        listOf(a, b, c).betal(1.januar).also {
            assertEquals(39.838709677419345.prosent, it.totalSykdomsgrad())
            // grense = 864
        }
        listOf(a, b, c).forEach {
            assertTrue(it.er6GBegrenset())
        }
        assertUtbetaling(a, 470.0, 0.0)
        assertUtbetaling(b, 322.0, 0.0)
        assertUtbetaling(c, 69.0, 0.0)
    }

    @Test
    fun `eksempel fra regneark modifisert for utbetaling til arbeidstaker`() {
        val a = Økonomi.sykdomsgrad(50.prosent).inntekt(21000.månedlig, skjæringstidspunkt = 1.januar).arbeidsgiverRefusjon(21000.månedlig * 50.prosent)
        val b = Økonomi.sykdomsgrad(20.prosent).inntekt(10000.månedlig, skjæringstidspunkt = 1.januar).arbeidsgiverRefusjon(10000.månedlig * 20.prosent * 20.prosent)
        val c = Økonomi.sykdomsgrad(20.prosent).inntekt(31000.månedlig, skjæringstidspunkt = 1.januar).arbeidsgiverRefusjon(31000.månedlig * 25.prosent * 20.prosent)
        listOf(a, b, c).betal(1.januar).also {
            assertEquals(30.16129032258064.prosent, it.totalSykdomsgrad())
            // grense = 864
        }
        listOf(a, b, c).forEach {
            assertTrue(it.er6GBegrenset())
        }
        assertUtbetaling(a, 485.0, 0.0)
        assertUtbetaling(b, 18.0, 20.0)
        assertUtbetaling(c, 72.0, 57.0)
    }

    @Test
    fun `Sykdomdsgrad rundes opp`() {
        val a = Økonomi.sykdomsgrad(20.prosent).inntekt(10000.daglig, 10000.daglig, skjæringstidspunkt = 1.januar)
        val b = Økonomi.sykdomsgrad(21.prosent).inntekt(10000.daglig, 10000.daglig, skjæringstidspunkt = 1.januar)
        listOf(a, b).betal(1.januar).also {
            assertEquals(20.5.prosent, it.totalSykdomsgrad()) //dekningsgrunnlag 454
        }
        assertUtbetaling(a, 216.0, 0.0) //454 * 2000 / 4100 ~+1
        assertUtbetaling(b, 227.0, 0.0)
    }

    private fun assertUtbetaling(økonomi: Økonomi, expectedArbeidsgiver: Double, expectedPerson: Double) {
        økonomi.medData { _, _, _, _, _, _, arbeidsgiverbeløp, personbeløp, _ ->
            assertEquals(expectedArbeidsgiver, arbeidsgiverbeløp, "arbeidsgiverbeløp problem")
            assertEquals(expectedPerson, personbeløp, "personbeløp problem")
        }
    }
}

internal val Prosentdel.sykdomsgrad get() = Økonomi.sykdomsgrad(this)
