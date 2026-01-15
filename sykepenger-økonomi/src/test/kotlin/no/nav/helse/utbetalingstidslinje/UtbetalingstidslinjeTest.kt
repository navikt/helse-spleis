package no.nav.helse.utbetalingstidslinje

import no.nav.helse.desember
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.testhelpers.AP
import no.nav.helse.testhelpers.FRI
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.inspectors.inspektør
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.MathContext
import java.time.LocalDate
import no.nav.helse.testhelpers.NAP
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals

internal class UtbetalingstidslinjeTest {

    @Test
    fun subsetting() {
        assertEquals(1.januar til 5.januar, tidslinjeOf(5.NAV).subset(1.januar til 5.januar).periode())
        assertEquals(4.januar.somPeriode(), tidslinjeOf(5.NAV).subset(4.januar.somPeriode()).periode())
        assertEquals(
            1.januar til 5.januar,
            tidslinjeOf(5.NAV).subset(31.desember(2017) til 6.januar).periode()
        )
        Assertions.assertTrue(tidslinjeOf(5.NAV).subset(6.januar til 6.januar).isEmpty())
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
        assertEquals(
            1.januar til 10.januar,
            (tidslinjeOf(3.NAV) + tidslinjeOf(5.NAV, startDato = 6.januar)).periode()
        )
    }

    @Test
    fun `totalbeløp summerer opp dager som kan utbetales`() {
        val inntekt = 1000.daglig
        val fullRefusjon = Utbetalingstidslinje.betale(inntekt, listOf(
            tidslinjeOf(
                10.AP(dekningsgrunnlag = inntekt.dagligInt),
                6.NAP(dekningsgrunnlag = inntekt.dagligInt),
                5.NAV(dekningsgrunnlag = inntekt.dagligInt, refusjonsbeløp = inntekt.dagligInt),
                1.FRI(dekningsgrunnlag = inntekt.dagligInt),
                4.NAV(dekningsgrunnlag = inntekt.dagligInt, refusjonsbeløp = 0)
            )
        )).single()
        assertEquals(inntekt * 3, fullRefusjon.totalbeløpRefusjon)
        assertEquals(inntekt * 8, fullRefusjon.totalbeløpPerson)
    }

    @Test
    fun negativEndringIBeløp() {
        val inntekt = 1000.daglig
        val fullRefusjon = Utbetalingstidslinje.betale(inntekt, listOf(
            tidslinjeOf(
                5.NAV(
                    dekningsgrunnlag = inntekt.dagligInt,
                    refusjonsbeløp = inntekt.dagligInt
                )
            )
        )).single()
        val merRefusjon = Utbetalingstidslinje.betale(inntekt * 2, listOf(
            tidslinjeOf(
                5.NAV(
                    dekningsgrunnlag = (inntekt * 2).dagligInt,
                    refusjonsbeløp = (inntekt * 2).dagligInt
                )
            )
        )).single()
        val delvisRefusjon = Utbetalingstidslinje.betale(inntekt, listOf(
            tidslinjeOf(
                5.NAV(
                    dekningsgrunnlag = inntekt.dagligInt,
                    refusjonsbeløp = (inntekt / 2).dagligInt
                )
            )
        )).single()
        val ingenRefusjon = Utbetalingstidslinje.betale(inntekt, listOf(
            tidslinjeOf(
                5.NAV(
                    dekningsgrunnlag = inntekt.dagligInt,
                    refusjonsbeløp = 0
                )
            )
        )).single()

        val ferie = Utbetalingstidslinje.betale(inntekt, listOf(tidslinjeOf(5.FRI))).single()

        Assertions.assertFalse(merRefusjon.negativEndringIBeløp(fullRefusjon))
        Assertions.assertTrue(fullRefusjon.negativEndringIBeløp(merRefusjon))
        Assertions.assertFalse(fullRefusjon.negativEndringIBeløp(fullRefusjon))
        Assertions.assertTrue(fullRefusjon.negativEndringIBeløp(delvisRefusjon))
        Assertions.assertTrue(fullRefusjon.negativEndringIBeløp(ingenRefusjon))
        Assertions.assertFalse(fullRefusjon.negativEndringIBeløp(ferie))
        Assertions.assertFalse(delvisRefusjon.negativEndringIBeløp(ferie))
        Assertions.assertFalse(ingenRefusjon.negativEndringIBeløp(ferie))
        Assertions.assertTrue(ferie.negativEndringIBeløp(fullRefusjon))
        Assertions.assertTrue(ferie.negativEndringIBeløp(delvisRefusjon))
        Assertions.assertTrue(ferie.negativEndringIBeløp(ingenRefusjon))
    }

    @Test
    fun `negativ endring i personbeløp`() {
        val inntekt = 1000.daglig
        val fullRefusjon = Utbetalingstidslinje.betale(inntekt, listOf(
            tidslinjeOf(
                5.NAV(
                    dekningsgrunnlag = inntekt.dagligInt,
                    refusjonsbeløp = inntekt.dagligInt
                )
            )
        )).single()
        val øktInntektMedRefusjonOgPerson = Utbetalingstidslinje.betale(inntekt * 2, listOf(
            tidslinjeOf(
                5.NAV(
                    dekningsgrunnlag = (inntekt * 2).dagligInt,
                    refusjonsbeløp = inntekt.dagligInt
                )
            )
        )).single()
        val øktInntektMedPerson = Utbetalingstidslinje.betale(inntekt * 2, listOf(
            tidslinjeOf(
                5.NAV(
                    dekningsgrunnlag = (inntekt * 2).dagligInt,
                    refusjonsbeløp = 0
                )
            )
        )).single()
        val ingenRefusjon = Utbetalingstidslinje.betale(inntekt, listOf(
            tidslinjeOf(
                5.NAV(
                    dekningsgrunnlag = inntekt.dagligInt,
                    refusjonsbeløp = 0
                )
            )
        )).single()

        Assertions.assertTrue(fullRefusjon.negativEndringIBeløp(ingenRefusjon)) {
            "mindre personbeløp vil starte en tilbakekreving fra bruker"
        }
        Assertions.assertFalse(øktInntektMedRefusjonOgPerson.negativEndringIBeløp(ingenRefusjon)) {
            "inntekt økes slik at bruker fremdeles får utbetalt samme så er det ikke en negativ endring for personen"
        }
        Assertions.assertFalse(øktInntektMedPerson.negativEndringIBeløp(øktInntektMedRefusjonOgPerson)) {
            "totalbeløp er det samme selv om arbeidsgiverrefusjonen har byttet hender til personen"
        }
    }

    @Test
    fun betale() {
        val `6G`= 2161.daglig
        val input = listOf(tidslinjeOf(1.NAV(1081)), tidslinjeOf(1.NAV(1081)))
        val result = Utbetalingstidslinje.betale(`6G`, input)

        input.forEachIndexed { index, input ->
            Assertions.assertNull(input[1.januar].økonomi.inspektør.arbeidsgiverbeløp) { "den uberegnede listen skal ikke modifiseres" }
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
                val dag = tredje[dato] as Utbetalingsdag.AvvistDag
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
}

fun Utbetalingstidslinje.Companion.betale(sykepengegrunnlagBegrenset6G: Inntekt, tidslinjer: List<Utbetalingstidslinje>, andreYtelser: (dato: LocalDate) -> Prosentdel = { 0.prosent }) = Utbetalingstidslinje.betale(sykepengegrunnlagBegrenset6G, tidslinjer, andreYtelser)
