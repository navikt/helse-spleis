package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.person.TilstandType
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.utbetalte
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class UtbetalingOgAnnulleringTest : AbstractEndToEndTest() {

    @Disabled("Slik skal det virke etter at annullering trigger replay")
    @Test
    fun `annullerer første periode i en sammenhengende utbetaling`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(27.januar, 15.februar, 100))
        håndterSøknadMedValidering(2.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(27.januar, 15.februar, 100))
        håndterYtelser(2.vedtaksperiode)   // No history
        håndterSimulering(2.vedtaksperiode)
        val fagsystemId = inspektør.utbetalinger.last().arbeidsgiverOppdrag().fagsystemId()
        håndterKansellerUtbetaling(fagsystemId = fagsystemId)

        assertEquals(26.januar, observatør.utbetaltEventer[0].tom)
        assertEquals(26.januar, observatør.annulleringer[0].utbetalingslinjer.last().tom)
    }

    @Disabled("Slik skal det virke etter at annullering trigger replay")
    @Test
    fun `annullerer første periode i en ikke-sammenhengende utbetaling`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(30.januar, 15.februar, 100))
        håndterSøknadMedValidering(2.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(30.januar, 15.februar, 100))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(3.januar, 18.januar)), førsteFraværsdag = 30.januar)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)   // No history
        håndterSimulering(2.vedtaksperiode)
        val fagsystemId = inspektør.utbetalinger.utbetalte().last().arbeidsgiverOppdrag().fagsystemId()
        håndterKansellerUtbetaling(fagsystemId = fagsystemId)

        assertEquals(26.januar, observatør.annulleringer[0].utbetalingslinjer.last().tom)
    }

    @Disabled("Slik skal det virke etter at annullering trigger replay")
    @Test
    fun `annullerer første periode i en ikke-sammenhengende utbetaling med mer enn 16 dager gap`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(15.februar, 15.mars, 100))
        håndterSøknadMedValidering(2.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(15.februar, 15.mars, 100))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(15.februar, 2.mars)), førsteFraværsdag = 15.februar)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)   // No history
        håndterSimulering(2.vedtaksperiode)
        val fagsystemId = inspektør.utbetalinger.utbetalte().last().arbeidsgiverOppdrag().fagsystemId()
        håndterKansellerUtbetaling(fagsystemId = fagsystemId)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        assertEquals(26.januar, observatør.annulleringer[0].utbetalingslinjer.last().tom)
        assertEquals(15.mars, observatør.utbetaltEventer.last().tom)
        assertEquals(1, observatør.utbetaltEventer.last().oppdrag.first().utbetalingslinjer.size)
        assertEquals(0, observatør.utbetaltEventer.last().oppdrag.last().utbetalingslinjer.size)
    }

    @Test
    fun `annullerer første periode før andre periode starter i en ikke-sammenhengende utbetaling med mer enn 16 dager gap`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)
        val fagsystemId = inspektør.utbetalinger.utbetalte().last().arbeidsgiverOppdrag().fagsystemId()
        håndterKansellerUtbetaling(fagsystemId = fagsystemId)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(15.februar, 15.mars, 100))
        håndterSøknadMedValidering(2.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(15.februar, 15.mars, 100))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(15.februar, 2.mars)), førsteFraværsdag = 15.februar)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)   // No history
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        assertEquals(26.januar, observatør.annulleringer[0].utbetalingslinjer.last().tom)
        assertEquals(15.mars, observatør.utbetaltEventer.last().tom)
        assertEquals(1, observatør.utbetaltEventer.last().oppdrag.first().utbetalingslinjer.size)
        assertEquals(0, observatør.utbetaltEventer.last().oppdrag.last().utbetalingslinjer.size)
    }

    @Test
    fun `annullering kaster alle etterfølgende perioder`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(15.februar, 15.mars, 100))
        håndterSøknadMedValidering(2.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(15.februar, 15.mars, 100))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(15.februar, 2.mars)), førsteFraværsdag = 15.februar)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)   // No history
        håndterSimulering(2.vedtaksperiode)

        val fagsystemIdFørsteVedtaksperiode = inspektør.utbetalinger.utbetalte().last().arbeidsgiverOppdrag().fagsystemId()
        håndterKansellerUtbetaling(fagsystemId = fagsystemIdFørsteVedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        assertForkastetPeriodeTilstander(1.vedtaksperiode,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP,
            TilstandType.AVVENTER_SØKNAD_FERDIG_GAP,
            TilstandType.AVVENTER_VILKÅRSPRØVING_GAP,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.TIL_UTBETALING,
            TilstandType.AVSLUTTET,
            TilstandType.TIL_ANNULLERING,
            TilstandType.TIL_INFOTRYGD
        )

        assertForkastetPeriodeTilstander(2.vedtaksperiode,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP,
            TilstandType.AVVENTER_GAP,
            TilstandType.AVVENTER_VILKÅRSPRØVING_GAP,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.TIL_INFOTRYGD
        )


    }
}
