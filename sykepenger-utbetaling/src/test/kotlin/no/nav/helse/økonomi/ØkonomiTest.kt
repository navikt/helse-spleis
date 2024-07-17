package no.nav.helse.økonomi

import java.time.LocalDate
import no.nav.helse.Grunnbeløp.Companion.`6G`
import no.nav.helse.inspectors.inspektør
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.summer
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Prosentdel.Companion.ratio
import no.nav.helse.økonomi.Økonomi.Companion.erUnderGrensen
import no.nav.helse.økonomi.Økonomi.Companion.totalSykdomsgrad
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class ØkonomiTest {

    @Test
    fun `er 6g-begrensning selv om dekningsgrunnlaget er lavere`() {
        val `6g` = `6G`.beløp(1.januar)
        val over6g = `6g` + 260.daglig
        val økonomi = listOf(100.prosent.sykdomsgrad.inntekt(over6g, dekningsgrunnlag = `6g`, `6G` = `6g`))
        val betalte = Økonomi.betal(økonomi)
        assertTrue(betalte.er6GBegrenset()) { "sykepengegrunnlaget regnes ut fra innrapportert inntekt (§ 8-28) og ikke dekningsgrunnlaget" }
    }

    @Test
    fun `akkurat under 20-prosent-grensen`() {
        val økonomi = listOf(
            8.prosent.sykdomsgrad.inntekt(30000.månedlig, `6G` = `6G`.beløp(1.januar)),
            54.prosent.sykdomsgrad.inntekt(10312.40.månedlig, `6G` = `6G`.beløp(1.januar))
        )
        val totalSykdomsgrad = totalSykdomsgrad(økonomi)
        assertNotEquals(20.prosent, totalSykdomsgrad)
        assertTrue(totalSykdomsgrad.erUnderGrensen())
    }

    @Test
    fun `total sykdomsgrad med 0 i inntekter`() {
        val økonomi = listOf(
            100.prosent.sykdomsgrad.inntekt(0.månedlig, `6G` = `6G`.beløp(1.januar)),
            0.prosent.sykdomsgrad.inntekt(0.månedlig, `6G` = `6G`.beløp(1.januar)),
            0.prosent.sykdomsgrad.inntekt(0.månedlig, `6G` = `6G`.beløp(1.januar)),
            0.prosent.sykdomsgrad.inntekt(0.månedlig, `6G` = `6G`.beløp(1.januar)),
            0.prosent.sykdomsgrad.inntekt(0.månedlig, `6G` = `6G`.beløp(1.januar)),
            0.prosent.sykdomsgrad.inntekt(0.månedlig, `6G` = `6G`.beløp(1.januar)),
            0.prosent.sykdomsgrad.inntekt(0.månedlig, `6G` = `6G`.beløp(1.januar)),
        )
        val totalSykdomsgrad = totalSykdomsgrad(økonomi).totalSykdomsgrad()
        assertEquals(14.285714285714286.prosent, totalSykdomsgrad) // 🤔
    }

    @Test
    fun `total sykdomsgrad regnes ut fra aktuell dagsinntekt`() {
        val inntekt = 10000.månedlig
        val økonomi = listOf(
            100.prosent.sykdomsgrad.inntekt(inntekt, dekningsgrunnlag = inntekt, `6G` = `6G`.beløp(1.januar)),
            50.prosent.sykdomsgrad.inntekt(inntekt, dekningsgrunnlag = INGEN, `6G` = `6G`.beløp(1.januar))
        )
        assertEquals(75.prosent, økonomi.totalSykdomsgrad())
    }

    @Test
    fun `to arbeidsgivere gikk inn i en bar og ba om 20 prosent hver`() {
        val inntektA1 = 18199.7.månedlig
        val inntektA2 = 22966.54.månedlig
        val økonomi = listOf(
            20.prosent.sykdomsgrad.inntekt(inntektA1, `6G` = `6G`.beløp(1.januar)),
            20.prosent.sykdomsgrad.inntekt(inntektA2, `6G` = `6G`.beløp(1.januar))
        )
        val abc = ((18199.7 * 0.2) + (22966.54 * 0.2)) / (18199.7 + 22966.54) // 19.999999999999996
        assertEquals(20.prosent, økonomi.totalSykdomsgrad())
        assertFalse(økonomi.totalSykdomsgrad().erUnderGrensen())

    }

    @Test
    fun `kan ikke sette dagsats mer enn en gang`() {
        assertThrows<IllegalStateException> {
            25.prosent.sykdomsgrad.inntekt(1200.daglig, 1200.daglig, `6G` = `6G`.beløp(1.januar)).inntekt(
                800.daglig,
                800.daglig,
                `6G` = `6G`.beløp(1.januar)
            )
        }
    }

    @Test
    fun singelsykegrad() {
        assertEquals(
            75.prosent,
            listOf(
                75.prosent.sykdomsgrad.inntekt(1200.daglig, 1200.daglig, `6G` = `6G`.beløp(1.januar))
            ).totalSykdomsgrad()
        )
    }

    @Test
    fun singelsykegradUtenInntekt() {
        assertEquals(
            75.prosent,
            listOf(
                75.prosent.sykdomsgrad.inntekt(0.daglig, 0.daglig, `6G` = `6G`.beløp(1.januar))
            ).totalSykdomsgrad()
        )
    }

    @Test
    fun `to arbeidsgivere`() {
        assertEquals(
            38.prosent,
            listOf(
                50.prosent.sykdomsgrad.inntekt(1200.daglig, 1200.daglig, `6G` = `6G`.beløp(1.januar)),
                20.prosent.sykdomsgrad.inntekt(800.daglig, 800.daglig, `6G` = `6G`.beløp(1.januar))
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
                50.prosent.sykdomsgrad.inntekt(0.daglig, 0.daglig, `6G` = `6G`.beløp(1.januar)),
                20.prosent.sykdomsgrad.inntekt(0.daglig, 0.daglig, `6G` = `6G`.beløp(1.januar))
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
                50.prosent.sykdomsgrad.inntekt(1200.daglig, 1200.daglig, `6G` = `6G`.beløp(1.januar)),
                20.prosent.sykdomsgrad.inntekt(800.daglig, 800.daglig, `6G` = `6G`.beløp(1.januar)),
                60.prosent.sykdomsgrad.inntekt(2000.daglig, 2000.daglig, `6G` = `6G`.beløp(1.januar))
            ).totalSykdomsgrad()
        )
    }

    @Test
    fun `låst økonomi fungerer ikke som arbeidsdag ved total sykdomsgrad`() {
        assertEquals(
            49.prosent,
            listOf(
                50.prosent.sykdomsgrad.inntekt(1200.daglig, 1200.daglig, `6G` = `6G`.beløp(1.januar)),
                20.prosent.sykdomsgrad.inntekt(800.daglig, 800.daglig, `6G` = `6G`.beløp(1.januar)),
                60.prosent.sykdomsgrad.inntekt(2000.daglig, 2000.daglig, `6G` = `6G`.beløp(1.januar)).lås()
            ).totalSykdomsgrad().also {
                assertFalse(it.erUnderGrensen())
            }
        )
    }

    @Test
    fun `kan låse igjen hvis allerede låst`() {
        assertDoesNotThrow {
            50.prosent.sykdomsgrad.inntekt(1200.daglig, 1200.daglig, `6G` = `6G`.beløp(1.januar)).lås().lås()
        }
    }

    @Test
    fun `kan ikke låses etter betaling`() {
        50.prosent.sykdomsgrad.inntekt(
            1200.daglig,
            1200.daglig,
            `6G` = `6G`.beløp(1.januar),
            refusjonsbeløp = 1200.daglig
        ).also { økonomi ->
            val betalte = listOf(økonomi).betal()
            assertUtbetaling(betalte.single(), 600.0, 0.0)
            assertThrows<IllegalStateException> { betalte.single().lås() }
        }
    }

    @Test
    fun `arbeidsgiverrefusjon med inntekt`() {
        100.prosent.sykdomsgrad.inntekt(1000.daglig, `6G` = `6G`.beløp(1.januar), refusjonsbeløp = 1000.daglig)
            .also { økonomi ->
                assertEquals(1000.daglig, økonomi.inspektør.arbeidsgiverRefusjonsbeløp)
            }
    }

    @Test
    fun `dekningsgrunnlag returns clone`() {
        50.prosent.sykdomsgrad.also { original ->
            assertNotSame(original, original.inntekt(1200.daglig, 1200.daglig, `6G` = `6G`.beløp(1.januar)))
        }
    }

    @Test
    fun `kan ikke låses etter utbetaling`() {
        50.prosent.sykdomsgrad.inntekt(1200.daglig, 1200.daglig, `6G` = `6G`.beløp(1.januar)).also { økonomi ->
            val betalte = listOf(økonomi).betal()
            assertThrows<IllegalStateException> { betalte.single().lås() }
        }
    }

    @Test
    fun `toMap uten dekningsgrunnlag`() {
        val økonomi = 79.5.prosent.sykdomsgrad
        assertEquals(79.5, økonomi.inspektør.grad.toDouble())
        assertEquals(INGEN, økonomi.inspektør.dekningsgrunnlag)
        assertEquals(INGEN, økonomi.inspektør.aktuellDagsinntekt)
    }

    @Test
    fun `toMap med dekningsgrunnlag`() {
        val økonomi = 79.5.prosent.sykdomsgrad.inntekt(1200.4.daglig, 1200.4.daglig, `6G` = `6G`.beløp(1.januar))
        assertEquals(79.5, økonomi.inspektør.grad.toDouble())
        assertEquals(1200.4.daglig, økonomi.inspektør.dekningsgrunnlag)
    }

    @Test
    fun `toIntMap med dekningsgrunnlag`() {
        79.5.prosent.sykdomsgrad.inntekt(1200.4.daglig, 1200.4.daglig, `6G` = `6G`.beløp(1.januar))
            .also { økonomi ->
                assertEquals(79.5, økonomi.inspektør.grad.toDouble())
                assertEquals(1200.4.daglig, økonomi.inspektør.dekningsgrunnlag)
            }
    }

    @Test
    fun `kan beregne betaling bare en gang`() {
        assertThrows<IllegalStateException> {
            listOf(80.prosent.sykdomsgrad.inntekt(1200.daglig, 1200.daglig, `6G` = `6G`.beløp(1.januar)))
                .betal()
                .betal()
        }
    }

    @Test
    fun `Beregn utbetaling når mindre enn 6G`() {
        80.prosent.sykdomsgrad.inntekt(
            1200.daglig,
            1200.daglig,
            `6G` = `6G`.beløp(1.januar),
            refusjonsbeløp = 1200.daglig
        ).also {
            val betalte = listOf(it).betal()
            betalte.single().also { økonomi ->
                assertEquals(80.0, økonomi.inspektør.grad.toDouble())
                assertEquals(1200.daglig, økonomi.inspektør.dekningsgrunnlag)
                assertEquals(960.daglig, økonomi.inspektør.arbeidsgiverbeløp)
                assertEquals(INGEN, økonomi.inspektør.personbeløp)
            }
            betalte.single().also { økonomi ->
                assertEquals(80.0, økonomi.inspektør.grad.toDouble())
                assertEquals(1200.daglig, økonomi.inspektør.dekningsgrunnlag)
                assertEquals(960.daglig, økonomi.inspektør.arbeidsgiverbeløp)
                assertEquals(INGEN, økonomi.inspektør.personbeløp)
            }
        }
    }

    @Test
    fun `arbeidsgiver og person splittes tilsvarer totalt`() {
        100.prosent.sykdomsgrad
            .inntekt(999.daglig, `6G` = `6G`.beløp(1.januar), refusjonsbeløp = 499.5.daglig)
            .also {
                val betalte = listOf(it).betal()
                betalte.single().also { økonomi ->
                    assertEquals(100.0, økonomi.inspektør.grad.toDouble())
                    assertEquals(499.5.daglig, økonomi.inspektør.arbeidsgiverRefusjonsbeløp)
                    assertEquals(999.daglig, økonomi.inspektør.dekningsgrunnlag)
                    assertEquals(500.daglig, økonomi.inspektør.arbeidsgiverbeløp)
                    assertEquals(499.daglig, økonomi.inspektør.personbeløp)
                }
            }
    }

    @Test
    fun `tre arbeidsgivere uten grenser`() {
        val a = 50.prosent.sykdomsgrad.inntekt(600.daglig, `6G` = `6G`.beløp(1.januar), refusjonsbeløp = 300.daglig)
        val b = 20.prosent.sykdomsgrad.inntekt(400.daglig, `6G` = `6G`.beløp(1.januar), refusjonsbeløp = 400.daglig)
        val c = 60.prosent.sykdomsgrad.inntekt(1000.daglig, `6G` = `6G`.beløp(1.januar), refusjonsbeløp = 0.daglig)
        val betalte = listOf(a, b, c).betal().also {
            assertEquals(49.prosent, it.totalSykdomsgrad())
        }
        betalte.forEach {
            assertFalse(it.er6GBegrenset())
        }
        assertUtbetaling(betalte[0], 150.0, 150.0)
        assertUtbetaling(betalte[1], 80.0, 0.0)
        assertUtbetaling(betalte[2], 0.0, 600.0)
    }

    @Test
    fun `tre arbeidsgivere med persongrense`() {
        val a = 50.prosent.sykdomsgrad.inntekt(
            1200.daglig,
            `6G` = `6G`.beløp(1.januar),
            refusjonsbeløp = 1200.daglig * 50.prosent
        )
        val b = 20.prosent.sykdomsgrad.inntekt(800.daglig, `6G` = `6G`.beløp(1.januar), refusjonsbeløp = 800.daglig)
        val c = 60.prosent.sykdomsgrad.inntekt(2000.daglig, `6G` = `6G`.beløp(1.januar), refusjonsbeløp = 0.daglig)
        val betalte = listOf(a, b, c).betal().also {
            assertEquals(49.prosent, it.totalSykdomsgrad())
            // grense = 1059
        }
        betalte.forEach {
            assertTrue(it.er6GBegrenset())
        }
        assertUtbetaling(betalte[0], 300.0, 120.0)
        assertUtbetaling(betalte[1], 160.0, 0.0)
        assertUtbetaling(betalte[2], 0.0, 479.0)
    }

    @Test
    fun `tre arbeidsgivere over 6G, ingen personutbetaling`() {
        val a = 40.prosent.sykdomsgrad.inntekt(
            30000.månedlig,
            `6G` = `6G`.beløp(1.januar),
            refusjonsbeløp = 30000.månedlig * 75.prosent
        )
        val b =
            50.prosent.sykdomsgrad.inntekt(10000.månedlig, `6G` = `6G`.beløp(1.januar), refusjonsbeløp = 10000.månedlig)
        val c =
            70.prosent.sykdomsgrad.inntekt(15000.månedlig, `6G` = `6G`.beløp(1.januar), refusjonsbeløp = 15000.månedlig)
        val betalte = listOf(a, b, c).betal()
        betalte.forEach {
            assertTrue(it.er6GBegrenset())
        }

        assertUtbetaling(betalte[0], 396.0, 0.0)
        assertUtbetaling(betalte[1], 221.0, 0.0)
        assertUtbetaling(betalte[2], 463.0, 0.0)
    }

    @Test
    fun `tre arbeidsgivere med arbeidsgivere`() {
        val a = 50.prosent.sykdomsgrad.inntekt(4800.daglig, `6G` = `6G`.beløp(1.januar), refusjonsbeløp = 2400.daglig)
        val b = 20.prosent.sykdomsgrad.inntekt(3200.daglig, `6G` = `6G`.beløp(1.januar), refusjonsbeløp = 3200.daglig)
        val c = 60.prosent.sykdomsgrad.inntekt(8000.daglig, `6G` = `6G`.beløp(1.januar), refusjonsbeløp = 0.daglig)
        val betalte = listOf(a, b, c).betal()
        assertEquals(49.prosent, betalte.totalSykdomsgrad())
        assertTrue(betalte.er6GBegrenset())
        assertUtbetaling(
            betalte[0],
            691.0, 0.0
        )  // (1059 / (1200 + 640)) * 1200
        assertUtbetaling(betalte[1], 368.0, 0.0)
        assertUtbetaling(betalte[2], 0.0, 0.0)
    }

    @Test
    fun `eksempel fra regneark`() {
        val a =
            50.prosent.sykdomsgrad.inntekt(21000.månedlig, `6G` = `6G`.beløp(1.januar), refusjonsbeløp = 21000.månedlig)
        val b = 80.prosent.sykdomsgrad.inntekt(
            10000.månedlig,
            `6G` = `6G`.beløp(1.januar),
            refusjonsbeløp = 10000.månedlig * 90.prosent
        )
        val c = 20.prosent.sykdomsgrad.inntekt(
            31000.månedlig,
            `6G` = `6G`.beløp(1.januar),
            refusjonsbeløp = 31000.månedlig * 25.prosent
        )
        val betalte = listOf(a, b, c).betal()
        val forventet = ratio(247.0, 620.0)
        val faktisk = betalte.totalSykdomsgrad()
        assertEquals(forventet, faktisk)
        // grense = 864
        assertTrue(betalte.er6GBegrenset())
        assertUtbetaling(betalte[0], 470.0, 0.0)
        assertUtbetaling(betalte[1], 321.0, 0.0)
        assertUtbetaling(betalte[2], 70.0, 0.0)
    }

    @Test
    fun `eksempel fra regneark modifisert for utbetaling til arbeidstaker`() {
        val a =
            50.prosent.sykdomsgrad.inntekt(21000.månedlig, `6G` = `6G`.beløp(1.januar), refusjonsbeløp = 21000.månedlig)
        val b = 20.prosent.sykdomsgrad.inntekt(
            10000.månedlig,
            `6G` = `6G`.beløp(1.januar),
            refusjonsbeløp = 10000.månedlig * 20.prosent
        )
        val c = 20.prosent.sykdomsgrad.inntekt(
            31000.månedlig,
            `6G` = `6G`.beløp(1.januar),
            refusjonsbeløp = 31000.månedlig * 25.prosent
        )
        val betalte = listOf(a, b, c).betal().also {
            assertEquals(ratio(187.0, 620.0), it.totalSykdomsgrad())
            // grense = 864
        }
        betalte.forEach {
            assertTrue(it.er6GBegrenset())
        }
        assertUtbetaling(betalte[0], 485.0, 0.0)
        assertUtbetaling(betalte[1], 18.0, 20.0)
        assertUtbetaling(betalte[2], 72.0, 57.0)
    }

    @Test
    fun `Sykdomdsgrad rundes opp`() {
        val a = 20.prosent.sykdomsgrad.inntekt(
            10000.daglig,
            10000.daglig,
            `6G` = `6G`.beløp(1.januar),
            refusjonsbeløp = 10000.daglig
        )
        val b = 21.prosent.sykdomsgrad.inntekt(
            10000.daglig,
            10000.daglig,
            `6G` = `6G`.beløp(1.januar),
            refusjonsbeløp = 10000.daglig
        )
        val betalte = listOf(a, b).betal().also {
            assertEquals(20.5.prosent, it.totalSykdomsgrad()) //dekningsgrunnlag 454
        }
        assertUtbetaling(betalte[0], 216.0, 0.0) //454 * 2000 / 4100 ~+1
        assertUtbetaling(betalte[1], 227.0, 0.0)
    }

    @Test
    fun `Refusjonsbeløp graderes i henhold til sykdomsgrad`() {
        val økonomi = 50.prosent.sykdomsgrad.inntekt(
            1000.daglig,
            1000.daglig,
            `6G` = `6G`.beløp(1.januar),
            refusjonsbeløp = 500.daglig
        )
        val betalte = listOf(økonomi).betal()
        assertUtbetaling(betalte.single(), 250.0, 250.0)
    }

    @Test
    fun `fordeling mellom arbeidsgivere med lik inntekt og refusjon 1`() {
        val a = 100.prosent.sykdomsgrad.inntekt(
            15000.månedlig,
            `6G` = `6G`.beløp(1.januar),
            refusjonsbeløp = 15000.månedlig
        )
        val b = 100.prosent.sykdomsgrad.inntekt(
            15000.månedlig,
            `6G` = `6G`.beløp(1.januar),
            refusjonsbeløp = 15000.månedlig
        )
        val betalte = listOf(a, b).betal().also {
            assertEquals(100.prosent, it.totalSykdomsgrad())
        }
        betalte.forEach {
            assertFalse(it.er6GBegrenset())
        }
        assertForventetFeil(
            forklaring = "det utbetales 1 kr mindre per dag totalt sett",
            nå = {
                assertEquals(
                    1384,
                    betalte.mapNotNull { it.inspektør.arbeidsgiverbeløp }.summer()
                        .reflection { _, _, _, dagligInt -> dagligInt })
                assertUtbetaling(betalte[0], 692.0, 0.0)
                assertUtbetaling(betalte[1], 692.0, 0.0)
            },
            ønsket = {
                assertEquals(
                    1385,
                    betalte.mapNotNull { it.inspektør.arbeidsgiverbeløp }.summer()
                        .reflection { _, _, _, dagligInt -> dagligInt })
                assertUtbetaling(betalte[0], 693.0, 0.0)
                assertUtbetaling(betalte[1], 692.0, 0.0)
            }
        )
    }

    @Test
    fun `fordeling mellom arbeidsgivere med lik inntekt og refusjon 2`() {
        val a =
            100.prosent.sykdomsgrad.inntekt(7750.månedlig, `6G` = `6G`.beløp(1.januar), refusjonsbeløp = 7750.månedlig)
        val b =
            100.prosent.sykdomsgrad.inntekt(7750.månedlig, `6G` = `6G`.beløp(1.januar), refusjonsbeløp = 7750.månedlig)
        val betalte = listOf(a, b).betal().also {
            assertEquals(100.prosent, it.totalSykdomsgrad())
        }
        betalte.forEach {
            assertFalse(it.er6GBegrenset())
        }
        assertUtbetaling(betalte[0], 358.0, 0.0)
        assertUtbetaling(betalte[1], 357.0, 0.0)
    }

    @Test
    fun `fordeling mellom arbeidsgivere med lik refusjon, men ulik inntekt`() {
        val a =
            100.prosent.sykdomsgrad.inntekt(8000.månedlig, `6G` = `6G`.beløp(1.januar), refusjonsbeløp = 7750.månedlig)
        val b =
            100.prosent.sykdomsgrad.inntekt(7750.månedlig, `6G` = `6G`.beløp(1.januar), refusjonsbeløp = 7750.månedlig)
        val betalte = listOf(a, b).betal().also {
            assertEquals(100.prosent, it.totalSykdomsgrad())
        }
        betalte.forEach {
            assertFalse(it.er6GBegrenset())
        }
        assertForventetFeil(
            nå = {
                assertUtbetaling(betalte[0], 358.0, 11.0)
                assertUtbetaling(betalte[1], 358.0, 0.0)
            },
            ønsket = {
                assertUtbetaling(betalte[0], 358.0, 12.0)
                assertUtbetaling(betalte[1], 357.0, 0.0)
            }
        )
    }

    @Test
    fun `tilkommen inntekt - total grad bestemmes ut fra sykepengegrunnlaget`() {
        val `6G` = `6G`.beløp(1.januar)
        val a = 80.prosent.sykdomsgrad.inntekt(50000.månedlig, beregningsgrunnlag = 50000.månedlig, `6G` = `6G`)
        val b = 40.prosent.sykdomsgrad.inntekt(
            30000.månedlig,
            beregningsgrunnlag = INGEN,
            refusjonsbeløp = INGEN,
            `6G` = `6G`
        )
        val betalte = listOf(a, b).betal().also {
            assertEquals(44.prosent, it.totalSykdomsgrad())
        }
        assertUtbetaling(betalte[0], 951.0, 0.0)
        assertUtbetaling(betalte[1], 0.0, 0.0)
    }

    @Test
    fun `Tilkommen inntekt - fordeler personbeløp ved avsluttet arbeidsforhold`() {
        val `6G` = `6G`.beløp(1.januar)
        val a = 0.prosent.sykdomsgrad.inntekt(INGEN, beregningsgrunnlag = 31000.månedlig, `6G`= `6G`)
        val b = 100.prosent.sykdomsgrad.inntekt(10000.månedlig, beregningsgrunnlag = INGEN, `6G` = `6G`)
        val betalte = listOf(a, b).betal()
        assertUtbetaling(betalte[0], 0.0, 0.0)
        assertUtbetaling(betalte[1], 462.0, 969.0)
    }

    @Test
    fun `fordeling mellom arbeidsgivere mde ulik inntekt og refusjon`() {
        val a = 100.prosent.sykdomsgrad.inntekt(
            30000.månedlig,
            `6G` = `6G`.beløp(1.januar),
            refusjonsbeløp = 30000.månedlig
        )
        val b = 100.prosent.sykdomsgrad.inntekt(
            35000.månedlig,
            `6G` = `6G`.beløp(1.januar),
            refusjonsbeløp = 35000.månedlig
        )
        val betalte = listOf(a, b).betal().also {
            assertEquals(100.prosent, it.totalSykdomsgrad())
        }
        betalte.forEach {
            assertTrue(it.er6GBegrenset())
        }
        assertEquals(
            2161,
            betalte.mapNotNull { it.inspektør.arbeidsgiverbeløp }.summer()
                .reflection { _, _, _, dagligInt -> dagligInt })
        assertForventetFeil(
            forklaring = "arbeidsgiver 2 har høyere avrundingsdifferanse enn arbeidsgiver 1, og bør få den 1 kr diffen." +
                    "sykepengegrunnlaget er 561 860 kr (6G). Arbeidsgiver 1 sin andel utgjør (30,000 / (30,000 + 35,000 kr)) * 561 860 kr = 997,38 kr daglig." +
                    "Arbeidsgiver 2 sin andel utgjør  (35,000 / (30,000 + 35,000 kr)) * 561 860 kr = 1163,62 kr daglig, og har med dette en større avrundingsdifferanse.",
            nå = {
                assertUtbetaling(betalte[0], 998.0, 0.0)
                assertUtbetaling(betalte[1], 1163.0, 0.0)
            },
            ønsket = {
                assertUtbetaling(betalte[0], 997.0, 0.0)
                assertUtbetaling(betalte[1], 1164.0, 0.0)
            }
        )
    }

    private fun assertUtbetaling(økonomi: Økonomi, expectedArbeidsgiver: Double, expectedPerson: Double) {
        assertEquals(expectedArbeidsgiver.daglig, økonomi.inspektør.arbeidsgiverbeløp, "arbeidsgiverbeløp problem")
        assertEquals(expectedPerson.daglig, økonomi.inspektør.personbeløp, "personbeløp problem")
    }

    private val Prosentdel.sykdomsgrad get() = Økonomi.sykdomsgrad(this)

    private fun Økonomi.inntekt(
        aktuellDagsinntekt: Inntekt,
        dekningsgrunnlag: Inntekt = aktuellDagsinntekt,
        `6G`: Inntekt,
        beregningsgrunnlag: Inntekt = aktuellDagsinntekt
    ) =
        inntekt(
            aktuellDagsinntekt,
            dekningsgrunnlag,
            beregningsgrunnlag = beregningsgrunnlag,
            `6G` = `6G`,
            refusjonsbeløp = aktuellDagsinntekt
        )
}

private val Int.januar get() = LocalDate.of(2018, 1, this)

private const val ØnsketOppførsel =
    "✅ Koden oppfører seg nå som ønsket! Fjern bruken av 'assertForventetFeil', og behold kun assertions for ønsket oppførsel ✅"
private const val FeilITestkode = "☠️️ Feil i testkoden, feiler ikke på assertions ☠️️"

private fun Throwable.håndterNåOppførselFeil(harØnsketOppførsel: Boolean) {
    if (harØnsketOppførsel) throw AssertionError(ØnsketOppførsel)
    if (this is AssertionError) throw AssertionError(
        "⚠️ Koden har endret nå-oppførsel, men ikke til ønsket oppførsel ⚠️️️",
        this
    )
    throw AssertionError(FeilITestkode, this)
}

private fun Throwable.håndterØnsketOppførselFeil(forklaring: String?) = when (this) {
    is AssertionError -> println("☹️ Det er kjent at vi ikke har ønsket oppførsel for ${forklaring ?: "denne testen"} ☹️️")
    else -> throw AssertionError(FeilITestkode, this)
}

private fun assertForventetFeil(forklaring: String? = null, nå: () -> Unit, ønsket: () -> Unit) {
    runCatching(nå).exceptionOrNull()?.håndterNåOppførselFeil(harØnsketOppførsel = runCatching(ønsket).isSuccess)
    assertThrows<Throwable>(ØnsketOppførsel) { ønsket() }.håndterØnsketOppførselFeil(forklaring)
}