package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver
import no.nav.helse.person.TilstandType
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.testhelpers.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class PingPongWarningTest : AbstractEndToEndTest() {

    @Test
    fun `Warning ved ping-pong hvis historikken er nyere enn seks måneder med opphav i Spleis`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterSøknad(Sykdom(1.januar, 31.januar, gradFraSykmelding = 100), sendtTilNav = 18.februar)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100))
        håndterSøknad(Sykdom(1.mars, 31.mars, gradFraSykmelding = 100), sendtTilNav = 1.april)
        val historikk = RefusjonTilArbeidsgiver(1.februar, 28.februar, 1337, 100, ORGNUMMER)
        håndterUtbetalingshistorikk(2.vedtaksperiode, historikk)
        håndterYtelser(2.vedtaksperiode, historikk)

        assertTrue(inspektør.personLogg.warn().toString().contains("Perioden forlenger en behandling i ny løsning, og har historikk i Infotrygd også: Undersøk at antall dager igjen er beregnet riktig.")) {
            inspektør.personLogg.toString()
        }
    }

    @Test
    fun `Warning ved ping-pong hvis historikken er nyere enn seks måneder med opphav i Infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.februar, 16.februar)))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode, RefusjonTilArbeidsgiver(1.januar, 31.januar, 1337, 100, ORGNUMMER))
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(1.april, 30.april, 100))
        håndterSøknad(Sykdom(1.april, 30.april, 100))
        håndterUtbetalingshistorikk(2.vedtaksperiode,
            RefusjonTilArbeidsgiver(1.januar, 31.januar, 1337, 100, ORGNUMMER),
            RefusjonTilArbeidsgiver(1.mars, 31.mars, 1337, 100, ORGNUMMER)
        )
        håndterYtelser(2.vedtaksperiode,
            RefusjonTilArbeidsgiver(1.januar, 31.januar, 1337, 100, ORGNUMMER),
            RefusjonTilArbeidsgiver(1.mars, 31.mars, 1337, 100, ORGNUMMER)
        )

        assertTrue(inspektør.personLogg.warn().toString().contains("Perioden forlenger en behandling i Infotrygd, og har historikk i ny løsning også: Undersøk at antall dager igjen er beregnet riktig.")) {
            inspektør.personLogg.toString()
        }
    }

    @Test
    fun `Ikke warning ved ping-pong hvor forkastet periode ligger mer enn seks måneder tilbake i tid`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 17.januar, 100))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterSøknad(Sykdom(1.januar, 17.januar, 100))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)

        håndterSykmelding(Sykmeldingsperiode(1.juli, 17.juli, 100))
        håndterSøknad(
            Sykdom(1.juli, 17.juli, gradFraSykmelding = 100),
            sendtTilNav = 18.juli,
            harAndreInntektskilder = true
        )

        håndterSykmelding(Sykmeldingsperiode(18.juli, 31.juli, 100))
        håndterSøknad(Sykdom(18.juli, 31.juli, 100))
        val historikk = RefusjonTilArbeidsgiver(1.juli, 17.juli, 1000, 100, ORGNUMMER)
        håndterUtbetalingshistorikk(3.vedtaksperiode, historikk)
        håndterYtelser(3.vedtaksperiode, historikk)
        håndterSimulering(3.vedtaksperiode)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_GODKJENNING)

        assertTrue(inspektør.personLogg.warn().isEmpty())
    }

    @Test
    fun `Ikke warning for ping-pong hvis tidligere periode ikke gikk til avsluttet`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 17.januar, 100))
        håndterSøknad(
            Sykdom(1.januar, 17.januar, gradFraSykmelding = 100),
            sendtTilNav = 18.januar,
            harAndreInntektskilder = true // <- til infotrygd
        )

        håndterSykmelding(Sykmeldingsperiode(18.januar, 31.januar, 100))
        håndterSøknad(Sykdom(18.januar, 31.januar, gradFraSykmelding = 100), sendtTilNav = 1.februar)
        håndterUtbetalingshistorikk(
            2.vedtaksperiode,
            RefusjonTilArbeidsgiver(1.januar, 17.januar, 1337, 100, ORGNUMMER)
        )
        håndterYtelser(2.vedtaksperiode, RefusjonTilArbeidsgiver(1.januar, 17.januar, 1337, 100, ORGNUMMER))
        håndterSimulering(2.vedtaksperiode)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING)

        assertTrue(inspektør.personLogg.warn().isEmpty())
    }

    @Test
    fun `Ikke ping-pong-warning hvis periode i ny løsning ikke hadde utbetaling`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 13.januar, 100))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(1.januar, 13.januar, 100))
        håndterInntektsmelding(listOf(1.januar til 13.januar), førsteFraværsdag = 1.januar)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        assertSisteTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING)

        håndterSykmelding(Sykmeldingsperiode(20.februar, 28.februar, 100))
        håndterSøknad(
            Sykdom(20.februar, 28.februar, gradFraSykmelding = 100),
            sendtTilNav = 1.mars,
            harAndreInntektskilder = true // <- til infotrygd
        )

        håndterSykmelding(Sykmeldingsperiode(1.mars, 21.mars, 100))
        håndterSøknad(Sykdom(1.mars, 21.mars, gradFraSykmelding = 100), sendtTilNav = 1.april)
        håndterInntektsmelding(listOf(1.mars til 16.mars), førsteFraværsdag = 1.mars)
        håndterVilkårsgrunnlag(3.vedtaksperiode, INNTEKT)
        håndterYtelser(3.vedtaksperiode, RefusjonTilArbeidsgiver(20.februar, 28.februar, 1337, 100, ORGNUMMER))
        håndterSimulering(3.vedtaksperiode)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_GODKJENNING)
        assertTrue(inspektør.personLogg.warn().isEmpty())
    }

}
