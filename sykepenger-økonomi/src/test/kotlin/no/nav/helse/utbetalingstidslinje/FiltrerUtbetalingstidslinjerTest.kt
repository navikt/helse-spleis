package no.nav.helse.utbetalingstidslinje

import java.util.UUID
import no.nav.helse.erHelg
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.testhelpers.AP
import no.nav.helse.testhelpers.ARB
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.NAVDAGER
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingstidslinje.Begrunnelse.ManglerMedlemskap
import no.nav.helse.utbetalingstidslinje.Begrunnelse.ManglerOpptjening
import no.nav.helse.utbetalingstidslinje.Begrunnelse.MinimumInntekt
import no.nav.helse.utbetalingstidslinje.Begrunnelse.SykepengedagerOppbrukt
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FiltrerUtbetalingstidslinjerTest {

    private companion object {
        private val `32 år 10 januar 2018` = 10.januar(1988)
        private val a1 = Arbeidsgiverberegning.Inntektskilde.Yrkesaktivitet.Arbeidstaker("a1")
        private val a2 = Arbeidsgiverberegning.Inntektskilde.Yrkesaktivitet.Arbeidstaker("a2")
    }

    @Test
    fun `en arbeidsgiver - bare avslag`() {
        val input = beregning(tidslinjeOf(16.AP, 15.NAV), a1)
        val result = undersøke(
            uberegnetTidslinjePerArbeidsgiver = listOf(input),
            erMedlemAvFolketrygden = false,
            harOpptjening = false,
            erUnderMinsteinntektskravTilFylte67 = true,
            erUnderMinsteinntektEtterFylte67 = true,
            historiskTidslinje = tidslinjeOf(248.NAVDAGER, startDato = 1.januar(2017))
        )

        result.single().utbetalingstidslinje.inspektør.also { inspektør ->
            assertEquals(11, inspektør.avvistDagTeller)
            assertEquals(0, inspektør.navdager.size)
            assertEquals(0, inspektør.totalUtbetaling)
            (17.januar til 31.januar)
                .filter { !it.erHelg() }
                .forEach {
                    assertEquals(listOf(MinimumInntekt, ManglerMedlemskap, ManglerOpptjening, SykepengedagerOppbrukt), inspektør.begrunnelse(it))
                }
        }
    }

    @Test
    fun `en arbeidsgiver - under 6g`() {
        val inntekt = 1200
        val input = beregning(tidslinjeOf(16.AP(inntekt), 15.NAV(inntekt)), a1)
        val result = undersøke(input, sykepengegrunnlagBegrenset6G = inntekt.daglig)

        result.utbetalingstidslinje.inspektør.also { inspektør ->
            assertEquals(11, inspektør.navdager.size)
            assertEquals(inntekt * 11, inspektør.totalUtbetaling)
            (17.januar til 31.januar)
                .filter { !it.erHelg() }
                .forEach {
                    assertEquals(inntekt, inspektør.arbeidsgiverbeløp(it)?.dagligInt)
                    assertEquals(0, inspektør.personbeløp(it)?.dagligInt)
                }
        }
    }

    @Test
    fun `en arbeidsgiver - over 6g`() {
        val inntekt = 1200
        val input = beregning(tidslinjeOf(16.AP(inntekt), 15.NAV(inntekt)), a1)
        val result = undersøke(input, sykepengegrunnlagBegrenset6G = (inntekt / 2).daglig)

        result.utbetalingstidslinje.inspektør.also { inspektør ->
            assertEquals(11, inspektør.navdager.size)
            assertEquals(inntekt * 11 / 2, inspektør.totalUtbetaling)
            (17.januar til 31.januar)
                .filter { !it.erHelg() }
                .forEach {
                    assertEquals(inntekt / 2, inspektør.arbeidsgiverbeløp(it)?.dagligInt)
                    assertEquals(0, inspektør.personbeløp(it)?.dagligInt)
                }
        }
    }

    @Test
    fun `en arbeidsgiver - med ghost`() {
        val inntekt = 1200
        val input = listOf(
            beregning(tidslinjeOf(16.AP(inntekt), 15.NAV(inntekt)), a1),
            beregning(null, ghostsOgAndreInntektskilder = listOf(tidslinjeOf(31.ARB(inntekt))), yrkesaktivitet = a2)
        )
        val result = undersøke(input, sykepengegrunnlagBegrenset6G = inntekt.daglig)

        result[0].utbetalingstidslinje.inspektør.also { inspektør ->
            assertEquals(11, inspektør.navdager.size)
            assertEquals(inntekt * 11 / 2, inspektør.totalUtbetaling)
            (17.januar til 31.januar)
                .filter { !it.erHelg() }
                .forEach {
                    assertEquals(inntekt / 2, inspektør.arbeidsgiverbeløp(it)?.dagligInt)
                    assertEquals(0, inspektør.personbeløp(it)?.dagligInt)
                }
        }
    }

    @Test
    fun `to arbeidsgivere - under 6g`() {
        val inntekt = 1200
        val input = listOf(
            beregning(tidslinjeOf(16.AP(inntekt), 15.NAV(inntekt)), a1),
            beregning(tidslinjeOf(16.AP(inntekt), 15.NAV(inntekt)), a2)
        )
        val result = undersøke(input, sykepengegrunnlagBegrenset6G = (2 * inntekt).daglig)

        result[0].utbetalingstidslinje.inspektør.also { inspektør ->
            assertEquals(11, inspektør.navdager.size)
            assertEquals(inntekt * 11, inspektør.totalUtbetaling)
            (17.januar til 31.januar)
                .filter { !it.erHelg() }
                .forEach {
                    assertEquals(inntekt, inspektør.arbeidsgiverbeløp(it)?.dagligInt)
                    assertEquals(0, inspektør.personbeløp(it)?.dagligInt)
                }
        }
        result[1].utbetalingstidslinje.inspektør.also { inspektør ->
            assertEquals(11, inspektør.navdager.size)
            assertEquals(inntekt * 11, inspektør.totalUtbetaling)
            (17.januar til 31.januar)
                .filter { !it.erHelg() }
                .forEach {
                    assertEquals(inntekt, inspektør.arbeidsgiverbeløp(it)?.dagligInt)
                    assertEquals(0, inspektør.personbeløp(it)?.dagligInt)
                }
        }
    }

    @Test
    fun `to arbeidsgivere - over 6g`() {
        val inntekt = 1200
        val input = listOf(
            beregning(tidslinjeOf(16.AP(inntekt), 15.NAV(inntekt)), a1),
            beregning(tidslinjeOf(16.AP(inntekt), 15.NAV(inntekt)), a2)
        )
        val result = undersøke(input, sykepengegrunnlagBegrenset6G = inntekt.daglig)

        result[0].utbetalingstidslinje.inspektør.also { inspektør ->
            assertEquals(11, inspektør.navdager.size)
            assertEquals(inntekt * 11 / 2, inspektør.totalUtbetaling)
            (17.januar til 31.januar)
                .filter { !it.erHelg() }
                .forEach {
                    assertEquals(inntekt / 2, inspektør.arbeidsgiverbeløp(it)?.dagligInt)
                    assertEquals(0, inspektør.personbeløp(it)?.dagligInt)
                }
        }
        result[1].utbetalingstidslinje.inspektør.also { inspektør ->
            assertEquals(11, inspektør.navdager.size)
            assertEquals(inntekt * 11 / 2, inspektør.totalUtbetaling)
            (17.januar til 31.januar)
                .filter { !it.erHelg() }
                .forEach {
                    assertEquals(inntekt / 2, inspektør.arbeidsgiverbeløp(it)?.dagligInt)
                    assertEquals(0, inspektør.personbeløp(it)?.dagligInt)
                }
        }
    }

    private fun beregning(utbetalingstidslinje: Utbetalingstidslinje?, yrkesaktivitet: Arbeidsgiverberegning.Inntektskilde.Yrkesaktivitet, ghostsOgAndreInntektskilder: List<Utbetalingstidslinje> = emptyList()): Arbeidsgiverberegning {
        val vedtaksperioder = utbetalingstidslinje?.let {
            listOf(
                Vedtaksperiodeberegning(
                    vedtaksperiodeId = UUID.randomUUID(),
                    utbetalingstidslinje = utbetalingstidslinje
                )
            )
        } ?: emptyList()
        return Arbeidsgiverberegning(
            inntektskilde = yrkesaktivitet,
            vedtaksperioder = vedtaksperioder,
            ghostOgAndreInntektskilder = ghostsOgAndreInntektskilder
        )
    }

    private fun undersøke(uberegnetTidslinjePerArbeidsgiver: Arbeidsgiverberegning, sykepengegrunnlagBegrenset6G: Inntekt = 312_000.årlig) =
        undersøke(listOf(uberegnetTidslinjePerArbeidsgiver), sykepengegrunnlagBegrenset6G).single()

    private fun undersøke(
        uberegnetTidslinjePerArbeidsgiver: List<Arbeidsgiverberegning>,
        sykepengegrunnlagBegrenset6G: Inntekt = 312_000.årlig,
        erMedlemAvFolketrygden: Boolean = true,
        harOpptjening: Boolean = true,
        erUnderMinsteinntektskravTilFylte67: Boolean = false,
        erUnderMinsteinntektEtterFylte67: Boolean = false,
        regler: MaksimumSykepengedagerregler = MaksimumSykepengedagerregler.Companion.NormalArbeidstaker,
        historiskTidslinje: Utbetalingstidslinje = Utbetalingstidslinje()
    ): List<BeregnetPeriode> {
        val result = filtrerUtbetalingstidslinjer(
            uberegnetTidslinjePerArbeidsgiver = uberegnetTidslinjePerArbeidsgiver,
            sykepengegrunnlagBegrenset6G = sykepengegrunnlagBegrenset6G,
            erMedlemAvFolketrygden = erMedlemAvFolketrygden,
            harOpptjening = harOpptjening,
            sekstisyvårsdagen = `32 år 10 januar 2018`.plusYears(67),
            syttiårsdagen = `32 år 10 januar 2018`.plusYears(70),
            dødsdato = null,
            erUnderMinsteinntektskravTilFylte67 = erUnderMinsteinntektskravTilFylte67,
            erUnderMinsteinntektEtterFylte67 = erUnderMinsteinntektEtterFylte67,
            historisktidslinje = historiskTidslinje,
            perioderMedMinimumSykdomsgradVurdertOK = emptySet(),
            regler = regler
        )
        return result
    }
}
