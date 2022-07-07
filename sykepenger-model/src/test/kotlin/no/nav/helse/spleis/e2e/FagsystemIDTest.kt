package no.nav.helse.spleis.e2e

import no.nav.helse.april
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class FagsystemIDTest : AbstractEndToEndTest() {

    /*
       starter i Spleis.
       [spleis][infotrygd][spleis]
          ^ ny fagsystemID  ^ ny fagsystemID
    */
    @Test
    fun `får ny fagsystemId når Infotrygdperiode foran`() {
        håndterSykmelding(Sykmeldingsperiode(1.april, 30.april, 100.prosent))
        håndterSøknad(Sykdom(1.april, 30.april, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(1.april til 16.april))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.april(2017) til 1.mars(2018) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        val historie1 = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.mai,  29.mai, 100.prosent, 1000.daglig)
        )

        håndterSykmelding(Sykmeldingsperiode(30.mai, 30.juni, 100.prosent))
        håndterSøknad(Sykdom(30.mai, 30.juni, 100.prosent))
        håndterUtbetalingshistorikk(
            2.vedtaksperiode,
            *historie1.toTypedArray(),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.mai, INNTEKT, true))
        )
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        assertEquals(2, inspektør.utbetalinger.size)
        val første = inspektør.utbetalinger.first().inspektør.arbeidsgiverOppdrag
        val siste = inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag
        assertNotEquals(første.fagsystemId(), siste.fagsystemId())
        første.linjerUtenOpphør().also { linjer ->
            assertEquals(1, linjer.size)
            assertEquals(17.april, linjer.first().fom)
            assertEquals(30.april, linjer.first().tom)
        }
        siste.linjerUtenOpphør().also { linjer ->
            assertEquals(1, linjer.size)
            assertEquals(30.mai, linjer.first().fom)
            assertEquals(29.juni, linjer.first().tom)
        }
    }

    /*
       starter i Spleis.
       [spleis][infotrygd][     spleis     ][spleis]
          ^ ny fagsystemID  ^ ny fagsystemID  ^ samme fagsystemID
    */
    @Test
    fun `bruker samme fagsystemID ved forlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(1.april, 30.april, 100.prosent))
        håndterSøknad(Sykdom(1.april, 30.april, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(1.april til 16.april))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.april(2017) til 1.mars(2018) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        val historie1 = listOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.mai,  29.mai, 100.prosent, 1000.daglig)
        )
        håndterSykmelding(Sykmeldingsperiode(30.mai, 30.juni, 100.prosent))
        håndterSøknad(Sykdom(30.mai, 30.juni, 100.prosent))
        håndterUtbetalingshistorikk(
            2.vedtaksperiode,
            *historie1.toTypedArray(),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.mai, INNTEKT, true))
        )
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(1.juli, 31.juli, 100.prosent))
        håndterSøknad(Sykdom(1.juli, 31.juli, 100.prosent))
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        assertEquals(3, inspektør.utbetalinger.size)
        val første = inspektør.utbetalinger.first().inspektør.arbeidsgiverOppdrag
        val andre = inspektør.utbetalinger[1].inspektør.arbeidsgiverOppdrag
        val siste = inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag

        assertEquals(16.april, første.inspektør.sisteArbeidsgiverdag)
        assertEquals(29.mai, andre.inspektør.sisteArbeidsgiverdag)
        assertEquals(29.mai, siste.inspektør.sisteArbeidsgiverdag)
        assertNotEquals(første.fagsystemId(), siste.fagsystemId())
        assertEquals(andre.fagsystemId(), siste.fagsystemId())
        siste.linjerUtenOpphør().also { linjer ->
            assertEquals(1, linjer.size)
            assertEquals(30.mai, linjer.first().fom)
            assertEquals(31.juli, linjer.first().tom)
        }
    }

}
