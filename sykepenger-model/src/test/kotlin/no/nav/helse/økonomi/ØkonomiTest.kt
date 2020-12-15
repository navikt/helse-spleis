package no.nav.helse.økonomi

import no.nav.helse.testhelpers.januar
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class ØkonomiTest {

    @Test fun `kan ikke sette dagsats mer enn en gang`() {
        assertThrows<IllegalStateException>{ 25.prosent.sykdomsgrad.inntekt(1200.daglig).inntekt(800.daglig) }
    }

    @Test fun singelsykegrad() {
        assertEquals(
            75.prosent,
            listOf(
                75.prosent.sykdomsgrad.inntekt(1200.daglig)
            ).sykdomsgrad()
        )
    }

    @Test fun `to arbeidsgivere`() {
        assertEquals(
            38.prosent,
            listOf(
                50.prosent.sykdomsgrad.inntekt(1200.daglig),
                20.prosent.sykdomsgrad.inntekt(800.daglig)
            ).sykdomsgrad().also {
                assertFalse(it.erUnderGrensen())
            }
        )
    }

    @Test fun `flere arbeidsgivere`() {
        assertEquals(
            49.prosent,
            listOf(
                50.prosent.sykdomsgrad.inntekt(1200.daglig),
                20.prosent.sykdomsgrad.inntekt(800.daglig),
                60.prosent.sykdomsgrad.inntekt(2000.daglig)
            ).sykdomsgrad()
        )
    }

    @Test fun `låst økonomi fungerer som arbeidsdag`() {
        assertEquals(
            19.prosent,
            listOf(
                50.prosent.sykdomsgrad.inntekt(1200.daglig),
                20.prosent.sykdomsgrad.inntekt(800.daglig),
                60.prosent.sykdomsgrad.inntekt(2000.daglig).lås()
            ).sykdomsgrad().also {
                assertTrue(it.erUnderGrensen())
            }
        )
    }

    @Test fun `kan låse igjen hvis allerede låst`() {
        assertDoesNotThrow { 50.prosent.sykdomsgrad.inntekt(1200.daglig).lås().lås()}
    }

    @Test fun `kan ikke låses etter betaling`() {
        50.prosent.sykdomsgrad.inntekt(1200.daglig).also { økonomi ->
            listOf(økonomi).betal(1.januar)
            assertUtbetaling(økonomi, 600, 0)
            assertThrows<IllegalStateException> { økonomi.lås() }
        }
    }

    @Test fun `opplåsing tillater betaling`() {
        50.prosent.sykdomsgrad.inntekt(1200.daglig).lås().låsOpp().also { økonomi ->
            listOf(økonomi).betal(1.januar)
            assertUtbetaling(økonomi, 600, 0)
        }
    }

    @Test fun `kan ikke låses opp med mindre den er låst`() {
        assertThrows<IllegalStateException> { 50.prosent.sykdomsgrad.låsOpp() }
        50.prosent.sykdomsgrad.inntekt(1200.daglig).also { økonomi ->
            assertThrows<IllegalStateException> { økonomi.låsOpp() }
            listOf(økonomi).betal(1.januar)
            assertUtbetaling(økonomi, 600, 0)
            assertThrows<IllegalStateException> { økonomi.låsOpp() }
        }
    }

    @Test fun `dekningsgrunnlag returns clone`() {
        50.prosent.sykdomsgrad.also { original ->
            assertNotSame(original, original.inntekt(1200.daglig))
        }
    }

    @Test fun `betal 0 hvis låst`() {
        50.prosent.sykdomsgrad.inntekt(1200.daglig).lås().also { økonomi ->
            listOf(økonomi).betal(1.januar)
            assertUtbetaling(økonomi, 0, 0)
            økonomi.låsOpp()
            listOf(økonomi).betal(1.januar)
            assertUtbetaling(økonomi, 600, 0)
        }
    }

    @Test fun `kan ikke låses etter utbetaling`() {
        50.prosent.sykdomsgrad.inntekt(1200.daglig).also { økonomi ->
            listOf(økonomi).betal(1.januar)
            assertThrows<IllegalStateException> { økonomi.lås() }
        }
    }

    @Test
    fun `toMap uten dekningsgrunnlag`() {
        79.5.prosent.sykdomsgrad
            .reflection { grad, arbeidsgiverBetalingProsent, dekningsgrunnlag, _, _, _, _, _ ->
                assertEquals(79.5, grad)
                assertEquals(100.0, arbeidsgiverBetalingProsent)
                assertNull(dekningsgrunnlag)
            }
    }

    @Test
    fun `toMap med dekningsgrunnlag`() {
        79.5.prosent.sykdomsgrad.inntekt(1200.4.daglig)
            .reflection { grad, arbeidsgiverBetalingProsent, dekningsgrunnlag, _, _, _, _, _ ->
                assertEquals(79.5, grad)
                assertEquals(100.0, arbeidsgiverBetalingProsent)
                assertEquals(1200.4, dekningsgrunnlag)
            }
    }

    @Test
    fun `toIntMap med dekningsgrunnlag`() {
        79.5.prosent.sykdomsgrad.inntekt(1200.4.daglig)
            .reflectionRounded { grad, arbeidsgiverBetalingProsent, dekningsgrunnlag, _, _, _, _ ->
                assertEquals(80, grad)
                assertEquals(100, arbeidsgiverBetalingProsent)
                assertEquals(1200, dekningsgrunnlag)
            }
    }

    @Test fun `kan beregne betaling bare en gang`() {
        assertDoesNotThrow {
            listOf(80.prosent.sykdomsgrad.inntekt(1200.daglig)).betal(1.januar).betal(1.januar)
        }
    }

    @Test fun `Beregn utbetaling når mindre enn 6G`() {
        80.prosent.sykdomsgrad.inntekt(1200.daglig).also {
            listOf(it).betal(1.januar)
            it.reflection { grad, arbeidsgiverBetalingProsent, dekningsgrunnlag, _, _, arbeidsgiverbeløp, personbeløp, _ ->
                assertEquals(80.0, grad)
                assertEquals(100.0, arbeidsgiverBetalingProsent)
                assertEquals(1200.0, dekningsgrunnlag)
                assertEquals(960, arbeidsgiverbeløp)
                assertEquals(0, personbeløp)
            }
            it.reflectionRounded { grad, arbeidsgiverBetalingProsent, dekningsgrunnlag, _, arbeidsgiverbeløp, personbeløp, _ ->
            assertEquals(80, grad)
                assertEquals(100, arbeidsgiverBetalingProsent)
                assertEquals(1200, dekningsgrunnlag)
                assertEquals(960, arbeidsgiverbeløp)
                assertEquals(0, personbeløp)
            }
        }
    }

    @Test fun `arbeidsgiver og person splittes tilsvarer totalt`() {
        Økonomi.sykdomsgrad(100.prosent, 50.prosent).inntekt(999.daglig).also {
            listOf(it).betal(1.januar)
            it.reflection { grad, arbeidsgiverBetalingProsent, dekningsgrunnlag, _, _, arbeidsgiverbeløp, personbeløp, _ ->
            assertEquals(100.0, grad)
                assertEquals(50.0, arbeidsgiverBetalingProsent)
                assertEquals(999.0, dekningsgrunnlag)
                assertEquals(500, arbeidsgiverbeløp)
                assertEquals(499, personbeløp)
            }
        }
    }

    @Test fun `tre arbeidsgivere uten grenser`() {
        val a =  Økonomi.sykdomsgrad(50.prosent, 50.prosent).inntekt(600.daglig)
        val b =  Økonomi.sykdomsgrad(20.prosent, 100.prosent).inntekt(400.daglig)
        val c =  Økonomi.sykdomsgrad(60.prosent, 0.prosent).inntekt(1000.daglig)
        listOf(a, b, c).betal(1.januar).also {
            assertEquals(49.prosent, it.sykdomsgrad())
        }
        listOf(a, b, c).forEach {
            assertFalse(it.er6GBegrenset())
        }
        assertUtbetaling(a, 150, 150)
        assertUtbetaling(b, 80, 0)
        assertUtbetaling(c, 0, 600)
    }

    @Test fun `tre arbeidsgivere med persongrense`() {
        val a =  Økonomi.sykdomsgrad(50.prosent, 50.prosent).inntekt(1200.daglig)
        val b =  Økonomi.sykdomsgrad(20.prosent, 100.prosent).inntekt(800.daglig)
        val c =  Økonomi.sykdomsgrad(60.prosent, 0.prosent).inntekt(2000.daglig)
        listOf(a, b, c).betal(1.januar).also {
            assertEquals(49.prosent, it.sykdomsgrad())
            // grense = 1059
        }
        listOf(a, b, c).forEach {
            assertTrue(it.er6GBegrenset())
        }
        assertUtbetaling(a, 300, 120)
        assertUtbetaling(b, 160, 0)
        assertUtbetaling(c, 0, 479)
    }

    @Test fun `tre arbeidsgivere med arbeidsgivere`() {
        val a =  Økonomi.sykdomsgrad(50.prosent, 50.prosent).inntekt(4800.daglig)
        val b =  Økonomi.sykdomsgrad(20.prosent, 100.prosent).inntekt(3200.daglig)
        val c =  Økonomi.sykdomsgrad(60.prosent, 0.prosent).inntekt(8000.daglig)
        listOf(a, b, c).betal(1.januar).also {
            assertEquals(49.prosent, it.sykdomsgrad())
            // grense = 1059
        }
        listOf(a, b, c).forEach {
            assertTrue(it.er6GBegrenset())
        }
        assertUtbetaling(a, 691, 0)  // (1059 / (1200 + 640)) * 1200
        assertUtbetaling(b, 368, 0)
        assertUtbetaling(c, 0, 0)
    }

    @Test fun `eksempel fra regneark`() {
        val a =  Økonomi.sykdomsgrad(50.prosent, 100.prosent).inntekt(21000.månedlig)
        val b =  Økonomi.sykdomsgrad(80.prosent, 90.prosent).inntekt(10000.månedlig)
        val c =  Økonomi.sykdomsgrad(20.prosent, 25.prosent).inntekt(31000.månedlig)
        listOf(a, b, c).betal(1.januar).also {
            assertEquals(39.838709677419345.prosent, it.sykdomsgrad())
            // grense = 864
        }
        listOf(a, b, c).forEach {
            assertTrue(it.er6GBegrenset())
        }
        assertUtbetaling(a, 471, 0)
        assertUtbetaling(b, 323, 0)
        assertUtbetaling(c, 70, 0)
    }

    @Test fun `eksempel fra regneark modifisert for utbetaling til arbeidstaker`() {
        val a =  Økonomi.sykdomsgrad(50.prosent, 100.prosent).inntekt(21000.månedlig)
        val b =  Økonomi.sykdomsgrad(20.prosent, 20.prosent).inntekt(10000.månedlig)
        val c =  Økonomi.sykdomsgrad(20.prosent, 25.prosent).inntekt(31000.månedlig)
        listOf(a, b, c).betal(1.januar).also {
            assertEquals(30.16129032258064.prosent, it.sykdomsgrad())
            // grense = 864
        }
        listOf(a, b, c).forEach {
            assertTrue(it.er6GBegrenset())
        }
        assertUtbetaling(a, 485, 0)
        assertUtbetaling(b, 18, 19)
        assertUtbetaling(c, 72, 54)
    }

    @Test fun `Sykdomdsgrad rundes opp`() {
        val a =  Økonomi.sykdomsgrad(20.prosent).inntekt(10000.daglig)
        val b =  Økonomi.sykdomsgrad(21.prosent).inntekt(10000.daglig)
        listOf(a, b).betal(1.januar).also {
            assertEquals(20.5.prosent, it.sykdomsgrad()) //dekningsgrunnlag 454
        }
        assertUtbetaling(a, 221, 0) //454 * 2000 / 4100 ~+1
        assertUtbetaling(b, 233, 0)
    }

    private fun assertUtbetaling(økonomi: Økonomi, expectedArbeidsgiver: Int, expectedPerson: Int) {
        økonomi.reflection { _, _, _, _, _, arbeidsgiverbeløp, personbeløp, _ ->
            assertEquals(expectedArbeidsgiver, arbeidsgiverbeløp, "arbeidsgiverbeløp problem")
            assertEquals(expectedPerson, personbeløp, "personbeløp problem")
        }
    }
}

internal val Prosentdel.sykdomsgrad get() = Økonomi.sykdomsgrad(this, 100.prosent)
