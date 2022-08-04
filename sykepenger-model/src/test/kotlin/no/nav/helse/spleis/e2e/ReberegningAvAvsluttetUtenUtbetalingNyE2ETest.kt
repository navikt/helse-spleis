package no.nav.helse.spleis.e2e

import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ReberegningAvAvsluttetUtenUtbetalingNyE2ETest : AbstractEndToEndTest() {

    @Test
    fun `revurderer ikke eldre skjæringstidspunkt`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        nyttVedtak(1.mars, 31.mars)
        nullstillTilstandsendringer()
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(1.januar til 16.januar))
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET)
    }

    @Test
    fun `revurderer ikke eldre skjæringstidspunkt selv ved flere mindre perioder`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(21.januar, 26.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 26.januar, 100.prosent))

        håndterUtbetalingshistorikk(2.vedtaksperiode)
        nyttVedtak(1.mars, 31.mars)

        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(10.januar til 25.januar))

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET)
    }


    @Test
    fun `gjenopptar ikke behandling dersom det er nyere periode som er utbetalt`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        nyttVedtak(1.mars, 31.mars)
        håndterInntektsmelding(listOf(1.januar til 16.januar))

        håndterSykmelding(Sykmeldingsperiode(1.mai, 15.mai, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(16.mai, 28.mai, 100.prosent))
        håndterSøknad(Sykdom(1.mai, 15.mai, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode)
        håndterSøknad(Sykdom(16.mai, 28.mai, 100.prosent))

        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(4.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
    }

    @Test
    fun `revurderer ikke avsluttet periode dersom perioden fortsatt er innenfor agp etter IM`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        nullstillTilstandsendringer()
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(5.januar til 20.januar))
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `inntektsmelding gjør om kort periode til arbeidsdager`() {
        håndterSykmelding(Sykmeldingsperiode(19.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(18.januar, 20.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(21.januar, 3.februar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 3.februar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        nullstillTilstandsendringer()

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        håndterInntektsmelding(listOf(10.januar til 20.januar, 28.januar til 1.februar))

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)

        assertTrue(inspektør.sykdomstidslinje[21.januar] is Dag.FriskHelgedag)
        assertTrue(inspektør.sykdomstidslinje[27.januar] is Dag.FriskHelgedag)

        håndterYtelser(2.vedtaksperiode)

        assertWarning("Første fraværsdag i inntektsmeldingen er ulik skjæringstidspunktet. Kontrollér at inntektsmeldingen er knyttet til riktig periode.", 1.vedtaksperiode.filter(a1))
        assertNoWarnings(2.vedtaksperiode.filter(a1))

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_VILKÅRSPRØVING_REVURDERING)
    }

    @Test
    fun `inntektsmelding gjør om kort periode til arbeidsdager etter utbetalt`() {
        håndterSykmelding(Sykmeldingsperiode(19.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(18.januar, 20.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(21.januar, 3.februar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 3.februar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(4.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(4.februar, 28.februar, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode)
        håndterInntektsmelding(listOf(19.januar til 3.februar))
        håndterYtelser(3.vedtaksperiode)
        håndterVilkårsgrunnlag(3.vedtaksperiode)
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()

        nullstillTilstandsendringer()

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(3.vedtaksperiode, AVSLUTTET)

        håndterInntektsmelding(listOf(10.januar til 20.januar, 28.januar til 1.februar))

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)

        assertTrue(inspektør.sykdomstidslinje[21.januar] is Dag.FriskHelgedag)
        assertTrue(inspektør.sykdomstidslinje[27.januar] is Dag.FriskHelgedag)

        håndterYtelser(3.vedtaksperiode)

        assertWarning("Første fraværsdag i inntektsmeldingen er ulik skjæringstidspunktet. Kontrollér at inntektsmeldingen er knyttet til riktig periode.", 1.vedtaksperiode.filter(a1))
        assertNoWarnings(2.vedtaksperiode.filter(a1))
        assertNoWarnings(3.vedtaksperiode.filter(a1))

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_VILKÅRSPRØVING_REVURDERING)
    }

    @Test
    fun `inntektsmelding gjør at kort periode faller utenfor agp - før vilkårsprøving`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(21.januar, 27.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 27.januar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(28.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(28.januar, 31.januar, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode)

        nullstillTilstandsendringer()

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)

        håndterInntektsmelding(listOf(10.januar til 25.januar))
        håndterYtelser(2.vedtaksperiode)

        assertNoWarnings(1.vedtaksperiode.filter(a1))
        assertNoWarnings(2.vedtaksperiode.filter(a1))
        assertNoWarnings(3.vedtaksperiode.filter(a1))

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_VILKÅRSPRØVING_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `inntektsmelding gjør at kort periode faller utenfor agp - etter vilkårsprøving`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(21.januar, 27.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 27.januar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(28.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(28.januar, 31.januar, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode)
        håndterInntektsmelding(listOf(12.januar til 27.januar))
        håndterYtelser(3.vedtaksperiode)
        håndterVilkårsgrunnlag(3.vedtaksperiode)
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)

        nullstillTilstandsendringer()

        håndterInntektsmelding(listOf(10.januar til 25.januar))
        håndterYtelser(2.vedtaksperiode)

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_VILKÅRSPRØVING_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `inntektsmelding gjør at kort periode faller utenfor agp - etter utbetalt`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(21.januar, 27.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 27.januar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(28.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(28.januar, 31.januar, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode)
        håndterInntektsmelding(listOf(12.januar til 27.januar))
        håndterYtelser(3.vedtaksperiode)
        håndterVilkårsgrunnlag(3.vedtaksperiode)
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()

        nullstillTilstandsendringer()

        håndterInntektsmelding(listOf(10.januar til 25.januar))

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `revurderer ikke avsluttet periode dersom perioden fortsatt er innenfor agp etter IM selv ved flere mindre`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterSykmelding(Sykmeldingsperiode(21.januar, 25.januar, 100.prosent))

        håndterSøknad(Sykdom(21.januar, 25.januar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(10.januar til 25.januar))

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `avsluttet periode trenger egen inntektsmelding etter at inntektsmelding treffer forrige`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(23.januar, 25.januar, 100.prosent))
        håndterSøknad(Sykdom(23.januar, 25.januar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(29.januar, 29.januar, 100.prosent))
        håndterSøknad(Sykdom(29.januar, 29.januar, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode)

        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(5.januar til 20.januar))

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `avsluttet periode trenger egen inntektsmelding etter at inntektsmelding treffer forrige 2`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(21.januar, 25.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 25.januar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(29.januar, 29.januar, 100.prosent))
        håndterSøknad(Sykdom(29.januar, 29.januar, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode)

        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(5.januar til 20.januar))

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_REVURDERING)
    }

    @Test
    fun `gjenopptar behandling på neste periode dersom inntektsmelding treffer avsluttet periode`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(21.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(5.januar til 20.januar))
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
    }

    @Test
    fun `revurderer ved mottatt inntektsmelding - påfølgende periode med im går i vanlig løype`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(21.januar, 26.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 26.januar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(30.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(30.januar, 31.januar, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode)

        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(10.januar til 25.januar))
        håndterInntektsmelding(listOf(10.januar til 25.januar), 30.januar)

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `revurderer ved mottatt inntektsmelding - påfølgende periode med im går i vanlig løype - omvendt`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(21.januar, 26.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 26.januar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(30.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(30.januar, 31.januar, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode)

        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(10.januar til 25.januar), 30.januar)
        håndterInntektsmelding(listOf(10.januar til 25.januar))

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `inntektsmelding ag1 - ag1 må vente på inntekt for ag2`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 18.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(3.januar, 18.januar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode, orgnummer = a1)
        håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(1.vedtaksperiode, orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(19.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(19.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(19.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(2.vedtaksperiode, orgnummer = a1)
        håndterSøknad(Sykdom(19.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(2.vedtaksperiode, orgnummer = a2)

        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_REVURDERING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a2)
    }

    @Test
    fun `inntektsmelding ag2 - ag2 må vente på inntekt for ag1`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 18.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(3.januar, 18.januar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode, orgnummer = a1)
        håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(1.vedtaksperiode, orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(19.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(19.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(19.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(2.vedtaksperiode, orgnummer = a1)
        håndterSøknad(Sykdom(19.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(2.vedtaksperiode, orgnummer = a2)

        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a2)

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_REVURDERING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
    }

    @Test
    fun `inntektsmelding for begge arbeidsgivere - bare én av de korte periodene skal utbetales`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 18.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(3.januar, 18.januar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode, orgnummer = a1)
        håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(1.vedtaksperiode, orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(19.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(19.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(19.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(2.vedtaksperiode, orgnummer = a1)
        håndterSøknad(Sykdom(19.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(2.vedtaksperiode, orgnummer = a2)

        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_REVURDERING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a2)

        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(3.januar til 19.januar), orgnummer = a2)

        assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
    }

    @Test
    fun `inntektsmelding for begge arbeidsgivere - begge de korte periodene skal utbetales`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 18.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(3.januar, 18.januar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode, orgnummer = a1)
        håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(1.vedtaksperiode, orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(19.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(19.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(19.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(2.vedtaksperiode, orgnummer = a1)
        håndterSøknad(Sykdom(19.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(2.vedtaksperiode, orgnummer = a2)

        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_REVURDERING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a2)

        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)

        assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_VILKÅRSPRØVING_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)

        nullstillTilstandsendringer()
        håndterUtbetalt(orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)

        assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)

        nullstillTilstandsendringer()
        håndterUtbetalt(orgnummer = a2)

        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)

    }
}
