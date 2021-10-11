package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class PingPongTest : AbstractEndToEndTest() {

    @Test
    fun `Forlengelser av infotrygd overgang har samme maksdato som forrige`() {
        val historikk1 = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 20.november(2019),  29.mai(2020), 100.prosent, 1145.daglig)
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER, 20.november(2019), 1000.daglig, true))
        håndterSykmelding(Sykmeldingsperiode(30.mai(2020), 19.juni(2020), 100.prosent))
        håndterSøknad(Sykdom(30.mai(2020), 19.juni(2020), 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk1, inntektshistorikk = inntekter)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)

        håndterSykmelding(Sykmeldingsperiode(22.juni(2020), 9.juli(2020), 100.prosent))
        håndterSøknad(
            Sykdom(22.juni(2020), 9.juli(2020), 100.prosent)
        )
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterPåminnelse(2.vedtaksperiode, AVVENTER_GODKJENNING, LocalDateTime.now().minusDays(110))

        val historikk2 = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 22.juni(2020),  17.august(2020), 100.prosent, 1145.daglig)
        val inntekter2 = listOf(
            Inntektsopplysning(ORGNUMMER, 20.november(2019), 1000.daglig, true),
            Inntektsopplysning(ORGNUMMER, 22.juni(2020), 1000.daglig, true)
        )
        håndterSykmelding(Sykmeldingsperiode(18.august(2020), 2.september(2020), 100.prosent))
        håndterSøknad(Sykdom(18.august(2020), 2.september(2020), 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode, historikk1, historikk2, inntektshistorikk = inntekter2)
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode, true)
        håndterUtbetalt(3.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        assertFalse(inspektør.periodeErForkastet(1.vedtaksperiode))
        assertEquals(30.oktober(2020), inspektør.sisteMaksdato(3.vedtaksperiode))
        assertEquals(inspektør.sisteMaksdato(1.vedtaksperiode), inspektør.sisteMaksdato(3.vedtaksperiode))
    }

    @Test
    fun `riktig skjæringstidspunkt ved spleis - infotrygd - spleis`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        val historie = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.februar,  28.februar, 100.prosent, 1000.daglig)
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER, 1.februar, INNTEKT, true))
        håndterUtbetalingshistorikk(2.vedtaksperiode, historie, inntektshistorikk = inntekter)
        håndterYtelser(2.vedtaksperiode)
        assertEquals(1.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
    }

    @Test
    fun `kort periode - infotrygd - spleis --- inntekt kommer fra infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(1.januar, 16.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(17.januar, 26.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(17.januar, 27.januar, 100.prosent))

        håndterInntektsmelding(listOf(1.januar til 16.januar))

        håndterSykmelding(Sykmeldingsperiode(28.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(28.januar, 31.januar, 100.prosent))

        val historie = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar,  27.januar, 100.prosent, 1000.daglig)
        val inntekt = listOf(Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT - 100.månedlig, true))

        håndterUtbetalingshistorikk(3.vedtaksperiode, historie, inntektshistorikk = inntekt)
        håndterYtelser(3.vedtaksperiode)
        assertEquals(1.januar, inspektør.skjæringstidspunkt(3.vedtaksperiode))
        assertEquals(INNTEKT - 100.månedlig, inspektør.vilkårsgrunnlag(3.vedtaksperiode)?.grunnlagForSykepengegrunnlag())
    }

    @Test
    fun `spleis - infotrygd - spleis --- inntekt kommer fra første periode`() {
        nyttVedtak(20.desember(2017), 16.januar)
        håndterSykmelding(Sykmeldingsperiode(17.januar, 26.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(17.januar, 27.januar, 100.prosent))

        håndterInntektsmelding(listOf(20.desember(2017) til 5.januar))

        håndterSykmelding(Sykmeldingsperiode(28.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(28.januar, 31.januar, 100.prosent))

        val historie = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar,  27.januar, 100.prosent, 1000.daglig)
        val inntekt = listOf(Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT - 100.månedlig, true))

        håndterUtbetalingshistorikk(3.vedtaksperiode, historie, inntektshistorikk = inntekt)
        håndterYtelser(3.vedtaksperiode)
        assertEquals(20.desember(2017), inspektør.skjæringstidspunkt(3.vedtaksperiode))
        assertEquals(INNTEKT, inspektør.vilkårsgrunnlag(3.vedtaksperiode)?.grunnlagForSykepengegrunnlag())
    }
}
