package no.nav.helse.utbetalingstidslinje

import java.math.MathContext
import no.nav.helse.desember
import no.nav.helse.etterlevelse.Tidslinjedag.Companion.dager
import no.nav.helse.etterlevelse.UtbetalingstidslinjeBuilder.Companion.subsumsjonsformat
import no.nav.helse.februar
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.testhelpers.AP
import no.nav.helse.testhelpers.ARB
import no.nav.helse.testhelpers.FRI
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.AvvistDag
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import no.nav.helse.økonomi.inspectors.inspektør

internal class UtbetalingstidslinjeTest {

    @Test
    fun subsetting() {
        assertEquals(1.januar til 5.januar, tidslinjeOf(5.NAV).subset(1.januar til 5.januar).periode())
        assertEquals(4.januar.somPeriode(), tidslinjeOf(5.NAV).subset(4.januar.somPeriode()).periode())
        assertEquals(1.januar til 5.januar, tidslinjeOf(5.NAV).subset(31.desember(2017) til 6.januar).periode())
        assertTrue(tidslinjeOf(5.NAV).subset(6.januar til 6.januar).isEmpty())
    }

    @Test
    fun fraOgMed() {
        assertEquals(3.januar til 5.januar, tidslinjeOf(5.NAV).fraOgMed(3.januar).periode())
        assertEquals(0, tidslinjeOf(5.NAV).fraOgMed(6.januar).size)
        assertEquals(1.januar til 5.januar, tidslinjeOf(5.NAV).fraOgMed(31.desember(2017)).periode())
    }

    @Test
    fun fremTilOgMed() {
        assertEquals(1.januar til 2.januar, tidslinjeOf(5.NAV).fremTilOgMed(2.januar).periode())
        assertEquals(1.januar til 5.januar, tidslinjeOf(5.NAV).fremTilOgMed(6.januar).periode())
        assertEquals(0, tidslinjeOf(5.NAV).fremTilOgMed(31.desember(2017)).size)
    }

    @Test
    fun plus() {
        assertEquals(0, (tidslinjeOf() + tidslinjeOf()).size)
        assertEquals(1.januar til 5.januar, (tidslinjeOf() + tidslinjeOf(5.NAV)).periode())
        assertEquals(1.januar til 10.januar, (tidslinjeOf(3.NAV) + tidslinjeOf(5.NAV, startDato = 6.januar)).periode())
    }

    @Test
    fun negativEndringIBeløp() {
        val inntekt = 1000.daglig
        val fullRefusjon = Utbetalingstidslinje.betale(inntekt, listOf(tidslinjeOf(5.NAV(dekningsgrunnlag = inntekt, refusjonsbeløp = inntekt)))).single()
        val merRefusjon = Utbetalingstidslinje.betale(inntekt * 2, listOf(tidslinjeOf(5.NAV(dekningsgrunnlag = inntekt * 2, refusjonsbeløp = inntekt * 2)))).single()
        val delvisRefusjon = Utbetalingstidslinje.betale(inntekt, listOf(tidslinjeOf(5.NAV(dekningsgrunnlag = inntekt, refusjonsbeløp = inntekt / 2)))).single()
        val ingenRefusjon = Utbetalingstidslinje.betale(inntekt, listOf(tidslinjeOf(5.NAV(dekningsgrunnlag = inntekt, refusjonsbeløp = Inntekt.INGEN)))).single()

        val ferie = Utbetalingstidslinje.betale(inntekt, listOf(tidslinjeOf(5.FRI))).single()

        assertFalse(merRefusjon.negativEndringIBeløp(fullRefusjon))
        assertTrue(fullRefusjon.negativEndringIBeløp(merRefusjon))
        assertFalse(fullRefusjon.negativEndringIBeløp(fullRefusjon))
        assertTrue(fullRefusjon.negativEndringIBeløp(delvisRefusjon))
        assertTrue(fullRefusjon.negativEndringIBeløp(ingenRefusjon))
        assertFalse(fullRefusjon.negativEndringIBeløp(ferie))
        assertFalse(delvisRefusjon.negativEndringIBeløp(ferie))
        assertFalse(ingenRefusjon.negativEndringIBeløp(ferie))
        assertTrue(ferie.negativEndringIBeløp(fullRefusjon))
        assertTrue(ferie.negativEndringIBeløp(delvisRefusjon))
        assertTrue(ferie.negativEndringIBeløp(ingenRefusjon))
    }

    @Test
    fun `negativ endring i personbeløp`() {
        val inntekt = 1000.daglig
        val fullRefusjon = Utbetalingstidslinje.betale(inntekt, listOf(tidslinjeOf(5.NAV(dekningsgrunnlag = inntekt, refusjonsbeløp = inntekt)))).single()
        val øktInntektMedRefusjonOgPerson = Utbetalingstidslinje.betale(inntekt * 2, listOf(tidslinjeOf(5.NAV(dekningsgrunnlag = inntekt * 2, refusjonsbeløp = inntekt)))).single()
        val øktInntektMedPerson = Utbetalingstidslinje.betale(inntekt * 2, listOf(tidslinjeOf(5.NAV(dekningsgrunnlag = inntekt * 2, refusjonsbeløp = Inntekt.INGEN)))).single()
        val ingenRefusjon = Utbetalingstidslinje.betale(inntekt, listOf(tidslinjeOf(5.NAV(dekningsgrunnlag = inntekt, refusjonsbeløp = Inntekt.INGEN)))).single()

        assertTrue(fullRefusjon.negativEndringIBeløp(ingenRefusjon)) {
            "mindre personbeløp vil starte en tilbakekreving fra bruker"
        }
        assertFalse(øktInntektMedRefusjonOgPerson.negativEndringIBeløp(ingenRefusjon)) {
            "inntekt økes slik at bruker fremdeles får utbetalt samme så er det ikke en negativ endring for personen"
        }
        assertFalse(øktInntektMedPerson.negativEndringIBeløp(øktInntektMedRefusjonOgPerson)) {
            "totalbeløp er det samme selv om arbeidsgiverrefusjonen har byttet hender til personen"
        }
    }

    @Test
    fun betale() {
        val `6G`= 2161.daglig
        val input = listOf(tidslinjeOf(1.NAV(1081)), tidslinjeOf(1.NAV(1081)))
        val result = Utbetalingstidslinje.betale(`6G`, input)

        input.forEachIndexed { index, input ->
            assertNull(input[1.januar].økonomi.inspektør.arbeidsgiverbeløp) { "den uberegnede listen skal ikke modifiseres" }
            assertEquals(1081, result[index][1.januar].økonomi.inspektør.arbeidsgiverbeløp?.dagligInt)
        }
    }

    @Test
    fun `avviser perioder med flere begrunnelser`() {
        val periode = 1.januar til 5.januar
        tidslinjeOf(5.NAV).also {
            val første = it.avvis(listOf(periode), Begrunnelse.MinimumSykdomsgrad)
            val andre = første.avvis(listOf(periode), Begrunnelse.EtterDødsdato)
            val tredje = andre.avvis(listOf(periode), Begrunnelse.ManglerMedlemskap)
            periode.forEach { dato ->
                val dag = tredje[dato] as AvvistDag
                assertEquals(3, dag.begrunnelser.size)
            }
        }
    }

    @Test
    fun `samlet periode`() {
        assertEquals(1.januar til 1.januar, Utbetalingstidslinje.periode(listOf(tidslinjeOf(1.NAV))))
        assertEquals(
            1.desember(2017) til 7.mars, Utbetalingstidslinje.periode(
            listOf(
                tidslinjeOf(7.NAV),
                tidslinjeOf(7.NAV, startDato = 1.mars),
                tidslinjeOf(7.NAV, startDato = 1.desember(2017)),
            )
        )
        )
    }

    @Test
    fun `total sykdomsgrad`() {
        val ag1 = tidslinjeOf(5.NAV(dekningsgrunnlag = 1000, grad = 50))
        val ag2 = tidslinjeOf(1.FRI(dekningsgrunnlag = 2000), 4.NAV(dekningsgrunnlag = 2000, grad = 100))
        val tidslinjer = listOf(ag1, ag2)
        val result = Utbetalingstidslinje.totalSykdomsgrad(tidslinjer)

        // totalgrad = 16.6666666666...
        1.januar.also { dato ->
            val mc = MathContext(15)
            val expected = (100 / 6.0).toBigDecimal(mc)
            result.forEach {
                val actual = it[dato].økonomi.inspektør.totalGrad.toBigDecimal(mc)
                assertEquals(expected.toInt(), actual.toInt())
            }
        }

        // totalgrad = 83.333333
        2.januar.also { dato ->
            val mc = MathContext(15)
            val expected = (250 / 3.0).toBigDecimal(mc)
            result.forEach {
                val actual = it[dato].økonomi.inspektør.totalGrad.toBigDecimal(mc)
                assertEquals(expected.toInt(), actual.toInt())
            }
        }
    }

    @Test
    fun `total sykdomsgrad med ukjent dag`() {
        val ag1 = tidslinjeOf(1.NAV(dekningsgrunnlag = 1000, grad = 50))
        val ag2 = tidslinjeOf()
        val tidslinjer = listOf(ag1, ag2)
        val result = Utbetalingstidslinje.totalSykdomsgrad(tidslinjer)

        1.januar.also { dato ->
            val expected = 50
            val actual = result[0][dato].økonomi.inspektør.totalGrad
            assertEquals(expected, actual)
        }
    }


    @Test
    fun `tar med fridager på slutten av en sykdomsperiode`() {
        val utbetalingstidslinje = tidslinjeOf(16.AP, 15.NAV, 2.FRI)
        val tidslinjedager = utbetalingstidslinje.subsumsjonsformat().dager()

        assertEquals(
            listOf(
                mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                mapOf("fom" to 17.januar, "tom" to 31.januar, "dagtype" to "NAVDAG", "grad" to 100),
                mapOf("fom" to 1.februar, "tom" to 2.februar, "dagtype" to "FRIDAG", "grad" to 0)
            ),
            tidslinjedager
        )
    }

    @Test
    fun `tar ikke med fridager i oppholdsperiode`() {
        val utbetalingstidslinje = tidslinjeOf(16.AP, 15.NAV, 10.ARB, 2.FRI)
        val tidslinjedager = utbetalingstidslinje.subsumsjonsformat().dager()

        assertEquals(
            listOf(
                mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                mapOf("fom" to 17.januar, "tom" to 31.januar, "dagtype" to "NAVDAG", "grad" to 100)
            ),
            tidslinjedager
        )
    }

    @Disabled
    @Test
    fun `dette burde ikke committes, tror jeg`() {
        println(tidslinjeOf(16.AP, 15.NAV, 10.ARB, 2.FRI).toFancyString())
        println(tidslinjeOf(16.AP, 10.ARB, 15.NAV, 2.FRI).toFancyString())
    }
}
