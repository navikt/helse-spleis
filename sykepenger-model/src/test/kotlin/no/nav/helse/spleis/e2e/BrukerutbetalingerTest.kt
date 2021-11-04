package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.hendelser.til
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.person.infotrygdhistorikk.PersonUtbetalingsperiode
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class BrukerutbetalingerTest : AbstractEndToEndTest() {

    @Test
    fun `maksdato blir riktig når person har brukerutbetaling på samme arbeidsgiver`() {
        val historikk = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar,  17.mai, 100.prosent, INNTEKT),
            PersonUtbetalingsperiode(ORGNUMMER, 18.mai,  30.mai, 100.prosent, INNTEKT)
        )
        val inntektsopplysning = listOf(
            Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true),
            Inntektsopplysning(ORGNUMMER, 18.mai, INNTEKT, false)
        )

        håndterSykmelding(Sykmeldingsperiode(1.juni, 30.juni, 100.prosent))
        håndterSøknad(Sykdom(1.juni, 30.juni, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.juni)
        håndterYtelser(1.vedtaksperiode, *historikk.toTypedArray(), inntektshistorikk = inntektsopplysning)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.juni(2017) til 1.mai(2018) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        assertEquals(31.desember, inspektør.sisteMaksdato(1.vedtaksperiode))
    }

    @Test
    fun `maksdato blir riktig når person har gammel brukerutbetaling som selvstendig næringsdrivende`() {
        val historikk = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar,  30.mai, 100.prosent, INNTEKT),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.september(2017),  1.september(2017), 100.prosent, INNTEKT),
            PersonUtbetalingsperiode("0", 18.mai(2017),  30.mai(2017), 100.prosent, INNTEKT)
        )
        val inntektsopplysning = listOf(
            Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true),
            Inntektsopplysning(ORGNUMMER, 1.september(2017), INNTEKT, true),
            Inntektsopplysning("0", 18.mai(2017), INNTEKT, false)
        )

        håndterSykmelding(Sykmeldingsperiode(1.juni, 30.juni, 100.prosent))
        håndterSøknad(Sykdom(1.juni, 30.juni, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.juni)
        håndterYtelser(1.vedtaksperiode,  *historikk.toTypedArray(), inntektshistorikk = inntektsopplysning, besvart = LocalDateTime.now().minusHours(24))
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.juni(2017) til 1.mai(2018) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode, *historikk.toTypedArray(), inntektshistorikk = inntektsopplysning)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        assertEquals(17.desember, inspektør.sisteMaksdato(1.vedtaksperiode))
    }
}
