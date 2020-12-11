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
    fun `maksdato blir riktig når person har brukerutbetaling`() {
        val historikk = listOf(
            RefusjonTilArbeidsgiver(16.januar, 30.april, INNTEKT, 100.prosent, ORGNUMMER),
            Utbetalingshistorikk.Infotrygdperiode.Utbetaling(1.mai, 30.mai, INNTEKT, 100.prosent, UNG_PERSON_FNR_2018)
        )
        val inntektsopplysning = listOf(
            Inntektsopplysning(1.januar, INNTEKT, ORGNUMMER, true)
        )

        håndterSykmelding(Sykmeldingsperiode(1.juni, 30.juni, 100.prosent))
        håndterSøknad(Sykdom(1.juni, 30.juni, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 15.januar), førsteFraværsdag = 1.juni)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterUtbetalingshistorikk(1.vedtaksperiode, utbetalinger = historikk.toTypedArray(), inntektshistorikk = inntektsopplysning)
        håndterYtelser(1.vedtaksperiode, utbetalinger = historikk.toTypedArray(), inntektshistorikk = inntektsopplysning)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        assertEquals(28.desember, inspektør.maksdato(1.vedtaksperiode))
    }
}
