package no.nav.helse.bugs_showstoppers

import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.*
import no.nav.helse.hendelser.SøknadArbeidsgiver
import no.nav.helse.hendelser.til
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.*
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.person.infotrygdhistorikk.Utbetalingsperiode
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ManglendeVilkårsgrunnlagTest : AbstractEndToEndTest() {

    @Test
    fun `inntektsmelding avslutter to korte perioder og flytter nav-perioden uten å utføre vilkårsprøving`() {
        håndterSykmelding(Sykmeldingsperiode(9.januar, 15.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(9.januar, 15.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(19.januar, 26.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(19.januar, 26.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(29.januar, 2.februar, 100.prosent))
        håndterSøknad(Sykdom(29.januar, 2.februar, 100.prosent))
        // inntektsmeldingen lukker de to korte periodene og gjør samtidig at
        // nav-perioden går fra AvventerInntektsmeldingUferdigForlengelse til AvventerUferdigForlengelse.
        // Når Inntektsmeldingen er håndtert sendes GjenopptaBehandling ut, som medfører at
        // NAV-perioden går videre til AvventerHistorikk uten å gjøre vilkårsvurdering først
        håndterInntektsmelding(
            listOf(
                9.januar til 15.januar,
                19.januar til 26.januar,
                29.januar til 29.januar
            ), 29.januar
        )
        håndterYtelser(3.vedtaksperiode)
        håndterVilkårsgrunnlag(3.vedtaksperiode)
        håndterYtelser(3.vedtaksperiode)

        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE, AVVENTER_HISTORIKK, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
    }

    @Test
    fun `arbeidsgiverperiode med brudd i helg`() {
        håndterSykmelding(Sykmeldingsperiode(4.januar, 5.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(4.januar, 5.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(8.januar, 12.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(8.januar, 12.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(13.januar, 19.januar, 100.prosent))
        håndterSøknad(Sykdom(13.januar, 19.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(20.januar, 1.februar, 100.prosent))
        håndterSøknad(Sykdom(20.januar, 1.februar, 100.prosent))

        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE)
        assertTilstander(4.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE)

        // 6. og 7. januar blir FriskHelg og medfører brudd i arbeidsgiverperioden
        // og dermed ble også skjæringstidspunktet forskjøvet til 8. januar
        håndterInntektsmelding(
            listOf(
                1.januar til 3.januar,
                4.januar til 5.januar,
                // 6. og 7. januar er helg
                8.januar til 12.januar,
                13.januar til 18.januar
            ), 1.januar
        )

        håndterYtelser(3.vedtaksperiode)

        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE, AVVENTER_HISTORIKK, AVVENTER_VILKÅRSPRØVING)
        assertTilstander(4.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE)

        assertTrue(inspektør.etterspurteBehov(1.vedtaksperiode).isEmpty())
        assertTrue(inspektør.etterspurteBehov(2.vedtaksperiode).isEmpty())
        assertTrue(inspektør.etterspurteBehov(3.vedtaksperiode).map { it.type }.containsAll(listOf(InntekterForSammenligningsgrunnlag, Opptjening, Medlemskap)))
        assertTrue(inspektør.etterspurteBehov(4.vedtaksperiode).isEmpty())
    }

    @Test
    fun `inntektsmelding drar periode tilbake og lager tilstøtende`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(1.januar, 5.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(26.januar, 2.februar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(26.januar, 2.februar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(5.februar, 21.februar, 100.prosent))
        håndterSøknad(Sykdom(5.februar, 21.februar, 100.prosent))

        // inntektsmelding inneholder en ukjent dag — 8. januar — som vi ikke
        // har registrert før i forbindelse med verken søknad eller sykmelding
        // Dette medfører at perioden 26. januar - 2. februar "dras tilbake"
        // til 6. januar, siden 8. januar er en mandag og 5. januar (forrige agp-innslag) er en fredag
        // så regnes lørdag + søndag som del av arbeidsgiverperioden også.
        // Dermed ble perioden 6. januar - 2. februar regnet som tilstøtende til 1.-5. januar, selv om
        // de to har forskjellige skjæringstidspunkt.
        håndterInntektsmelding(
            listOf(
                1.januar til 5.januar,
                8.januar til 8.januar,
                24.januar til 2.februar
            ), 24.januar
        )

        håndterYtelser(3.vedtaksperiode)

        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE, AVVENTER_HISTORIKK, AVVENTER_VILKÅRSPRØVING)

        assertTrue(inspektør.etterspurteBehov(1.vedtaksperiode).isEmpty())
        assertTrue(inspektør.etterspurteBehov(2.vedtaksperiode).isEmpty())
        assertTrue(inspektør.etterspurteBehov(3.vedtaksperiode).map { it.type }.containsAll(listOf(InntekterForSammenligningsgrunnlag, Opptjening, Medlemskap)))
    }

    @Test
    fun `periode etter en periode med ferie - opphav i Infotrygd`() {
        val historikk = Utbetalingsperiode(ORGNUMMER, 1.desember(2017),  31.desember(2017), 100.prosent, INNTEKT)
        val inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 1.desember(2017), INNTEKT, true)
        )
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntektshistorikk)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Ferie(1.februar, 28.februar))
        håndterYtelser(2.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterYtelser(3.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVVENTER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
    }

    @Test
    fun `periode etter en periode med permisjon (gir warning) - opphav i Infotrygd`() {
        val historikk = Utbetalingsperiode(ORGNUMMER, 1.desember(2017),  31.desember(2017), 100.prosent, INNTEKT)
        val inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 1.desember(2017), INNTEKT, true)
        )
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntektshistorikk)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Permisjon(1.februar, 28.februar))
        håndterYtelser(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterYtelser(3.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
    }
}
