package no.nav.helse.spleis.e2e

import no.nav.helse.Grunnbeløp
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.UtbetalingHendelse.Oppdragstatus.AKSEPTERT
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class GrunnbeløpsreguleringTest : AbstractEndToEndTest() {
    private companion object {
        private val HØY_INNTEKT = 800000.00.årlig
        private val GYLDIGHETSDATO_2020_GRUNNBELØP = 21.september(2020)
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
        assertTrue(etterutbetaling.erEtterutbetaling())
    }

    @Test
    fun `etterutbetaling har samme data som siste utbetalte`() {
        utbetaltVedtaksperiodeBegrensetAv6G(1, 1.april(2020), 31.mai(2020)) // skjæringstidspunkt før 1. mai
        utbetaltVedtaksperiodeBegrensetAv6G(2, 10.juni(2020), 30.juni(2020)) // gap, ny skjæringstidspunkt 10.juni
        utbetaltForlengetVedtaksperiodeBegrensetAv6G(3, 1.juli(2020), 31.juli(2020))
        utbetaltVedtaksperiodeBegrensetAv6G(4, 10.august(2020), 31.august(2020)) // gap, ny skjæringstidspunkt 10. august
        håndterGrunnbeløpsregulering(gyldighetsdato = GYLDIGHETSDATO_2020_GRUNNBELØP)
        val etterutbetaling = inspektør.utbetaling(4)
        assertTrue(etterutbetaling.erEtterutbetaling())
        assertEquals(inspektør.maksdato(3), inspektør.maksdato(4))
        assertEquals(inspektør.forbrukteSykedager(3), inspektør.forbrukteSykedager(4))
        assertEquals(inspektør.gjenståendeSykedager(3), inspektør.gjenståendeSykedager(4))
    }

    @Test
    fun `ubetalt periode etterutbetales ikke`() {
        utbetaltVedtaksperiodeBegrensetAv6G(1, 10.juni(2020), 30.juni(2020)) // skjæringstidspunkt før 1. mai
        ubetaltVedtaksperiodeBegrensetAv6G(2, 10.juli(2020), 30.juli(2020)) // gap, ny skjæringstidspunkt 10.juni
        håndterGrunnbeløpsregulering(gyldighetsdato = GYLDIGHETSDATO_2020_GRUNNBELØP)
        assertEquals(2, inspektør.utbetalinger.size)
        assertTrue(inspektør.utbetalinger.none { it.erEtterutbetaling() })
    }

    @Test
    fun `ubetalt annullering etterutbetales ikke`() {
        utbetaltVedtaksperiodeBegrensetAv6G(1, 1.april(2020), 31.mai(2020)) // skjæringstidspunkt før 1. mai
        utbetaltVedtaksperiodeBegrensetAv6G(2, 10.juni(2020), 30.juni(2020)) // gap, ny skjæringstidspunkt 10.juni
        håndterAnnullerUtbetaling(fagsystemId = inspektør.fagsystemId(2.vedtaksperiode))
        håndterGrunnbeløpsregulering(gyldighetsdato = GYLDIGHETSDATO_2020_GRUNNBELØP)
        assertEquals(3, inspektør.utbetalinger.size)
        assertTrue(inspektør.utbetalinger.none { it.erEtterutbetaling() })
    }

    @Test
    fun `annullert utbetalig etterutbetales ikke`() {
        utbetaltVedtaksperiodeBegrensetAv6G(1, 1.april(2020), 31.mai(2020)) // skjæringstidspunkt før 1. mai
        utbetaltVedtaksperiodeBegrensetAv6G(2, 10.juni(2020), 30.juni(2020)) // gap, ny skjæringstidspunkt 10.juni
        håndterAnnullerUtbetaling(fagsystemId = inspektør.fagsystemId(2.vedtaksperiode))
        håndterUtbetalt()
        håndterGrunnbeløpsregulering(gyldighetsdato = GYLDIGHETSDATO_2020_GRUNNBELØP)
        assertEquals(3, inspektør.utbetalinger.size)
        assertTrue(inspektør.utbetalinger.none { it.erEtterutbetaling() })
    }

    @Test
    fun `grunnbeløpsregulering ødelegger ikke for oppdragene frem i tid`() {
        utbetaltVedtaksperiodeBegrensetAv6G(1, 1.mai(2020), 31.mai(2020))
        utbetaltVedtaksperiodeBegrensetAv6G(2, 5.juli(2020), 31.juli(2020))
        håndterGrunnbeløpsregulering(
            gyldighetsdato = GYLDIGHETSDATO_2020_GRUNNBELØP,
            fagsystemId = inspektør.utbetalinger.first().arbeidsgiverOppdrag().fagsystemId()
        )
        utbetaltForlengetVedtaksperiodeBegrensetAv6G(3, 1.august(2020), 31.august(2020))
        inspektør.utbetalinger.also { utbetalinger ->
            assertEquals(4, utbetalinger.size)
            assertEquals(inspektør.vedtaksperiodeutbetaling(1.vedtaksperiode), utbetalinger[0])
            assertEquals(inspektør.vedtaksperiodeutbetaling(2.vedtaksperiode), utbetalinger[1])
            assertEquals(inspektør.vedtaksperiodeutbetaling(3.vedtaksperiode), utbetalinger[3])

            assertEquals(utbetalinger[0].arbeidsgiverOppdrag().fagsystemId(), utbetalinger[2].arbeidsgiverOppdrag().fagsystemId())
            assertTrue(utbetalinger[2].erEtterutbetaling())
            assertNotEquals(utbetalinger[0].arbeidsgiverOppdrag().fagsystemId(), utbetalinger[1].arbeidsgiverOppdrag().fagsystemId())
            assertEquals(utbetalinger[1].arbeidsgiverOppdrag().fagsystemId(), utbetalinger[3].arbeidsgiverOppdrag().fagsystemId())
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
        assertTrue(etterutbetaling.erEtterutbetaling())
    }

    @Test
    fun `periode som skal ha 2019-sats får ikke 2020-sats`() {
        val fom = GYLDIGHETSDATO_2020_GRUNNBELØP.minusDays(1)
        val tom = fom.plusDays(31)
        utbetaltVedtaksperiodeBegrensetAv6G(1, fom, tom)
        håndterGrunnbeløpsregulering(gyldighetsdato = GYLDIGHETSDATO_2020_GRUNNBELØP)
        assertEquals(1, inspektør.utbetalinger.size)
        val etterutbetaling = inspektør.utbetalinger.last()
        assertFalse(etterutbetaling.erEtterutbetaling())
    }

    @Test
    fun `periode med nytt grunnbeløp trengs ikke justering`() {
        val fom = GYLDIGHETSDATO_2020_GRUNNBELØP
        val tom = fom.plusDays(31)
        utbetaltVedtaksperiodeBegrensetAv6G(1, fom, tom)
        håndterGrunnbeløpsregulering(gyldighetsdato = GYLDIGHETSDATO_2020_GRUNNBELØP)
        assertEquals(1, inspektør.utbetalinger.size)
        val etterutbetaling = inspektør.utbetalinger.last()
        assertFalse(etterutbetaling.erEtterutbetaling())
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
        arbeidsgiverperiode: List<Periode> = listOf(Periode(
            fom,
            fom.plusDays(15)
        )),
        førsteFraværsdag: LocalDate = fom
    ) {
        håndterSykmelding(Sykmeldingsperiode(fom, tom, 100))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(fom, tom, gradFraSykmelding = 100), sendtTilNav = tom)
        håndterInntektsmeldingMedValidering(
            vedtaksperiodeIndeks.vedtaksperiode,
            arbeidsgiverperiode,
            førsteFraværsdag = førsteFraværsdag,
            refusjon = Triple(null, HØY_INNTEKT, emptyList())
        )
        håndterVilkårsgrunnlag(vedtaksperiodeIndeks.vedtaksperiode, HØY_INNTEKT)
        håndterYtelser(vedtaksperiodeIndeks.vedtaksperiode) // No history
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
        førsteFraværsdag: LocalDate = fom
    ) {
        ubetaltVedtaksperiodeBegrensetAv6G(vedtaksperiodeIndeks, fom, tom, arbeidsgiverperiode, førsteFraværsdag)
        håndterUtbetalt(vedtaksperiodeIndeks.vedtaksperiode, AKSEPTERT)
    }

    private fun utbetaltForlengetVedtaksperiodeBegrensetAv6G(
        vedtaksperiodeIndeks: Int,
        fom: LocalDate,
        tom: LocalDate
    ) {
        håndterSykmelding(Sykmeldingsperiode(fom, tom, 100))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(fom, tom, gradFraSykmelding = 100), sendtTilNav = tom)
        håndterYtelser(vedtaksperiodeIndeks.vedtaksperiode) // No history
        håndterSimulering(vedtaksperiodeIndeks.vedtaksperiode)
        håndterUtbetalingsgodkjenning(vedtaksperiodeIndeks.vedtaksperiode, true)
        håndterUtbetalt(vedtaksperiodeIndeks.vedtaksperiode, AKSEPTERT)
    }
}
