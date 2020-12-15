package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.Utbetalingshistorikk.Infotrygdperiode.RefusjonTilArbeidsgiver
import no.nav.helse.hendelser.Utbetalingshistorikk.Inntektsopplysning
import no.nav.helse.hendelser.til
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class BrukerutbetalingerTest : AbstractEndToEndTest() {

    @Test
    fun `maksdato blir riktig når person har brukerutbetaling på samme arbeidsgiver`() {
        val historikk = listOf(
            RefusjonTilArbeidsgiver(17.januar, 17.mai, INNTEKT, 100.prosent, ORGNUMMER),
            Utbetalingshistorikk.Infotrygdperiode.Utbetaling(18.mai, 30.mai, INNTEKT, 100.prosent, ORGNUMMER)
        )
        val inntektsopplysning = listOf(
            Inntektsopplysning(17.januar, INNTEKT, ORGNUMMER, true),
            Inntektsopplysning(18.mai, INNTEKT, ORGNUMMER, false)
        )

        håndterSykmelding(Sykmeldingsperiode(1.juni, 30.juni, 100.prosent))
        håndterSøknad(Sykdom(1.juni, 30.juni, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.juni)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterUtbetalingshistorikk(1.vedtaksperiode, utbetalinger = historikk.toTypedArray(), inntektshistorikk = inntektsopplysning)
        håndterYtelser(1.vedtaksperiode, utbetalinger = historikk.toTypedArray(), inntektshistorikk = inntektsopplysning)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        assertEquals(31.desember, inspektør.maksdato(1.vedtaksperiode))
    }

    @Test
    fun `maksdato blir riktig når person har gammel brukerutbetaling som selvstendig næringsdrivende`() {
        val historikk = listOf(
            RefusjonTilArbeidsgiver(17.januar, 30.mai, INNTEKT, 100.prosent, ORGNUMMER),
            RefusjonTilArbeidsgiver(1.september(2017), 1.september(2017), INNTEKT, 100.prosent, ORGNUMMER),
            Utbetalingshistorikk.Infotrygdperiode.Utbetaling(18.mai(2017), 30.mai(2017), INNTEKT, 100.prosent, "0")
        )
        val inntektsopplysning = listOf(
            Inntektsopplysning(17.januar, INNTEKT, ORGNUMMER, true),
            Inntektsopplysning(1.september(2017), INNTEKT, ORGNUMMER, true),
            Inntektsopplysning(18.mai(2017), INNTEKT, "0", false)
        )

        håndterSykmelding(Sykmeldingsperiode(1.juni, 30.juni, 100.prosent))
        håndterSøknad(Sykdom(1.juni, 30.juni, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.juni)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterUtbetalingshistorikk(1.vedtaksperiode, utbetalinger = historikk.toTypedArray(), inntektshistorikk = inntektsopplysning)
        håndterYtelser(1.vedtaksperiode, utbetalinger = historikk.toTypedArray(), inntektshistorikk = inntektsopplysning)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        assertEquals(17.desember, inspektør.maksdato(1.vedtaksperiode))
    }
}
