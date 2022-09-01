package no.nav.helse.spleis.e2e.flere_arbeidsgivere

import java.time.LocalDate
import no.nav.helse.assertForventetFeil
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.IdInnhenter
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertForkastetPeriodeTilstander
import no.nav.helse.spleis.e2e.assertIngenVarsel
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.forlengTilGodkjenning
import no.nav.helse.spleis.e2e.forkastAlle
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.grunnlag
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterPåminnelse
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikk
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyeVedtak
import no.nav.helse.spleis.e2e.repeat
import no.nav.helse.spleis.e2e.sammenligningsgrunnlag
import no.nav.helse.spleis.e2e.tilGodkjenning
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class FlereArbeidsgivereFlytTest : AbstractEndToEndTest() {

    @Test
    fun `En periode i AvventerTidligerEllerOverlappendePerioder for hver arbeidsgiver - kun en periode skal gå videre`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.mars til 16.mars), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(listOf(1.mars til 16.mars), orgnummer = a2)

        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt()

        assertTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
    }

    @Test
    fun `To overlappende vedtaksperioder for forskjellige arbeidsgivere - skal ikke gå videre uten at begge har IM og søknad`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)

        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            orgnummer = a1
        )
    }

    @Test
    fun `Periode i AUU for a2 hensyntas på utbetalingen til a1`() {
        nyeVedtak(1.januar, 31.januar, a1, a2, inntekt = 40000.månedlig)
        forlengVedtak(1.februar, 28.februar, a1)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 10.mars, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.mars, 10.mars, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(2.vedtaksperiode, orgnummer = a2)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 10.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.mars, 10.mars, 100.prosent), orgnummer = a1)
        håndterYtelser(3.vedtaksperiode, orgnummer = a1)

        assertEquals(1080.daglig, inspektør(a1).utbetalingstidslinjer(3.vedtaksperiode)[1.mars].økonomi.inspektør.arbeidsgiverbeløp)
    }


    @Test
    fun `flere AG - kort periode har gap på arbeidsgivernivå men er sammenhengende på personnivå - kort periode`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 28.februar, a1)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 10.mars, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.mars, 10.mars, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(2.vedtaksperiode, orgnummer = a2)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)
    }

    @Test
    fun `flere AG - periode har gap på arbeidsgivernivå men er sammenhengende på personnivå - sender feilaktig flere perioder til behandling`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 28.februar, orgnummer = a1)
        forlengTilGodkjenning(1.mars, 31.mars, orgnummer = a2)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING, orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 19.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.mars, 31.mars, 19.prosent), orgnummer = a1)
        håndterYtelser(3.vedtaksperiode, orgnummer = a1)

        assertSisteTilstand(3.vedtaksperiode, AVVENTER_SIMULERING, orgnummer = a1)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)

        assertForventetFeil(
            forklaring = "vi avviser de 16 første dagene på AG1 fordi det er ny arbeidsgiverperiode på AG2: https://trello.com/c/NKpQWdsd",
            nå = {
                assertEquals(12, inspektør(a1).utbetalingstidslinjer(3.vedtaksperiode).inspektør.avvistDagTeller)
            },
            ønsket = {
                assertEquals(0, inspektør(a1).utbetalingstidslinjer(3.vedtaksperiode).inspektør.avvistDagTeller)
            }
        )
    }

    @Test
    fun `To overlappende vedtaksperioder med en forlengelse - vedtaksperiode for ag2 dytter vedtaksperiode for ag1 videre`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)

        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a2)

        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            orgnummer = a1
        )
        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
    }

    @Test
    fun `drawio -- MANGLER SØKNAD`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        assertEquals(emptyList<Periode>(), inspektør(a1).sykmeldingsperioder())

        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(listOf(1.mars til 16.mars), orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, a1)

        håndterInntektsmelding(listOf(1.mars til 16.mars), orgnummer = a2)
        utbetalPeriode(1.vedtaksperiode, a1, 1.mars)
        utbetalPeriodeEtterVilkårsprøving(1.vedtaksperiode, a2)
        assertTilstand(1.vedtaksperiode, AVSLUTTET, a1)
        assertTilstand(1.vedtaksperiode, AVSLUTTET, a2)
    }

    @Test
    fun `drawio -- ULIK LENGDE PÅ SYKEFRAVÆR`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(5.januar, 5.februar, 100.prosent), orgnummer = a2)
        assertEquals(listOf(1.januar til 31.januar), inspektør(a1).sykmeldingsperioder())
        assertEquals(listOf(5.januar til 5.februar), inspektør(a2).sykmeldingsperioder())

        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        assertEquals(emptyList<Periode>(), inspektør(a1).sykmeldingsperioder())

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        assertEquals(listOf(1.februar til 28.februar), inspektør(a1).sykmeldingsperioder())

        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, a1)

        håndterSøknad(Sykdom(5.januar, 5.februar, 100.prosent), orgnummer = a2)
        assertEquals(emptyList<Periode>(), inspektør(a2).sykmeldingsperioder())

        håndterInntektsmelding(listOf(5.januar til 20.januar), orgnummer = a2)

        utbetalPeriode(1.vedtaksperiode, a1, 1.januar)

        assertTilstand(1.vedtaksperiode, AVSLUTTET, a1)

        håndterSykmelding(Sykmeldingsperiode(6.februar, 26.februar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(6.februar, 26.februar, 100.prosent), orgnummer = a2)

        utbetalPeriodeEtterVilkårsprøving(1.vedtaksperiode, a2)
        assertTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)

        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        assertForventetFeil(
            forklaring = "Må revurdere fordi søknad overlapper med utbetalt periode hos annen arbeidsgiver. " +
                    "Ta en klype med salt på ønsket asserts",
            nå = {
                assertTilstand(2.vedtaksperiode, TIL_INFOTRYGD, orgnummer = a1)
                assertTilstand(2.vedtaksperiode, TIL_INFOTRYGD, orgnummer = a2)
            },
            ønsket = {
                // Litt usikker på hvem av arbeidsgiverne som blir revurdert først, men alle må revurderes
                assertTilstand(1.vedtaksperiode, AVVENTER_REVURDERING, orgnummer = a2)
                assertTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)

                assertTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
                assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a1)
            }
        )
    }

    @Test
    fun `drawio -- BURDE BLOKKERE PGA MANGLENDE IM`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a2)

        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a2)
        assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)

        utbetalPeriode(1.vedtaksperiode, a1, 1.januar)
        utbetalPeriodeEtterVilkårsprøving(1.vedtaksperiode, a2)

        assertTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
    }

    @Test
    fun `drawio -- PERIODE HOS AG1 STREKKER SEG OVER TO PERIODER HOS AG2 - Må vente på alle IM`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(2.januar, 18.januar, 100.prosent), orgnummer = a2)
        håndterSykmelding(Sykmeldingsperiode(20.januar, 30.januar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(2.januar, 18.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(20.januar, 30.januar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)

        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar, orgnummer = a1)
        håndterInntektsmelding(listOf(2.januar til 17.januar), førsteFraværsdag = 2.januar, orgnummer = a2)

        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
        assertTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a2)

        håndterInntektsmelding(listOf(2.januar til 17.januar), førsteFraværsdag = 20.januar, orgnummer = a2)

        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a2)
        assertTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)

        utbetalPeriode(1.vedtaksperiode, orgnummer = a2, 1.januar)

        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a2)


        utbetalPeriodeEtterVilkårsprøving(2.vedtaksperiode, orgnummer = a2)
        assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertTilstand(2.vedtaksperiode, AVSLUTTET, orgnummer = a2)

        utbetalPeriodeEtterVilkårsprøving(1.vedtaksperiode, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertTilstand(2.vedtaksperiode, AVSLUTTET, orgnummer = a2)
    }

    @Test
    fun `Skal ikke vente på inntektsmelding fra forlengelsen for å gå videre med førstegangsbehandling`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 18.januar, 100.prosent), orgnummer = a2)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(1.januar, 18.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar, orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar, orgnummer = a2)

        håndterPåminnelse(2.vedtaksperiode, orgnummer = a2, påminnetTilstand = AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        håndterUtbetalingshistorikk(2.vedtaksperiode, orgnummer = a2)

        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a2)
        assertTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
    }

    @Test
    fun `drawio -- Må vente på alle IM (forlengelse)`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 17.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(18.januar, 17.februar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(18.januar, 17.februar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(1.januar, 17.januar, 100.prosent), orgnummer = a1)

        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)

        håndterSøknad(Sykdom(18.januar, 17.februar, 100.prosent), orgnummer = a1)
        assertTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)

        håndterSøknad(Sykdom(18.januar, 17.februar, 100.prosent), orgnummer = a2)
        assertTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a2)

        håndterInntektsmelding(listOf(18.januar til 2.februar), orgnummer = a2)
        assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
        assertTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
    }

    @Test
    fun `drawio -- Må vente på alle IM (gap)`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 22.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(25.januar, 17.februar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(25.januar, 17.februar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(1.januar, 22.januar, 100.prosent), orgnummer = a1)

        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        assertForventetFeil(
            forklaring = "Vedtaksperioden kan gå videre til AvventerHistorikk siden vi har gap til neste periode",
            nå = { assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1) },
            ønsket = { assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1) }
        )

        håndterSøknad(Sykdom(25.januar, 17.februar, 100.prosent), orgnummer = a1)
        assertTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a1)

        håndterSøknad(Sykdom(25.januar, 17.februar, 100.prosent), orgnummer = a2)
        assertTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a2)

        håndterInntektsmelding(listOf(25.januar til 9.februar), orgnummer = a1)
        håndterInntektsmelding(listOf(25.januar til 9.februar), orgnummer = a2)
        assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
        assertTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1)
        utbetalPeriodeEtterVilkårsprøving(1.vedtaksperiode, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)

        utbetalPeriode(2.vedtaksperiode, orgnummer = a1, 25.januar)
        assertTilstand(2.vedtaksperiode, AVSLUTTET, orgnummer = a1)

        utbetalPeriodeEtterVilkårsprøving(1.vedtaksperiode, orgnummer = a2)
        assertTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
    }

    @Test
    fun `Kort periode skal ikke blokkeres av mangelende søknad`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 100.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(21.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(21.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a2)

        assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
    }

    @Test
    fun `En arbeidsgiver uten sykdom, blir syk i forlengelsen - skal vente på inntektsmelding før den går til AvventerTidligereEllerOverlappendePerioder`() {
        val periode1 = 1.januar(2021) til 31.januar(2021)
        val periode2 = 1.februar(2021) til 28.februar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode1.start, periode1.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(periode1.start, periode1.endInclusive, 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.januar(2021) til 16.januar(2021)),
            førsteFraværsdag = 1.januar(2021),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            orgnummer = a1,
            inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioderForSammenligningsgrunnlag {
                    1.januar(2020) til 1.desember(2020) inntekter {
                        a1 inntekt INNTEKT
                        a2 inntekt 1000.månedlig
                    }
                }
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                listOf(
                    grunnlag(a1, 1.januar(2021), INNTEKT.repeat(3)),
                    grunnlag(a2, 1.januar(2021), 1000.månedlig.repeat(3))
                ), arbeidsforhold = emptyList()
            ),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null)
            )
        )
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)


        håndterSykmelding(Sykmeldingsperiode(periode2.start, periode2.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode2.start, periode2.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(periode2.start, periode2.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(periode2.start, periode2.endInclusive, 100.prosent), orgnummer = a2)

        assertTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a2)
        håndterInntektsmelding(
            listOf(1.februar(2021) til 16.februar(2021)),
            førsteFraværsdag = 1.februar(2021),
            orgnummer = a2
        )
        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
    }

    @Disabled
    @Test
    fun `forlengelse av sykdom hos AG2 hvor sykdom startet hos AG1 en annen mnd enn skjæringstidspunkt, vedtaksperiode 2 for AG2 skal ikke vente på IM`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = a2)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)

        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(listOf(1.februar til 16.februar), orgnummer = a2)

        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a2)

        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
        assertTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
    }

    @Test
    fun `kort periode hos annen arbeidsgiver skal ikke blokkere videre behandling pga manglende IM`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode, orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(1.vedtaksperiode, orgnummer = a2)

        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a2)
        assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a2)
    }

    @Test
    fun `kort periode hos annen arbeidsgiver vi tidligere har utbetalt til skal ikke blokkere videre behandling pga manglende IM`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterUtbetalingshistorikk(1.vedtaksperiode, orgnummer = a2)

        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a2)

        utbetalPeriode(1.vedtaksperiode, a2, 1.januar)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 16.mars, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode, orgnummer = a1)

        håndterSøknad(Sykdom(1.mars, 16.mars, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(2.vedtaksperiode, orgnummer = a2)

        håndterInntektsmelding(listOf(1.mars til 16.mars), orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
    }

    @Test
    fun `inntektsmelding for arbeidsgiver 2 har ikke full refusjon - kan gå videre til utbetaling`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT,
            refusjon = Inntektsmelding.Refusjon(INNTEKT, null),
            orgnummer = a1
        )
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT,
            refusjon = Inntektsmelding.Refusjon(INGEN, null),
            orgnummer = a2
        )

        assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
    }

    @Test
    fun `gjenopptaBehandling poker ikke neste arbeidsgiver til AvventerHistorikk før den blir kastet ut`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a2)

        forkastAlle(hendelselogg)

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            TIL_INFOTRYGD,
            orgnummer = a1
        )
        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            TIL_INFOTRYGD,
            orgnummer = a2
        )
    }

    @Test
    fun `bruker har satt inntektskilde til ANDRE_ARBEIDSFORHOLD hvor vi har sykmeldingsperioder for begge arbeidsgivere`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(
            Sykdom(1.januar, 31.januar, 100.prosent),
            andreInntektskilder = listOf(Søknad.Inntektskilde(true, "ANDRE_ARBEIDSFORHOLD")),
            orgnummer = a1
        )
        håndterSøknad(
            Sykdom(1.januar, 31.januar, 100.prosent),
            andreInntektskilder = listOf(Søknad.Inntektskilde(true, "ANDRE_ARBEIDSFORHOLD")),
            orgnummer = a2
        )

        assertTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a2)
        assertIngenVarsel("Den sykmeldte har oppgitt å ha andre arbeidsforhold med sykmelding i søknaden.")
    }

    @Test
    fun `bruker har satt inntektskilde til ANDRE_ARBEIDSFORHOLD men vi kjenner ikke til sykdom for mer enn en arbeidsgiver`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(
            Sykdom(1.januar, 31.januar, 100.prosent),
            andreInntektskilder = listOf(Søknad.Inntektskilde(true, "ANDRE_ARBEIDSFORHOLD")),
            orgnummer = a1
        )

        assertTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a1)
        assertVarsel("Den sykmeldte har oppgitt å ha andre arbeidsforhold med sykmelding i søknaden.")
    }

    @Test
    fun `Gammel sykemeldingsperiode skal ikke blokkere videre behandling av en senere søknad`(){
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(5.februar, 28.februar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(5.februar, 28.februar, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(listOf(5.februar til 20.februar), orgnummer = a2)

        assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer=a2)
    }

    @Test
    fun `får tidligere sykmelding og søknad for en annen arbeidsgiver`() {
        tilGodkjenning(1.februar, 28.februar, a1)
        tilGodkjenning(1.januar, 31.januar, a2)

        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            AVVENTER_BLOKKERENDE_PERIODE,
            orgnummer = a1
        )
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            orgnummer = a2
        )
    }

    @Test
    fun `sykmelding og søknad i forlengelsen til a1 kommer før sykmelding til a2 - skal ikke ha flere perioder i AvventerGodkjenning`() {
        nyeVedtak(1.januar, 31.januar, a2, a1)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterSimulering(2.vedtaksperiode, orgnummer = a1)
        assertTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING, a1)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a2)

        // arbeidsgiveren spleis hører om først er den som blir valgt først til å gå videre i gjenopptaBehandling dersom periodenes tom er lik
        assertTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer=a2)
        assertTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer=a1)
    }

    private fun utbetalPeriodeEtterVilkårsprøving(vedtaksperiode: IdInnhenter, orgnummer: String) {
        håndterYtelser(vedtaksperiode, orgnummer = orgnummer)
        håndterSimulering(vedtaksperiode, orgnummer = orgnummer)
        håndterUtbetalingsgodkjenning(vedtaksperiode, orgnummer = orgnummer)
        håndterUtbetalt(orgnummer = orgnummer)
    }

    private fun vilkårsprøv(vedtaksperiode: IdInnhenter, orgnummer: String, skjæringstidspunkt: LocalDate) {
        håndterYtelser(vedtaksperiode, orgnummer = orgnummer)
        håndterVilkårsgrunnlag(
            vedtaksperiode, orgnummer = orgnummer,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, skjæringstidspunkt, 31000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, skjæringstidspunkt, 31000.månedlig.repeat(12)),
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                listOf(
                    grunnlag(a1, skjæringstidspunkt, 31000.månedlig.repeat(3)),
                    grunnlag(a2, skjæringstidspunkt, 31000.månedlig.repeat(3)),
                ), arbeidsforhold = emptyList()
            ),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH)
            )
        )
    }

    private fun utbetalPeriode(vedtaksperiode: IdInnhenter, orgnummer: String, skjæringstidspunkt: LocalDate) {
        vilkårsprøv(vedtaksperiode, orgnummer, skjæringstidspunkt)
        utbetalPeriodeEtterVilkårsprøving(vedtaksperiode, orgnummer)
    }

}
