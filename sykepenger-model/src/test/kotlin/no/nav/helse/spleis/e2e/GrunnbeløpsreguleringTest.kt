package no.nav.helse.spleis.e2e

import java.time.LocalDate
import no.nav.helse.Grunnbeløp
import no.nav.helse.april
import no.nav.helse.august
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.oktober
import no.nav.helse.september
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Oppdragstatus.AKSEPTERT
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

internal class GrunnbeløpsreguleringTest : AbstractEndToEndTest() {
    private companion object {
        private val HØY_INNTEKT = 800000.00.årlig
        private val GYLDIGHETSDATO_2020_GRUNNBELØP = 21.september(2020)
        private const val ORGNUMMER_AG2 = "654326881"
    }

    @Test
    fun `siste periode på en fagsystemID håndterer grunnbeløpsregulering`() {
        utbetaltVedtaksperiodeBegrensetAv6G(1, 1.april(2020), 31.mai(2020)) // skjæringstidspunkt før 1. mai
        utbetaltVedtaksperiodeBegrensetAv6G(2, 10.juni(2020), 30.juni(2020)) // gap, ny skjæringstidspunkt 10.juni
        utbetaltForlengetVedtaksperiodeBegrensetAv6G(3, 1.juli(2020), 31.juli(2020))
        utbetaltVedtaksperiodeBegrensetAv6G(4, 10.august(2020), 31.august(2020)) // gap, ny skjæringstidspunkt 10. august
        assertEquals(4, inspektør.utbetalinger.size)
        håndterGrunnbeløpsregulering(gyldighetsdato = GYLDIGHETSDATO_2020_GRUNNBELØP)
        assertEquals(5, inspektør.utbetalinger.size)
        val etterutbetaling = inspektør.utbetalinger.last()
        assertTrue(etterutbetaling.inspektør.erEtterutbetaling)
    }

    @Test
    fun `etterutbetale en som gikk til maksdato`() {
        utbetaltVedtaksperiodeBegrensetAv6G(1, 1.august(2019), 31.august(2019))
        utbetaltForlengetVedtaksperiodeBegrensetAv6G(2, 1.september(2019), 30.september(2019))
        utbetaltForlengetVedtaksperiodeBegrensetAv6G(3, 1.oktober(2019), 31.oktober(2019))
        utbetaltForlengetVedtaksperiodeBegrensetAv6G(4, 1.november(2019), 30.november(2019))
        utbetaltForlengetVedtaksperiodeBegrensetAv6G(5, 1.desember(2019), 31.desember(2019))
        utbetaltForlengetVedtaksperiodeBegrensetAv6G(6, 1.januar(2020), 31.januar(2020))
        utbetaltForlengetVedtaksperiodeBegrensetAv6G(7, 1.februar(2020), 28.februar(2020))
        utbetaltForlengetVedtaksperiodeBegrensetAv6G(8, 1.mars(2020), 31.mars(2020))
        utbetaltForlengetVedtaksperiodeBegrensetAv6G(9, 1.april(2020), 30.april(2020))

        utbetaltVedtaksperiodeBegrensetAv6G(10, 14.mai(2020), 31.mai(2020))
        utbetaltForlengetVedtaksperiodeBegrensetAv6G(11, 1.juni(2020), 30.juni(2020))
        utbetaltForlengetVedtaksperiodeBegrensetAv6G(12, 1.juli(2020), 31.juli(2020))
        utbetaltForlengetVedtaksperiodeBegrensetAv6G(13, 1.august(2020), 31.august(2020))

        assertEquals(11.august(2020), inspektør.sisteMaksdato(13.vedtaksperiode))
        håndterGrunnbeløpsregulering(gyldighetsdato = GYLDIGHETSDATO_2020_GRUNNBELØP)
        assertEquals(14, inspektør.utbetalinger.size)
        val etterutbetaling = inspektør.utbetalinger.last()
        assertTrue(etterutbetaling.inspektør.erEtterutbetaling)
        assertEquals(11.august(2020), etterutbetaling.inspektør.arbeidsgiverOppdrag.sistedato)
    }

    @Test
    fun `ignorerer flere arbeidsgivere uten utbetalinger`() {
        utbetaltVedtaksperiodeBegrensetAv6G(1, 10.juni(2020), 30.juni(2020), inntekter = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.juni(2019) til 1.mai(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                    ORGNUMMER_AG2 inntekt INNTEKT
                }
            }
        ))
        assertEquals(1, inspektør.utbetalinger.size)
        håndterGrunnbeløpsregulering(gyldighetsdato = GYLDIGHETSDATO_2020_GRUNNBELØP)
        assertEquals(2, inspektør.utbetalinger.size)
        val etterutbetaling = inspektør.utbetalinger.last()
        assertTrue(etterutbetaling.inspektør.erEtterutbetaling)
    }

    @Test
    fun `etterutbetaling har samme data som siste utbetalte`() {
        utbetaltVedtaksperiodeBegrensetAv6G(1, 1.april(2020), 31.mai(2020)) // skjæringstidspunkt før 1. mai
        utbetaltVedtaksperiodeBegrensetAv6G(2, 10.juni(2020), 30.juni(2020)) // gap, ny skjæringstidspunkt 10.juni
        utbetaltForlengetVedtaksperiodeBegrensetAv6G(3, 1.juli(2020), 31.juli(2020))
        utbetaltVedtaksperiodeBegrensetAv6G(4, 10.august(2020), 31.august(2020)) // gap, ny skjæringstidspunkt 10. august
        håndterGrunnbeløpsregulering(gyldighetsdato = GYLDIGHETSDATO_2020_GRUNNBELØP)
        val etterutbetaling = inspektør.utbetaling(4)
        assertTrue(etterutbetaling.inspektør.erEtterutbetaling)
        assertEquals(inspektør.maksdato(3), inspektør.maksdato(4))
        assertEquals(inspektør.forbrukteSykedager(3), inspektør.forbrukteSykedager(4))
        assertEquals(inspektør.gjenståendeSykedager(3), inspektør.gjenståendeSykedager(4))
    }

    @Test
    fun `ubetalt periode etterutbetales ikke`() {
        utbetaltVedtaksperiodeBegrensetAv6G(1, 10.juni(2020), 30.juni(2020))
        ubetaltVedtaksperiodeBegrensetAv6G(2, 10.juli(2020), 30.juli(2020))
        håndterGrunnbeløpsregulering(gyldighetsdato = GYLDIGHETSDATO_2020_GRUNNBELØP)
        assertEquals(2, inspektør.utbetalinger.size)
        assertTrue(inspektør.utbetalinger.none { it.inspektør.erEtterutbetaling })
    }

    @Test
    fun `ubetalt periode, etter utbetalt, etterutbetales ikke`() {
        (10.juni(2020) to 30.juni(2020)).also { (fom, tom) ->
            håndterSykmelding(Sykmeldingsperiode(fom, tom, 100.prosent))
            håndterSøknad(Sykdom(fom, tom, 100.prosent), sendtTilNAVEllerArbeidsgiver = tom)
            håndterInntektsmeldingMedValidering(
                1.vedtaksperiode,
                listOf(fom til fom.plusDays(15)),
                førsteFraværsdag = fom,
                beregnetInntekt = HØY_INNTEKT
            )
        }
        (1.juli(2020) to 30.juli(2020)).also { (fom, tom) ->
            håndterSykmelding(Sykmeldingsperiode(fom, tom, 100.prosent))
        }

        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, HØY_INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.juni(2019) til 1.mai(2020) inntekter {
                    ORGNUMMER inntekt HØY_INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt()

        håndterGrunnbeløpsregulering(gyldighetsdato = GYLDIGHETSDATO_2020_GRUNNBELØP)
        inspektør.utbetalingUtbetalingstidslinje(0).also { utbetalingUtbetalingstidslinje ->
            assertEquals(10.juni(2020) til 30.juni(2020), utbetalingUtbetalingstidslinje.periode())
        }
        inspektør.utbetaling(0).also { utbetaling ->
            assertEquals(26.juni(2020), utbetaling.inspektør.arbeidsgiverOppdrag.førstedato)
            assertEquals(30.juni(2020), utbetaling.inspektør.arbeidsgiverOppdrag.sistedato)
            assertEquals(1, utbetaling.inspektør.arbeidsgiverOppdrag.size)
        }
        inspektør.utbetalingUtbetalingstidslinje(1).also { utbetalingUtbetalingstidslinje ->
            assertEquals(10.juni(2020) til 30.juni(2020), utbetalingUtbetalingstidslinje.periode())
        }
        inspektør.utbetaling(1).also { utbetaling ->
            assertEquals(26.juni(2020), utbetaling.inspektør.arbeidsgiverOppdrag.førstedato)
            assertEquals(30.juni(2020), utbetaling.inspektør.arbeidsgiverOppdrag.sistedato)
            assertEquals(1, utbetaling.inspektør.arbeidsgiverOppdrag.size)
        }
    }

    @Test
    fun `ubetalt annullering etterutbetales ikke`() {
        utbetaltVedtaksperiodeBegrensetAv6G(1, 1.april(2020), 31.mai(2020)) // skjæringstidspunkt før 1. mai
        utbetaltVedtaksperiodeBegrensetAv6G(2, 10.juni(2020), 30.juni(2020)) // gap, ny skjæringstidspunkt 10.juni
        håndterAnnullerUtbetaling(fagsystemId = inspektør.fagsystemId(2.vedtaksperiode))
        håndterGrunnbeløpsregulering(gyldighetsdato = GYLDIGHETSDATO_2020_GRUNNBELØP)
        assertEquals(3, inspektør.utbetalinger.size)
        assertTrue(inspektør.utbetalinger.none { it.inspektør.erEtterutbetaling })
    }

    @Test
    fun `annullert utbetalig etterutbetales ikke`() {
        utbetaltVedtaksperiodeBegrensetAv6G(1, 1.april(2020), 31.mai(2020)) // skjæringstidspunkt før 1. mai
        utbetaltVedtaksperiodeBegrensetAv6G(2, 10.juni(2020), 30.juni(2020)) // gap, ny skjæringstidspunkt 10.juni
        håndterAnnullerUtbetaling(fagsystemId = inspektør.fagsystemId(2.vedtaksperiode))
        håndterUtbetalt()
        håndterGrunnbeløpsregulering(gyldighetsdato = GYLDIGHETSDATO_2020_GRUNNBELØP)
        assertEquals(3, inspektør.utbetalinger.size)
        assertTrue(inspektør.utbetalinger.none { it.inspektør.erEtterutbetaling })
    }

    @Test
    fun `grunnbeløpsregulering ødelegger ikke for oppdragene frem i tid`() {
        utbetaltVedtaksperiodeBegrensetAv6G(1, 1.mai(2020), 31.mai(2020))
        utbetaltVedtaksperiodeBegrensetAv6G(2, 5.juli(2020), 31.juli(2020))
        håndterGrunnbeløpsregulering(
            gyldighetsdato = GYLDIGHETSDATO_2020_GRUNNBELØP,
            fagsystemId = inspektør.utbetalinger.first().inspektør.arbeidsgiverOppdrag.fagsystemId()
        )
        håndterUtbetalt()
        utbetaltForlengetVedtaksperiodeBegrensetAv6G(3, 1.august(2020), 31.august(2020))
        inspektør.utbetalinger.also { utbetalinger ->
            assertEquals(4, utbetalinger.size)
            assertEquals(inspektør.gjeldendeUtbetalingForVedtaksperiode(1.vedtaksperiode), utbetalinger[0])
            assertEquals(inspektør.gjeldendeUtbetalingForVedtaksperiode(2.vedtaksperiode), utbetalinger[1])
            assertEquals(inspektør.gjeldendeUtbetalingForVedtaksperiode(3.vedtaksperiode), utbetalinger[3])

            assertEquals(utbetalinger[0].inspektør.arbeidsgiverOppdrag.fagsystemId(), utbetalinger[2].inspektør.arbeidsgiverOppdrag.fagsystemId())
            assertTrue(utbetalinger[2].inspektør.erEtterutbetaling)
            assertNotEquals(utbetalinger[0].inspektør.arbeidsgiverOppdrag.fagsystemId(), utbetalinger[1].inspektør.arbeidsgiverOppdrag.fagsystemId())
            assertEquals(utbetalinger[1].inspektør.arbeidsgiverOppdrag.fagsystemId(), utbetalinger[3].inspektør.arbeidsgiverOppdrag.fagsystemId())
        }
    }

    @Test
    fun `justere tidligere perioder automatisk ved ny periode etter ny grunnbeløpsvirkning`() {
        utbetaltVedtaksperiodeBegrensetAv6G(1, 1.april(2020), 31.mai(2020)) // skjæringstidspunkt før 1. mai
        utbetaltVedtaksperiodeBegrensetAv6G(2, 10.juni(2020), 30.juni(2020)) // gap, ny skjæringstidspunkt 10.juni
        utbetaltForlengetVedtaksperiodeBegrensetAv6G(3, 1.juli(2020), 31.juli(2020))
        utbetaltForlengetVedtaksperiodeBegrensetAv6G(4, 1.august(2020), 31.august(2020))
        utbetaltForlengetVedtaksperiodeBegrensetAv6G(5, 1.september(2020), 30.september(2020)) // får ny G med én gang og betaler ut de forrige
        utbetaltVedtaksperiodeBegrensetAv6G(6, 10.oktober(2020), 31.oktober(2020)) // gap, ny skjæringstidspunkt 10. oktober, ingen etterutbetaling

        assertOppdrag(0, 1, 1.april(2020))
        assertUtbetalingslinjer(0, Endringskode.NY)
        assertOppdrag(1, 2, 1.april(2020), 10.juni(2020))
        assertUtbetalingslinjer(1, Endringskode.UEND, Endringskode.NY)
        assertOppdrag(4, 2, 1.april(2020), 21.september(2020)) // periode for september 1-september 30 vil plukke ny G og etterbetale
        assertUtbetalingslinjer(4, Endringskode.UEND, Endringskode.NY)
        assertOppdrag(5, 3, 1.april(2020), 10.oktober(2020))
        assertUtbetalingslinjer(5, Endringskode.UEND, Endringskode.UEND, Endringskode.NY)
    }

    @Test
    fun `justere periode som deler oppdrag med en periode som ikke skal justeres`() {
        val fomPeriode1 = 30.april(2020)
        val tomPeriode1 = fomPeriode1.plusMonths(1)
        utbetaltVedtaksperiodeBegrensetAv6G(1, fomPeriode1, tomPeriode1)

        val fomPeriode2 = tomPeriode1.plusDays(7)
        val tomPeriode2 = fomPeriode2.plusMonths(1)
        utbetaltVedtaksperiodeBegrensetAv6G(2, fomPeriode2, tomPeriode2, listOf(Periode(fomPeriode1, fomPeriode1.plusDays(15))), fomPeriode2)

        håndterGrunnbeløpsregulering(gyldighetsdato = GYLDIGHETSDATO_2020_GRUNNBELØP)

        val etterutbetaling = inspektør.utbetalinger.last()
        assertTrue(etterutbetaling.inspektør.erEtterutbetaling)
    }

    @Test
    fun `periode som skal ha 2019-sats får ikke 2020-sats`() {
        val fom = GYLDIGHETSDATO_2020_GRUNNBELØP.minusDays(1)
        val tom = fom.plusDays(31)
        utbetaltVedtaksperiodeBegrensetAv6G(1, fom, tom)
        håndterGrunnbeløpsregulering(gyldighetsdato = GYLDIGHETSDATO_2020_GRUNNBELØP)
        assertEquals(1, inspektør.utbetalinger.size)
        val etterutbetaling = inspektør.utbetalinger.last()
        assertFalse(etterutbetaling.inspektør.erEtterutbetaling)
    }

    @Test
    fun `periode med nytt grunnbeløp trengs ikke justering`() {
        val fom = GYLDIGHETSDATO_2020_GRUNNBELØP
        val tom = fom.plusDays(31)
        utbetaltVedtaksperiodeBegrensetAv6G(1, fom, tom)
        håndterGrunnbeløpsregulering(gyldighetsdato = GYLDIGHETSDATO_2020_GRUNNBELØP)
        assertEquals(1, inspektør.utbetalinger.size)
        val etterutbetaling = inspektør.utbetalinger.last()
        assertFalse(etterutbetaling.inspektør.erEtterutbetaling)
    }

    private fun assertOppdrag(indeks: Int, forventetAntall: Int, vararg forventetSkjæringstidspunkt: LocalDate) {
        inspektør.arbeidsgiverOppdrag[indeks].linjerUtenOpphør().also { linjer ->
            assertEquals(forventetAntall, linjer.size)
            linjer.forEachIndexed { linjeNr, linje ->
                val forventetBeløp = forventetSkjæringstidspunkt.elementAtOrElse(linjeNr) { forventetSkjæringstidspunkt.last() }
                    .let { Grunnbeløp.`6G`.beløp(it) }
                assertEquals(forventetBeløp.rundTilDaglig(),linje.beløp!!.daglig) { "Feil beløp for linje ${linje.fom} - ${linje.tom}" }
            }
        }
    }

    private fun assertUtbetalingslinjer(indeks: Int, vararg forventetEndringskoder: Endringskode) {
        inspektør.arbeidsgiverOppdrag[indeks].linjerUtenOpphør().also { linjer ->
            linjer.forEachIndexed { linjeNr, utbetalingslinje ->
                when (forventetEndringskoder[linjeNr]) {
                    Endringskode.UEND -> assertFalse(utbetalingslinje.erForskjell())
                    Endringskode.NY -> assertTrue(utbetalingslinje.erForskjell())
                    else -> fail { "Endringskode må være ${Endringskode.UEND} eller ${Endringskode.NY}" }
                }
            }
        }
    }

    private fun ubetaltVedtaksperiodeBegrensetAv6G(
        vedtaksperiodeIndeks: Int,
        fom: LocalDate,
        tom: LocalDate,
        arbeidsgiverperiode: List<Periode> = listOf(Periode(fom, fom.plusDays(15))),
        førsteFraværsdag: LocalDate = fom,
        inntekter: Inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                fom.minusYears(1) til fom.minusMonths(1) inntekter {
                    ORGNUMMER inntekt HØY_INNTEKT
                }
            }
        )
    ) {
        håndterSykmelding(Sykmeldingsperiode(fom, tom, 100.prosent))
        håndterSøknad(Sykdom(fom, tom, 100.prosent), sendtTilNAVEllerArbeidsgiver = tom)
        håndterInntektsmeldingMedValidering(
            vedtaksperiodeIndeks.vedtaksperiode,
            arbeidsgiverperiode,
            førsteFraværsdag = førsteFraværsdag,
            beregnetInntekt = HØY_INNTEKT
        )
        håndterYtelser(vedtaksperiodeIndeks.vedtaksperiode)
        håndterVilkårsgrunnlag(vedtaksperiodeIndeks.vedtaksperiode, HØY_INNTEKT, inntektsvurdering = inntekter)
        håndterYtelser(vedtaksperiodeIndeks.vedtaksperiode)
        håndterSimulering(vedtaksperiodeIndeks.vedtaksperiode)
        håndterUtbetalingsgodkjenning(vedtaksperiodeIndeks.vedtaksperiode, true)
    }

    private fun utbetaltVedtaksperiodeBegrensetAv6G(
        vedtaksperiodeIndeks: Int,
        fom: LocalDate,
        tom: LocalDate,
        arbeidsgiverperiode: List<Periode> = listOf(Periode(
            fom,
            fom.plusDays(15)
        )),
        førsteFraværsdag: LocalDate = fom,
        inntekter: Inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                fom.minusYears(1) til fom.minusMonths(1) inntekter {
                    ORGNUMMER inntekt HØY_INNTEKT
                }
            }
        )
    ) {
        ubetaltVedtaksperiodeBegrensetAv6G(vedtaksperiodeIndeks, fom, tom, arbeidsgiverperiode, førsteFraværsdag, inntekter)
        håndterUtbetalt(AKSEPTERT)
    }

    private fun utbetaltForlengetVedtaksperiodeBegrensetAv6G(
        vedtaksperiodeIndeks: Int,
        fom: LocalDate,
        tom: LocalDate
    ) {
        håndterSykmelding(Sykmeldingsperiode(fom, tom, 100.prosent))
        håndterSøknad(Sykdom(fom, tom, 100.prosent), sendtTilNAVEllerArbeidsgiver = tom)
        håndterYtelser(vedtaksperiodeIndeks.vedtaksperiode)
        håndterSimulering(vedtaksperiodeIndeks.vedtaksperiode)
        håndterUtbetalingsgodkjenning(vedtaksperiodeIndeks.vedtaksperiode, true)
        håndterUtbetalt(AKSEPTERT)
    }
}
