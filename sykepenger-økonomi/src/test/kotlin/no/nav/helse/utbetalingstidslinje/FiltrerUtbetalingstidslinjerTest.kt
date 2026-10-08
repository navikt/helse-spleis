package no.nav.helse.utbetalingstidslinje

import no.nav.helse.erHelg
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.testhelpers.AP
import no.nav.helse.testhelpers.ARB
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.NAVDAGER
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingstidslinje.Begrunnelse.*
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class FiltrerUtbetalingstidslinjerTest {
    private companion object {
        private val `32 år 10 januar 2018` = 10.januar(1988)
        private val a1 = Arbeidsgiverberegning.Inntektskilde.Yrkesaktivitet.Arbeidstaker("a1")
        private val a2 = Arbeidsgiverberegning.Inntektskilde.Yrkesaktivitet.Arbeidstaker("a2")
    }

    @Test
    fun `en arbeidsgiver - bare avslag`() {
        val input = beregning(tidslinjeOf(16.AP, 15.NAV), a1)
        val result =
            undersøke(
                uberegnetTidslinjePerArbeidsgiver = listOf(input),
                erMedlemAvFolketrygden = false,
                harOpptjening = false,
                erUnderMinsteinntektskravTilFylte67 = true,
                erUnderMinsteinntektEtterFylte67 = true,
                historiskTidslinje = tidslinjeOf(248.NAVDAGER, startDato = 1.januar(2017)),
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
        val input =
            listOf(
                beregning(tidslinjeOf(16.AP(inntekt), 15.NAV(inntekt)), a1),
                beregning(null, ghostsOgAndreInntektskilder = listOf(tidslinjeOf(31.ARB(inntekt))), yrkesaktivitet = a2),
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
        val input =
            listOf(
                beregning(tidslinjeOf(16.AP(inntekt), 15.NAV(inntekt)), a1),
                beregning(tidslinjeOf(16.AP(inntekt), 15.NAV(inntekt)), a2),
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
        val input =
            listOf(
                beregning(tidslinjeOf(16.AP(inntekt), 15.NAV(inntekt)), a1),
                beregning(tidslinjeOf(16.AP(inntekt), 15.NAV(inntekt)), a2),
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

    @Test
    fun `to arbeidsgivere - graderte andre ytelser graderes kun mot yrkesaktiviteten de gjelder`() {
        val inntekt = 1200
        val input =
            listOf(
                beregning(tidslinjeOf(16.AP(inntekt), 15.NAV(inntekt)), a1),
                beregning(tidslinjeOf(16.AP(inntekt), 15.NAV(inntekt)), a2),
            )
        val result =
            undersøke(
                uberegnetTidslinjePerArbeidsgiver = input,
                sykepengegrunnlagBegrenset6G = (2 * inntekt).daglig,
                graderteAndreYtelser = { inntektskilde, _ -> if (inntektskilde == a1) 50.prosent else 0.prosent },
            )

        // a1: 100 % - 50 % = 50 %, a2: 100 % => (50 % * 1200 + 100 % * 1200) / 2400 = 75 %
        result.forEach { beregnetPeriode ->
            beregnetPeriode.utbetalingstidslinje.inspektør.also { inspektør ->
                assertEquals(11, inspektør.navdager.size)
                assertEquals(75.prosent, inspektør.totalSykdomsgrad(17.januar))
                assertEquals(75.prosent, inspektør.utbetalingsgrad(17.januar))
                assertEquals(900 * 11, inspektør.totalUtbetaling)
            }
        }
    }

    @Test
    fun `graderte andre ytelser trekkes direkte fra sykdomsgraden uten hensyn til arbeid`() {
        val inntekt = 1200
        val input = beregning(tidslinjeOf(16.AP(inntekt), 15.NAV(inntekt, 50.0)), a1)
        val result =
            undersøke(
                uberegnetTidslinjePerArbeidsgiver = listOf(input),
                sykepengegrunnlagBegrenset6G = inntekt.daglig,
                graderteAndreYtelser = { inntektskilde, _ -> if (inntektskilde == a1) 40.prosent else 0.prosent },
            ).single()

        result.utbetalingstidslinje.inspektør.also { inspektør ->
            assertEquals(11, inspektør.avvistDagTeller)
            assertEquals(10.prosent, inspektør.totalSykdomsgrad(17.januar))
            assertEquals(listOf(MinimumSykdomsgrad), inspektør.begrunnelse(17.januar))
        }
    }

    @Test
    fun `graderte andre ytelser for ukjent yrkesaktivitet påvirker ikke beregningen`() {
        val inntekt = 1200
        val input = beregning(tidslinjeOf(16.AP(inntekt), 15.NAV(inntekt)), a1)
        val result =
            undersøke(
                uberegnetTidslinjePerArbeidsgiver = listOf(input),
                sykepengegrunnlagBegrenset6G = inntekt.daglig,
                graderteAndreYtelser = { inntektskilde, _ -> if (inntektskilde == a2) 50.prosent else 0.prosent },
            ).single()

        result.utbetalingstidslinje.inspektør.also { inspektør ->
            assertEquals(100.prosent, inspektør.totalSykdomsgrad(17.januar))
            assertEquals(inntekt * 11, inspektør.totalUtbetaling)
        }
    }

    private fun beregning(
        utbetalingstidslinje: Utbetalingstidslinje?,
        yrkesaktivitet: Arbeidsgiverberegning.Inntektskilde.Yrkesaktivitet,
        ghostsOgAndreInntektskilder: List<Utbetalingstidslinje> = emptyList(),
    ): Arbeidsgiverberegning {
        val vedtaksperioder =
            utbetalingstidslinje?.let {
                listOf(
                    Vedtaksperiodeberegning(
                        vedtaksperiodeId = UUID.randomUUID(),
                        utbetalingstidslinje = utbetalingstidslinje,
                    ),
                )
            } ?: emptyList()
        return Arbeidsgiverberegning(
            inntektskilde = yrkesaktivitet,
            vedtaksperioder = vedtaksperioder,
            ghostOgAndreInntektskilder = ghostsOgAndreInntektskilder,
        )
    }

    private fun undersøke(
        uberegnetTidslinjePerArbeidsgiver: Arbeidsgiverberegning,
        sykepengegrunnlagBegrenset6G: Inntekt = 312_000.årlig,
    ) = undersøke(listOf(uberegnetTidslinjePerArbeidsgiver), sykepengegrunnlagBegrenset6G).single()

    private fun undersøke(
        uberegnetTidslinjePerArbeidsgiver: List<Arbeidsgiverberegning>,
        sykepengegrunnlagBegrenset6G: Inntekt = 312_000.årlig,
        erMedlemAvFolketrygden: Boolean = true,
        harOpptjening: Boolean = true,
        erUnderMinsteinntektskravTilFylte67: Boolean = false,
        erUnderMinsteinntektEtterFylte67: Boolean = false,
        regler: MaksimumSykepengedagerregler = MaksimumSykepengedagerregler.Companion.NormalArbeidstaker,
        historiskTidslinje: Utbetalingstidslinje = Utbetalingstidslinje(),
        graderteAndreYtelser: GraderteAndreYtelser = IngenGraderteAndreYtelser,
    ): List<BeregnetPeriode> {
        val result =
            filtrerUtbetalingstidslinjer(
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
                regler = regler,
                graderteAndreYtelser = graderteAndreYtelser,
            )
        return result
    }
}
