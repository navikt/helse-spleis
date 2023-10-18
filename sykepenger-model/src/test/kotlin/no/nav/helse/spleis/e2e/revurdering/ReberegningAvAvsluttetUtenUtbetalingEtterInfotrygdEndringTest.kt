package no.nav.helse.spleis.e2e.revurdering

import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import no.nav.helse.assertForventetFeil
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IT_14
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IT_3
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IT_38
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertForkastetPeriodeTilstander
import no.nav.helse.spleis.e2e.assertFunksjonellFeil
import no.nav.helse.spleis.e2e.assertInfo
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikkEtterInfotrygdendring
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

@EnableToggle(Toggle.OmgjøringVedInfotrygdendring::class)
internal class ReberegningAvAvsluttetUtenUtbetalingEtterInfotrygdEndringTest : AbstractEndToEndTest() {

    @Test
    fun `AUU med infotrygdperiode rett før skal omgjøres`() {
        nyPeriode(5.januar til 20.januar)
        nullstillTilstandsendringer()
        håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 4.januar, 100.prosent, INNTEKT))
        assertVarsel(RV_IT_14, 1.vedtaksperiode.filter())
        assertInfo("AUU vil omgjøres, men avventer å starte omgjøring ettersom vi vil ende opp med å spørre om inntektsmelding", 1.vedtaksperiode.filter())
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `AUU med utbetalt forlengelse med infotrygdperiode rett før`() {
        nyPeriode(5.januar til 20.januar)
        nyttVedtak(21.januar, 31.januar, arbeidsgiverperiode = listOf(5.januar til 20.januar))
        nullstillTilstandsendringer()
        håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 4.januar, 100.prosent, INNTEKT))
        assertVarsel(RV_IT_14, 1.vedtaksperiode.filter())
        assertVarsel(RV_IT_38, 1.vedtaksperiode.filter())
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK)
    }

    @Test
    fun `helt overlappende infotrygd`() { // TODO: Ny test hvor det bare er delvis overlapp
        nyPeriode(5.januar til 20.januar)
        nullstillTilstandsendringer()
        håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 5.januar, 20.januar, 100.prosent, INNTEKT))

        assertForventetFeil(
            forklaring = "Når _alle_ dagene er utbetalt i Infotrygd",
            nå = {
                assertInfo("AUU vil omgjøres, men avventer å starte omgjøring ettersom vi vil ende opp med å spørre om inntektsmelding", 1.vedtaksperiode.filter())
                assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            },
            ønsket = {
                assertFunksjonellFeil(RV_IT_3, 1.vedtaksperiode.filter())
                assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, TIL_INFOTRYGD)
            }
        )
    }

    @Test
    fun `overlappende infotrygd med utbetalt forlengelse`() {
        nyPeriode(5.januar til 20.januar)
        nyttVedtak(21.januar, 31.januar, arbeidsgiverperiode = listOf(5.januar til 20.januar))
        nullstillTilstandsendringer()
        håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 5.januar, 18.januar, 100.prosent, INNTEKT))
        assertVarsel(RV_IT_3, 1.vedtaksperiode.filter())
        assertVarsel(RV_IT_38, 1.vedtaksperiode.filter())
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
    }

    @Test
    fun `ny søknad medfører infotrygdhistorikk som overlapper helt med gammelt`() {
        nyPeriode(5.januar til 20.januar)
        nullstillTilstandsendringer()
        håndterSøknad(Sykdom(1.januar(2023), 31.januar(2023), 100.prosent))
        håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 5.januar, 20.januar, 100.prosent, INNTEKT))

        assertForventetFeil(
            forklaring = "",
            nå = {
                assertInfo("AUU vil omgjøres, men avventer å starte omgjøring ettersom vi vil ende opp med å spørre om inntektsmelding", 1.vedtaksperiode.filter())
                assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
                assertTilstander(2.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING)
            },
            ønsket = {
                assertFunksjonellFeil(RV_IT_3, 1.vedtaksperiode.filter())
                assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, TIL_INFOTRYGD)
                assertTilstander(2.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING)
            }
        )
    }

    @Test
    fun `ny søknad medfører infotrygdhistorikk som overlapper med gammelt med utbetalt forlengelse`() {
        nyPeriode(5.januar til 20.januar)
        nyttVedtak(21.januar, 31.januar, arbeidsgiverperiode = listOf(5.januar til 20.januar))
        nullstillTilstandsendringer()
        håndterSøknad(Sykdom(1.januar(2023), 31.januar(2023), 100.prosent))
        håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 5.januar, 20.januar, 100.prosent, INNTEKT))

        assertVarsel(RV_IT_3, 1.vedtaksperiode.filter())
        assertVarsel(RV_IT_38, 1.vedtaksperiode.filter())
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertTilstander(3.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING)
    }

    @Test
    fun `ny søknad medfører infotrygdhistorikk som som er like før gammelt med utbetalt forlengelse`() {
        nyPeriode(5.januar til 20.januar)
        nyttVedtak(21.januar, 31.januar, arbeidsgiverperiode = listOf(5.januar til 20.januar))
        nullstillTilstandsendringer()
        håndterSøknad(Sykdom(1.januar(2023), 31.januar(2023), 100.prosent))
        håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 4.januar, 100.prosent, INNTEKT))
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertTilstander(3.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING)
    }
}
