package no.nav.helse.økonomi

import no.nav.helse.inspectors.inspektør
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.summer
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Prosentdel.Companion.ratio
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
    fun `akkurat under 20-prosent-grensen`() {
        val økonomi = listOf(
            8.prosent.inntekt(30000.månedlig),
            54.prosent.inntekt(10312.40.månedlig)
        )
        val totalSykdomsgrad = økonomi.totalSykdomsgrad()
        assertNotEquals(20.prosent, totalSykdomsgrad)
        assertTrue(totalSykdomsgrad.erUnderGrensen())
    }

    @Test
    fun `total sykdomsgrad regnes ut fra aktuell dagsinntekt`() {
        val inntekt = 10000.månedlig
        val økonomi = listOf(
            100.prosent.inntekt(inntekt, dekningsgrunnlag = inntekt),
            50.prosent.inntekt(inntekt, dekningsgrunnlag = INGEN)
        )
        assertEquals(75.prosent, økonomi.totalSykdomsgrad())
    }

    @Test
    fun `to arbeidsgivere gikk inn i en bar og ba om 20 prosent hver`() {
        val inntektA1 = 18199.7.månedlig
        val inntektA2 = 22966.54.månedlig
        val økonomi = listOf(
            20.prosent.inntekt(inntektA1),
            20.prosent.inntekt(inntektA2)
        )
        assertEquals(20.prosent, økonomi.totalSykdomsgrad())
        assertFalse(økonomi.totalSykdomsgrad().erUnderGrensen())

    }

    @Test
    fun singelsykegrad() {
        assertEquals(
            75.prosent,
            listOf(
                75.prosent.inntekt(1200.daglig, 1200.daglig)
            ).totalSykdomsgrad()
        )
    }

    @Test
    fun singelsykegradUtenInntekt() {
        assertEquals(
            75.prosent,
            listOf(
                75.prosent.inntekt(0.daglig, 0.daglig)
            ).totalSykdomsgrad()
        )
    }

    @Test
    fun `to arbeidsgivere`() {
        assertEquals(
            38.prosent,
            listOf(
                50.prosent.inntekt(1200.daglig, 1200.daglig),
                20.prosent.inntekt(800.daglig, 800.daglig)
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
                50.prosent.inntekt(0.daglig, 0.daglig),
                20.prosent.inntekt(0.daglig, 0.daglig)
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
                50.prosent.inntekt(1200.daglig, 1200.daglig),
                20.prosent.inntekt(800.daglig, 800.daglig),
                60.prosent.inntekt(2000.daglig, 2000.daglig)
            ).totalSykdomsgrad()
        )
    }

    @Test
    fun `låst økonomi fungerer ikke som arbeidsdag ved total sykdomsgrad`() {
        assertEquals(
            49.prosent,
            listOf(
                50.prosent.inntekt(1200.daglig, 1200.daglig),
                20.prosent.inntekt(800.daglig, 800.daglig),
                60.prosent.inntekt(2000.daglig, 2000.daglig).ikkeBetalt()
            ).totalSykdomsgrad().also {
                assertFalse(it.erUnderGrensen())
            }
        )
    }

    @Test
    fun `arbeidsgiverrefusjon med inntekt`() {
        100.prosent.inntekt(1000.daglig, refusjonsbeløp = 1000.daglig)
            .also { økonomi ->
                assertEquals(1000.daglig, økonomi.inspektør.arbeidsgiverRefusjonsbeløp)
            }
    }

    @Test
    fun `dekningsgrunnlag returns clone`() {
        50.prosent.also { original ->
            assertNotSame(original, original.inntekt(1200.daglig, 1200.daglig))
        }
    }

    @Test
    fun `toMap med dekningsgrunnlag`() {
        val økonomi = 79.5.prosent.inntekt(1200.4.daglig, 1200.4.daglig)
        assertEquals(79.5, økonomi.inspektør.grad.toDouble())
        assertEquals(1200.4.daglig, økonomi.inspektør.dekningsgrunnlag)
    }

    @Test
    fun `toIntMap med dekningsgrunnlag`() {
        79.5.prosent.inntekt(1200.4.daglig, 1200.4.daglig)
            .also { økonomi ->
                assertEquals(79.5, økonomi.inspektør.grad.toDouble())
                assertEquals(1200.4.daglig, økonomi.inspektør.dekningsgrunnlag)
            }
    }

    @Test
    fun `kan beregne betaling mer enn en gang`() {
        assertDoesNotThrow {
            listOf(80.prosent.inntekt(1200.daglig, 1200.daglig))
                .betal(1200.daglig)
                .betal(1200.daglig)
        }
    }

    @Test
    fun `Beregn utbetaling når mindre enn 6G`() {
        80.prosent.inntekt(
            1200.daglig,
            1200.daglig,
            refusjonsbeløp = 1200.daglig
        ).also {
            val betalte = listOf(it).betal(1200.daglig)
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
        100.prosent
            .inntekt(999.daglig, refusjonsbeløp = 499.5.daglig)
            .also {
                val betalte = listOf(it).betal(999.daglig)
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
        val a = 50.prosent.inntekt(600.daglig, refusjonsbeløp = 300.daglig)
        val b = 20.prosent.inntekt(400.daglig, refusjonsbeløp = 400.daglig)
        val c = 60.prosent.inntekt(1000.daglig, refusjonsbeløp = 0.daglig)
        val betalte = listOf(a, b, c).betal(2000.daglig).also {
            assertEquals(49.prosent, it.totalSykdomsgrad())
        }
        assertUtbetaling(betalte[0], 150.0, 150.0)
        assertUtbetaling(betalte[1], 80.0, 0.0)
        assertUtbetaling(betalte[2], 0.0, 600.0)
    }

    @Test
    fun `tre arbeidsgivere med persongrense`() {
        val a = 50.prosent.inntekt(1200.daglig, refusjonsbeløp = 1200.daglig * 50.prosent)
        val b = 20.prosent.inntekt(800.daglig, refusjonsbeløp = 800.daglig)
        val c = 60.prosent.inntekt(2000.daglig, refusjonsbeløp = 0.daglig)
        val betalte = listOf(a, b, c).betal(561804.årlig).also {
            assertEquals(49.prosent, it.totalSykdomsgrad())
            // grense = 1059
        }
        assertUtbetaling(betalte[0], 300.0, 120.0)
        assertUtbetaling(betalte[1], 160.0, 0.0)
        assertUtbetaling(betalte[2], 0.0, 479.0)
    }

    @Test
    fun `tre arbeidsgivere over 6G, ingen personutbetaling`() {
        val a = 40.prosent.inntekt(
            30000.månedlig,
            refusjonsbeløp = 30000.månedlig * 75.prosent
        )
        val b = 50.prosent.inntekt(10000.månedlig, refusjonsbeløp = 10000.månedlig)
        val c = 70.prosent.inntekt(15000.månedlig, refusjonsbeløp = 15000.månedlig)
        val betalte = listOf(a, b, c).betal(561804.årlig)
        assertUtbetaling(betalte[0], 396.0, 0.0)
        assertUtbetaling(betalte[1], 221.0, 0.0)
        assertUtbetaling(betalte[2], 463.0, 0.0)
    }

    @Test
    fun `tre arbeidsgivere med arbeidsgivere`() {
        val a = 50.prosent.inntekt(4800.daglig, refusjonsbeløp = 2400.daglig)
        val b = 20.prosent.inntekt(3200.daglig, refusjonsbeløp = 3200.daglig)
        val c = 60.prosent.inntekt(8000.daglig, refusjonsbeløp = 0.daglig)
        val betalte = listOf(a, b, c).betal(561804.årlig)
        assertEquals(49.prosent, betalte.totalSykdomsgrad())
        assertUtbetaling(
            betalte[0],
            691.0, 0.0
        )  // (1059 / (1200 + 640)) * 1200
        assertUtbetaling(betalte[1], 368.0, 0.0)
        assertUtbetaling(betalte[2], 0.0, 0.0)
    }

    @Test
    fun `eksempel fra regneark`() {
        val a = 50.prosent.inntekt(21000.månedlig, refusjonsbeløp = 21000.månedlig)
        val b = 80.prosent.inntekt(10000.månedlig, refusjonsbeløp = 10000.månedlig * 90.prosent)
        val c = 20.prosent.inntekt(31000.månedlig, refusjonsbeløp = 31000.månedlig * 25.prosent)
        val betalte = listOf(a, b, c).betal(561804.årlig)
        val forventet = ratio(247.0, 620.0)
        val faktisk = betalte.totalSykdomsgrad()
        assertEquals(forventet, faktisk)
        // grense = 864
        assertUtbetaling(betalte[0], 470.0, 0.0)
        assertUtbetaling(betalte[1], 321.0, 0.0)
        assertUtbetaling(betalte[2], 70.0, 0.0)
    }

    @Test
    fun `eksempel fra regneark modifisert for utbetaling til arbeidstaker`() {
        val a = 50.prosent.inntekt(21000.månedlig, refusjonsbeløp = 21000.månedlig)
        val b = 20.prosent.inntekt(10000.månedlig, refusjonsbeløp = 10000.månedlig * 20.prosent)
        val c = 20.prosent.inntekt(31000.månedlig, refusjonsbeløp = 31000.månedlig * 25.prosent)
        val betalte = listOf(a, b, c).betal(561804.årlig).also {
            assertEquals(ratio(187.0, 620.0), it.totalSykdomsgrad())
            // grense = 864
        }
        assertUtbetaling(betalte[0], 485.0, 0.0)
        assertUtbetaling(betalte[1], 18.0, 20.0)
        assertUtbetaling(betalte[2], 72.0, 57.0)
    }

    @Test
    fun `Sykdomdsgrad rundes opp`() {
        val a = 20.prosent.inntekt(10000.daglig, 10000.daglig, refusjonsbeløp = 10000.daglig)
        val b = 21.prosent.inntekt(10000.daglig, 10000.daglig, refusjonsbeløp = 10000.daglig)
        val betalte = listOf(a, b).betal(561804.årlig).also {
            assertEquals(20.5.prosent, it.totalSykdomsgrad()) //dekningsgrunnlag 454
        }
        assertUtbetaling(betalte[0], 216.0, 0.0) //454 * 2000 / 4100 ~+1
        assertUtbetaling(betalte[1], 227.0, 0.0)
    }

    @Test
    fun `Refusjonsbeløp graderes i henhold til sykdomsgrad`() {
        val økonomi = 50.prosent.inntekt(
            1000.daglig,
            1000.daglig,
            refusjonsbeløp = 500.daglig
        )
        val betalte = listOf(økonomi).betal(1000.daglig)
        assertUtbetaling(betalte.single(), 250.0, 250.0)
    }

    @Test
    fun `fordeling mellom arbeidsgivere med lik inntekt og refusjon 1`() {
        val a = 100.prosent.inntekt(15000.månedlig, refusjonsbeløp = 15000.månedlig)
        val b = 100.prosent.inntekt(15000.månedlig, refusjonsbeløp = 15000.månedlig)
        val betalte = listOf(a, b).betal(30000.månedlig).also {
            assertEquals(100.prosent, it.totalSykdomsgrad())
        }
        assertForventetFeil(
            forklaring = "det utbetales 1 kr mindre per dag totalt sett",
            nå = {
                assertEquals(
                    1384,
                    betalte.mapNotNull { it.inspektør.arbeidsgiverbeløp }.summer()
                        .dagligInt
                )
                assertUtbetaling(betalte[0], 692.0, 0.0)
                assertUtbetaling(betalte[1], 692.0, 0.0)
            },
            ønsket = {
                assertEquals(
                    1385,
                    betalte.mapNotNull { it.inspektør.arbeidsgiverbeløp }.summer()
                        .dagligInt
                )
                assertUtbetaling(betalte[0], 693.0, 0.0)
                assertUtbetaling(betalte[1], 692.0, 0.0)
            }
        )
    }

    @Test
    fun `fordeling mellom arbeidsgivere med lik inntekt og refusjon 2`() {
        val a =
            100.prosent.inntekt(7750.månedlig, refusjonsbeløp = 7750.månedlig)
        val b =
            100.prosent.inntekt(7750.månedlig, refusjonsbeløp = 7750.månedlig)
        val betalte = listOf(a, b).betal(15500.månedlig).also {
            assertEquals(100.prosent, it.totalSykdomsgrad())
        }
        assertUtbetaling(betalte[0], 358.0, 0.0)
        assertUtbetaling(betalte[1], 357.0, 0.0)
    }

    @Test
    fun `fordeling mellom arbeidsgivere med lik refusjon, men ulik inntekt`() {
        val a = 100.prosent.inntekt(8000.månedlig, refusjonsbeløp = 7750.månedlig)
        val b = 100.prosent.inntekt(7750.månedlig, refusjonsbeløp = 7750.månedlig)
        val betalte = listOf(a, b).betal(15750.månedlig).also {
            assertEquals(100.prosent, it.totalSykdomsgrad())
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
        val a = 80.prosent.inntekt(50000.månedlig)
        val b = 100.prosent.inntekt(30000.månedlig, refusjonsbeløp = INGEN)
        val betalte = listOf(a, b).betal(561804.årlig).also {
            assertEquals(87, it.totalSykdomsgrad().toDouble().toInt())
        }
        assertUtbetaling(betalte[0], 1846.0, 0.0)
        assertUtbetaling(betalte[1], 0.0, 45.0)
    }

    @Test
    fun `tilkommen inntekt - mer i tilkommen enn 6G`() {
        val a = 100.prosent.inntekt(31000.månedlig)
        val b = 0.prosent.inntekt(50000.månedlig, refusjonsbeløp = INGEN)
        val betalte = listOf(a, b).betal(31000.månedlig).also {
            assertEquals(38, it.totalSykdomsgrad().toDouble().toInt())
        }
        assertUtbetaling(betalte[0], 548.0, 0.0)
        assertUtbetaling(betalte[1], 0.0, 0.0)
    }

    @Test
    fun `Tilkommen inntekt - fordeler ikke personbeløp ved avsluttet arbeidsforhold`() {
        val a = 0.prosent.inntekt(INGEN)
        val b = 100.prosent.inntekt(10000.månedlig)
        val betalte = listOf(a, b).betal(31000.månedlig)
        assertUtbetaling(betalte[0], 0.0, 0.0)
        assertUtbetaling(betalte[1], 462.0, 969.0)
    }

    @Test
    fun `fordeling mellom arbeidsgivere mde ulik inntekt og refusjon`() {
        val a = 100.prosent.inntekt(30000.månedlig, refusjonsbeløp = 30000.månedlig)
        val b = 100.prosent.inntekt(35000.månedlig, refusjonsbeløp = 35000.månedlig)
        val betalte = listOf(a, b).betal(561804.årlig).also {
            assertEquals(100.prosent, it.totalSykdomsgrad())
        }
        assertEquals(2161, betalte.mapNotNull { it.inspektør.arbeidsgiverbeløp }.summer().dagligInt)
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

    private fun List<Økonomi>.totalSykdomsgrad(): Prosentdel {
        return totalSykdomsgrad(this).first().totalSykdomsgrad
    }

    private fun assertUtbetaling(økonomi: Økonomi, expectedArbeidsgiver: Double, expectedPerson: Double) {
        assertEquals(expectedArbeidsgiver.daglig, økonomi.inspektør.arbeidsgiverbeløp, "arbeidsgiverbeløp problem")
        assertEquals(expectedPerson.daglig, økonomi.inspektør.personbeløp, "personbeløp problem")
    }

    private fun Prosentdel.inntekt(
        aktuellDagsinntekt: Inntekt,
        dekningsgrunnlag: Inntekt = aktuellDagsinntekt,
        refusjonsbeløp: Inntekt = aktuellDagsinntekt
    ) =
        Økonomi.inntekt(this, aktuellDagsinntekt, dekningsgrunnlag, refusjonsbeløp)
}

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
