package no.nav.helse.spleis.e2e

import java.time.LocalDateTime
import no.nav.helse.assertForventetFeil
import no.nav.helse.august
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.juni
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
import no.nav.helse.person.TilstandType.REVURDERING_FEILET
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
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
    fun `avvist revurdering uten tidligere utbetaling kan forkastes`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(21.januar, 27.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 27.januar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        håndterInntektsmelding(listOf(10.januar til 25.januar), beregnetInntekt = INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        nullstillTilstandsendringer()
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, utbetalingGodkjent = false)

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING, REVURDERING_FEILET)
    }

    @Test
    fun `tildele utbetaling etter reberegning`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(21.januar, 27.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 27.januar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(27.januar, Dagtype.Feriedag)))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        val vedtaksperiode1Utbetalinger = inspektør.utbetalinger(1.vedtaksperiode)
        val vedtaksperiode2Utbetalinger = inspektør.utbetalinger(2.vedtaksperiode)
        assertEquals(2, vedtaksperiode1Utbetalinger.size)
        assertEquals(2, vedtaksperiode2Utbetalinger.size)
        assertTrue(vedtaksperiode1Utbetalinger.first().hørerSammen(vedtaksperiode2Utbetalinger.first()))
        assertTrue(vedtaksperiode1Utbetalinger.last().hørerSammen(vedtaksperiode2Utbetalinger.last()))

        assertTilstander(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET,)
    }

    @Test
    fun `avvist revurdering uten tidligere utbetaling forkaster alle som omfattes av revurderingen`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(21.januar, 27.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 27.januar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        nullstillTilstandsendringer()
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, utbetalingGodkjent = false)

        assertForkastetPeriodeTilstander(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING, REVURDERING_FEILET)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING, REVURDERING_FEILET)
    }

    @Test
    fun `avvist revurdering uten tidligere utbetaling forkaster nyere forlengelser`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(21.januar, 27.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 27.januar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(28.januar, 27.februar, 100.prosent))
        håndterSøknad(Sykdom(28.januar, 27.februar, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode)

        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        nullstillTilstandsendringer()
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, utbetalingGodkjent = false)

        assertForkastetPeriodeTilstander(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING, REVURDERING_FEILET)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING, REVURDERING_FEILET)
        assertForkastetPeriodeTilstander(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, TIL_INFOTRYGD)
    }

    @Test
    fun `avvist revurdering uten tidligere utbetaling forkaster ikke nyere perioder`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(21.januar, 27.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 27.januar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(31.januar, 27.februar, 100.prosent))
        håndterSøknad(Sykdom(31.januar, 27.februar, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode)

        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        nullstillTilstandsendringer()
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, utbetalingGodkjent = false)

        assertForkastetPeriodeTilstander(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING, REVURDERING_FEILET)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING, REVURDERING_FEILET)
        assertForkastetPeriodeTilstander(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, TIL_INFOTRYGD)
    }

    @Test
    fun `avvist revurdering uten tidligere utbetaling gjenopptar nyere perioder som har inntekt`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(21.januar, 27.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 27.januar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(31.januar, 27.februar, 100.prosent))
        håndterSøknad(Sykdom(31.januar, 27.februar, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode)

        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 31.januar, beregnetInntekt = INNTEKT)

        nullstillTilstandsendringer()
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, utbetalingGodkjent = false)

        assertForkastetPeriodeTilstander(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING, REVURDERING_FEILET)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING, REVURDERING_FEILET)
        assertForkastetPeriodeTilstander(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, TIL_INFOTRYGD)
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
        assertWarning("Denne perioden var tidligere regnet som innenfor arbeidsgiverperioden", 2.vedtaksperiode.filter(a1))
        //assertNoWarnings(2.vedtaksperiode.filter(a1))

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
        assertWarning("Denne perioden var tidligere regnet som innenfor arbeidsgiverperioden", 2.vedtaksperiode.filter(a1))
        //assertNoWarnings(2.vedtaksperiode.filter(a1))
        assertNoWarnings(3.vedtaksperiode.filter(a1))

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_VILKÅRSPRØVING_REVURDERING)
    }

    @Test
    fun `avvik i inntekt ved vilkårsprøving`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(21.januar, 27.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 27.januar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        nullstillTilstandsendringer()

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        håndterInntektsmelding(listOf(10.januar til 25.januar), beregnetInntekt = INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT * 2)

        assertNoWarnings(1.vedtaksperiode.filter(a1))
        assertWarning("Har mer enn 25 % avvik", 2.vedtaksperiode.filter(a1))

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_VILKÅRSPRØVING_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
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
        assertWarning("Denne perioden var tidligere regnet som innenfor arbeidsgiverperioden", 2.vedtaksperiode.filter(a1))
        // assertNoWarnings(2.vedtaksperiode.filter(a1))
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
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
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

    @Test
    fun `arbeidsgiver 2 er utenfor arbeidsgiverperioden, men ikke arbeidsgiver 1 - arbeidsgiver 2 blir opprettet først`() {
        håndterSykmelding(Sykmeldingsperiode(17.juni(2022), 21.juni(2022), 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(17.juni(2022), 21.juni(2022), 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(1.vedtaksperiode, orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(17.juni(2022), 21.juni(2022), 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(17.juni(2022), 21.juni(2022), 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode, orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(22.juni(2022), 3.juli(2022), 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(22.juni(2022), 3.juli(2022), 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(2.vedtaksperiode, orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(22.juni(2022), 3.juli(2022), 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(22.juni(2022), 3.juli(2022), 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(2.vedtaksperiode, orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(20.juli(2022), 28.juli(2022), 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(20.juli(2022), 28.juli(2022), 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(3.vedtaksperiode, orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(29.juli(2022), 3.august(2022), 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(29.juli(2022), 3.august(2022), 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(4.vedtaksperiode, orgnummer = a2)

        nullstillTilstandsendringer()

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)
        assertTilstander(3.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)
        assertTilstander(4.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)

        nullstillTilstandsendringer()

        håndterInntektsmelding(listOf(
            7.juni(2022) til 7.juni(2022),
            9.juni(2022) til 10.juni(2022),
            17.juni(2022) til 29.juni(2022)
        ), orgnummer = a2)

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_REVURDERING, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a2)
        assertTilstander(3.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_REVURDERING, orgnummer = a2)
        assertTilstander(4.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_REVURDERING, orgnummer = a2)
    }

    @Test
    fun `arbeidsgiver 2 er utenfor arbeidsgiverperioden, men ikke arbeidsgiver 1 - arbeidsgiver 1 blir opprettet først`() {
        håndterSykmelding(Sykmeldingsperiode(17.juni(2022), 21.juni(2022), 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(17.juni(2022), 21.juni(2022), 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode, orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(17.juni(2022), 21.juni(2022), 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(17.juni(2022), 21.juni(2022), 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(1.vedtaksperiode, orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(22.juni(2022), 3.juli(2022), 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(22.juni(2022), 3.juli(2022), 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(2.vedtaksperiode, orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(22.juni(2022), 3.juli(2022), 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(22.juni(2022), 3.juli(2022), 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(2.vedtaksperiode, orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(20.juli(2022), 28.juli(2022), 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(20.juli(2022), 28.juli(2022), 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(3.vedtaksperiode, orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(29.juli(2022), 3.august(2022), 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(29.juli(2022), 3.august(2022), 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(4.vedtaksperiode, orgnummer = a2)

        nullstillTilstandsendringer()

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)
        assertTilstander(3.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)
        assertTilstander(4.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)

        nullstillTilstandsendringer()

        håndterInntektsmelding(listOf(
            7.juni(2022) til 7.juni(2022),
            9.juni(2022) til 10.juni(2022),
            17.juni(2022) til 29.juni(2022)
        ), orgnummer = a2)

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_REVURDERING, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a2)
        assertTilstander(3.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_REVURDERING, orgnummer = a2)
        assertTilstander(4.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_REVURDERING, orgnummer = a2)
    }

    @Test
    fun `arbeidsgiver 1 er utenfor arbeidsgiverperioden, men ikke arbeidsgiver 2 - arbeidsgiver 1 blir opprettet først`() {
        håndterSykmelding(Sykmeldingsperiode(17.juni(2022), 21.juni(2022), 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(17.juni(2022), 21.juni(2022), 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode, orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(17.juni(2022), 21.juni(2022), 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(17.juni(2022), 21.juni(2022), 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(1.vedtaksperiode, orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(22.juni(2022), 3.juli(2022), 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(22.juni(2022), 3.juli(2022), 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(2.vedtaksperiode, orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(22.juni(2022), 3.juli(2022), 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(22.juni(2022), 3.juli(2022), 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(2.vedtaksperiode, orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(20.juli(2022), 28.juli(2022), 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(20.juli(2022), 28.juli(2022), 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(3.vedtaksperiode, orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(29.juli(2022), 3.august(2022), 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(29.juli(2022), 3.august(2022), 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(4.vedtaksperiode, orgnummer = a1)

        nullstillTilstandsendringer()

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)
        assertTilstander(3.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a1)
        assertTilstander(4.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a1)

        nullstillTilstandsendringer()

        håndterInntektsmelding(listOf(
            7.juni(2022) til 7.juni(2022),
            9.juni(2022) til 10.juni(2022),
            17.juni(2022) til 29.juni(2022)
        ), orgnummer = a1)

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_REVURDERING, orgnummer = a2)
        assertTilstander(3.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_REVURDERING, orgnummer = a1)
        assertTilstander(4.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_REVURDERING, orgnummer = a1)
    }

    @Test
    fun `arbeidsgiver 1 er utenfor arbeidsgiverperioden, men ikke arbeidsgiver 2 - feil ved revurdering forkaster periodene`() {
        håndterSykmelding(Sykmeldingsperiode(17.juni(2022), 21.juni(2022), 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(17.juni(2022), 21.juni(2022), 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode, orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(17.juni(2022), 21.juni(2022), 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(17.juni(2022), 21.juni(2022), 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(1.vedtaksperiode, orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(22.juni(2022), 3.juli(2022), 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(22.juni(2022), 3.juli(2022), 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(2.vedtaksperiode, orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(22.juni(2022), 3.juli(2022), 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(22.juni(2022), 3.juli(2022), 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(2.vedtaksperiode, orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(20.juli(2022), 28.juli(2022), 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(20.juli(2022), 28.juli(2022), 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(3.vedtaksperiode, orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(29.juli(2022), 3.august(2022), 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(29.juli(2022), 3.august(2022), 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(4.vedtaksperiode, orgnummer = a1)

        håndterInntektsmelding(listOf(
            7.juni(2022) til 7.juni(2022),
            9.juni(2022) til 10.juni(2022),
            17.juni(2022) til 29.juni(2022)
        ), orgnummer = a1)
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(2.vedtaksperiode, orgnummer = a1)
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterSimulering(2.vedtaksperiode, orgnummer = a1)

        nullstillTilstandsendringer()
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, utbetalingGodkjent = false, orgnummer = a1)

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING, REVURDERING_FEILET, orgnummer = a1)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_REVURDERING, orgnummer = a2)
        assertForkastetPeriodeTilstander(3.vedtaksperiode, AVVENTER_REVURDERING, orgnummer = a1)
        assertForkastetPeriodeTilstander(4.vedtaksperiode, AVVENTER_REVURDERING, orgnummer = a1)
    }

    @Test
    fun `utbetalinger i infotrygd etterpå`() {
        håndterSykmelding(Sykmeldingsperiode(5.februar, 20.februar, 100.prosent))
        håndterSøknad(Sykdom(5.februar, 20.februar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, besvart = LocalDateTime.now().minusHours(48))

        håndterSykmelding(Sykmeldingsperiode(21.februar, 11.mars, 100.prosent))
        håndterSøknad(Sykdom(21.februar, 11.mars, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 15.februar, 11.mars, 100.prosent, INNTEKT))


        håndterInntektsmelding(listOf(
            15.januar til 15.januar,
            26.januar til 30.januar,
            5.februar til 14.februar
        ))

        assertForventetFeil(
            forklaring = "selv om 1.vedtaksperiode ikke skal være i AUU mer, er perioden betalt i Infotrygd og bør ikke revurderes." +
                    "Dersom revurdering startes, burde perioden avbryte revurdering som følge av AVVENTER_HISTORIKK_REVURDERING" +
                    "ved at perioden overlapper med Infotrygd",
            nå = {
                assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            },
            ønsket = {
                assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING)
            }
        )
        assertForkastetPeriodeTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, TIL_INFOTRYGD)
    }

    @Test
    fun `arbeidsgiver angrer på innsendt arbeidsgiverperiode`() {
        håndterSykmelding(Sykmeldingsperiode(5.februar, 20.februar, 100.prosent))
        håndterSøknad(Sykdom(5.februar, 20.februar, 100.prosent), Ferie(10.februar, 20.februar))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(
            29.januar til 13.februar
        ))
        håndterInntektsmelding(listOf(
            22.januar til 6.februar
        ))

        assertForventetFeil(
            forklaring = "revurderingsoppgavene er manuelle så riktig løsning (enn så lenge) bør kanskje innebære en warning" +
                    "om at vi har mottatt flere inntektsmeldinger, med utfall at saksbehandler avviser revurderingen (og periodene/dagene blir forkastet) ?",
            nå = {
                assertTrue(inspektør.sykdomstidslinje[10.februar] is Dag.ArbeidsgiverHelgedag)
                assertTrue(inspektør.sykdomstidslinje[11.februar] is Dag.ArbeidsgiverHelgedag)
                assertTrue(inspektør.sykdomstidslinje[13.februar] is Dag.Arbeidsgiverdag)
            },
            ønsket = {
                assertTrue(inspektør.sykdomstidslinje[10.februar] is Dag.Feriedag)
                assertTrue(inspektør.sykdomstidslinje[11.februar] is Dag.Feriedag)
                assertTrue(inspektør.sykdomstidslinje[13.februar] is Dag.Feriedag)
            }
        )
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `revvurdering etter periode til godkjenning`() {
        håndterSykmelding(Sykmeldingsperiode(1.juni, 16.juni, 100.prosent))
        håndterSøknad(Sykdom(1.juni, 16.juni, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(17.juni, 30.juni, 100.prosent))
        håndterSøknad(Sykdom(17.juni, 30.juni, 100.prosent))
        håndterInntektsmelding(listOf(1.juni til 16.juni))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, utbetalingGodkjent = false)

        // todo: vi mener arbeidsgiverperioden avbrytes 17. juli pga. 16 dager opphold siden 30. juni (hele perioden 13. juli-20.juli er ferie).
        //      syk+opphold+ferie => ferie regnes som opphold, ikke sykdom
        // Pga. mottatt inntektsmelding så ser dette rart ut
        håndterSykmelding(Sykmeldingsperiode(13.juli, 20.juli, 100.prosent))
        håndterSøknad(Sykdom(13.juli, 20.juli, 100.prosent), Ferie(13.juli, 20.juli))

        håndterSykmelding(Sykmeldingsperiode(25.juli, 31.juli, 100.prosent))
        håndterSøknad(Sykdom(25.juli, 31.juli, 100.prosent))

        nullstillTilstandsendringer()

        // todo: første fraværsdag-dagen vinner over feriedagen 13.juli og blir en "arbeidsgiverdag".
        // denne dagen blir en AvvistDag på utbetalingstidslinjen fordi egenmeldingsdager etter arbeidsgiverpereioden avvises. Dette medfører warning, som igjen medfører
        // at perioden går til godkjenning.
        håndterInntektsmelding(listOf(1.juni til 16.juni), førsteFraværsdag = 13.juli)

        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(1.juni til 16.juni), førsteFraværsdag = 25.juli)

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        assertTilstander(4.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE)
    }
}
