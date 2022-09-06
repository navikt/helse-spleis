package no.nav.helse.spleis.e2e.revurdering

import java.time.LocalDate
import no.nav.helse.april
import no.nav.helse.assertForventetFeil
import no.nav.helse.februar
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
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
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertForkastetPeriodeTilstander
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.finnSkjæringstidspunkt
import no.nav.helse.spleis.e2e.forlengTilGodkjenning
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.forlengelseTilGodkjenning
import no.nav.helse.spleis.e2e.grunnlag
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikk
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.spleis.e2e.repeat
import no.nav.helse.spleis.e2e.sammenligningsgrunnlag
import no.nav.helse.spleis.e2e.tilGodkjent
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class RevurderingOutOfOrderTest : AbstractEndToEndTest() {

    @Test
    fun `out-of-order søknad medfører revurdering av avsluttet periode`() {
        nyttVedtak(1.februar, 28.februar)
        nyPeriode(1.januar til 31.januar)

        val februarId = 1.vedtaksperiode
        val januarId = 2.vedtaksperiode

        assertSisteTilstand(februarId, AVVENTER_REVURDERING)
        assertSisteTilstand(januarId, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)

        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(januarId)
        håndterVilkårsgrunnlag(januarId)
        håndterYtelser(januarId)
        håndterSimulering(januarId)
        håndterUtbetalingsgodkjenning(januarId)
        håndterUtbetalt()

        assertSisteTilstand(februarId, AVVENTER_HISTORIKK_REVURDERING)
        assertSisteTilstand(januarId, AVSLUTTET)

        håndterYtelser(februarId)
        håndterSimulering(februarId)
        håndterUtbetalingsgodkjenning(februarId)
        håndterUtbetalt()

        assertSisteTilstand(januarId, AVSLUTTET)
        assertSisteTilstand(februarId, AVSLUTTET)
    }

    @Test
    fun `out of order periode trigger revurdering`() {
        nyttVedtak(1.mai, 31.mai)
        forlengVedtak(1.juni, 30.juni)
        nullstillTilstandsendringer()
        nyttVedtak(1.januar, 31.januar)
        assertSisteTilstand(3.vedtaksperiode, AVSLUTTET)
        assertTilstander(
            1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING
        )
        assertTilstander(
            2.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING
        )
    }

    @Test
    fun `out of order periode uten utbetaling trigger revurdering`() {
        nyttVedtak(1.mai, 31.mai)
        forlengVedtak(1.juni, 30.juni)
        nullstillTilstandsendringer()
        nyPeriode(1.januar til 15.januar)
        håndterUtbetalingshistorikk(3.vedtaksperiode)
        assertTilstander(
            3.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVSLUTTET_UTEN_UTBETALING
        )
        assertTilstander(
            1.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING
        )
        assertTilstander(
            2.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING
        )
    }

    @Test
    fun `revurdering av senere frittstående periode hos ag3 mens overlappende out of order hos ag1 og ag2 utbetales`() {
        håndterSykmelding(Sykmeldingsperiode(1.april, 30.april, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.april, 30.april, 100.prosent), orgnummer = a1)
        val inntekt = 20000.månedlig
        håndterInntektsmelding(listOf(1.april til 16.april), beregnetInntekt = inntekt, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        val sammenligningsgrunnlag = Inntektsvurdering(
            listOf(
                sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(12)),
                sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(12)),
                sammenligningsgrunnlag(a3, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(12))
            )
        )
        val sykepengegrunnlag = InntektForSykepengegrunnlag(
            listOf(
                grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(3)),
                grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(3)),
                grunnlag(a3, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(3))
            ), emptyList()
        )
        val arbeidsforhold = listOf(
            Arbeidsforhold(a1, LocalDate.EPOCH, null),
            Arbeidsforhold(a2, LocalDate.EPOCH, null),
            Arbeidsforhold(a3, LocalDate.EPOCH, null)
        )
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = sammenligningsgrunnlag,
            inntektsvurderingForSykepengegrunnlag = sykepengegrunnlag,
            arbeidsforhold = arbeidsforhold,
            orgnummer = a1
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt()


        nyPeriode(1.februar til 28.februar, a2, a3)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a3)

        håndterInntektsmelding(listOf(1.februar til 16.februar), beregnetInntekt = inntekt, orgnummer = a2)
        håndterInntektsmelding(listOf(1.februar til 16.februar), beregnetInntekt = inntekt, orgnummer = a3)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)

        val sammenligningsgrunnlag2 = Inntektsvurdering(
            listOf(
                sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), inntekt.repeat(12)),
                sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), inntekt.repeat(12)),
                sammenligningsgrunnlag(a3, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), inntekt.repeat(12))
            )
        )
        val sykepengegrunnlag2 = InntektForSykepengegrunnlag(
            listOf(
                grunnlag(a1, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), inntekt.repeat(3)),
                grunnlag(a2, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), inntekt.repeat(3)),
                grunnlag(a3, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), inntekt.repeat(3))
            ), emptyList()
        )
        val arbeidsforhold2 = listOf(
            Arbeidsforhold(a1, LocalDate.EPOCH, null),
            Arbeidsforhold(a2, LocalDate.EPOCH, null),
            Arbeidsforhold(a3, LocalDate.EPOCH, null)
        )
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = sammenligningsgrunnlag2,
            inntektsvurderingForSykepengegrunnlag = sykepengegrunnlag2,
            arbeidsforhold = arbeidsforhold2,
            orgnummer = a2
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a3)

        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a3)
    }

    @Test
    fun `revurdering av senere frittstående periode hos ag3 mens overlappende out of order hos ag1 og ag2 utbetales -- forlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(1.april, 30.april, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.april, 30.april, 100.prosent), orgnummer = a1)
        val inntekt = 20000.månedlig
        håndterInntektsmelding(listOf(1.april til 16.april), beregnetInntekt = inntekt, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        val sammenligningsgrunnlag = Inntektsvurdering(
            listOf(
                sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(12)),
                sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(12)),
                sammenligningsgrunnlag(a3, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(12))
            )
        )
        val sykepengegrunnlag = InntektForSykepengegrunnlag(
            listOf(
                grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(3)),
                grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(3)),
                grunnlag(a3, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(3))
            ), emptyList()
        )
        val arbeidsforhold = listOf(
            Arbeidsforhold(a1, LocalDate.EPOCH, null),
            Arbeidsforhold(a2, LocalDate.EPOCH, null),
            Arbeidsforhold(a3, LocalDate.EPOCH, null)
        )
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = sammenligningsgrunnlag,
            inntektsvurderingForSykepengegrunnlag = sykepengegrunnlag,
            arbeidsforhold = arbeidsforhold,
            orgnummer = a1
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt()

        nyPeriode(1.februar til 28.februar, a2, a3)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a3)

        håndterInntektsmelding(listOf(1.februar til 16.februar), beregnetInntekt = inntekt, orgnummer = a2)
        håndterInntektsmelding(listOf(1.februar til 16.februar), beregnetInntekt = inntekt, orgnummer = a3)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)

        val sammenligningsgrunnlag2 = Inntektsvurdering(
            listOf(
                sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), inntekt.repeat(12)),
                sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), inntekt.repeat(12)),
                sammenligningsgrunnlag(a3, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), inntekt.repeat(12))
            )
        )
        val sykepengegrunnlag2 = InntektForSykepengegrunnlag(
            listOf(
                grunnlag(a1, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), inntekt.repeat(3)),
                grunnlag(a2, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), inntekt.repeat(3)),
                grunnlag(a3, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), inntekt.repeat(3))
            ), emptyList()
        )
        val arbeidsforhold2 = listOf(
            Arbeidsforhold(a1, LocalDate.EPOCH, null),
            Arbeidsforhold(a2, LocalDate.EPOCH, null),
            Arbeidsforhold(a3, LocalDate.EPOCH, null)
        )
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = sammenligningsgrunnlag2,
            inntektsvurderingForSykepengegrunnlag = sykepengegrunnlag2,
            arbeidsforhold = arbeidsforhold2,
            orgnummer = a2
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        håndterYtelser(1.vedtaksperiode, orgnummer = a3)
        håndterSimulering(1.vedtaksperiode, orgnummer = a3)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a3)
        håndterUtbetalt(orgnummer = a3)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a3)

        forlengelseTilGodkjenning(1.mars, 15.mars, a2, a3)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a2)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a3)
        assertSisteTilstand(2.vedtaksperiode, TIL_UTBETALING, orgnummer = a2)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a3)
    }

    @Test
    fun `out of order periode mens senere periode revurderes til utbetaling`() {
        nyttVedtak(1.mai, 31.mai)
        forlengTilGodkjenning(1.juni, 30.juni)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        nyPeriode(1.januar til 31.januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        nullstillTilstandsendringer()

        assertForventetFeil(
            forklaring = """Forventer at periode som går fra til utbetaling til avsluttet blir sendt videre til 
                    avventer revurdering dersom tidligere periode gjenopptar behandling""",
            nå = {
                assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING)
                assertTilstander(2.vedtaksperiode, TIL_UTBETALING)
                assertSisteTilstand(3.vedtaksperiode, TIL_INFOTRYGD)

                håndterUtbetalt()

                assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING)
                assertTilstander(2.vedtaksperiode, TIL_UTBETALING, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
                assertSisteTilstand(3.vedtaksperiode, TIL_INFOTRYGD)
            },
            ønsket = {
                assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING)
                assertTilstander(2.vedtaksperiode, TIL_UTBETALING)
                assertSisteTilstand(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)

                håndterUtbetalt()

                assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING)
                assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING)
                assertSisteTilstand(3.vedtaksperiode, AVVENTER_HISTORIKK)
            }
        )
    }

    @Test
    fun `første periode i til utbetaling når det dukker opp en out of order-periode`() {
        tilGodkjent(1.mars, 31.mars, 100.prosent, 1.mars)
        nyPeriode(1.januar til 31.januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        nullstillTilstandsendringer()

        assertTilstander(1.vedtaksperiode, TIL_UTBETALING)
        håndterUtbetalt()

        assertForventetFeil(
            forklaring = """Forventer at periode som går fra til utbetaling til avsluttet blir sendt videre til 
                    avventer revurdering dersom tidligere periode gjenopptar behandling""",
            nå = {
                assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET)
                assertForkastetPeriodeTilstander(2.vedtaksperiode, TIL_INFOTRYGD)
            },
            ønsket = {
                assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET, AVVENTER_REVURDERING)
                assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)

                håndterYtelser(2.vedtaksperiode)
                håndterVilkårsgrunnlag(2.vedtaksperiode)
                håndterYtelser(2.vedtaksperiode)
                håndterSimulering(2.vedtaksperiode)
                håndterUtbetalingsgodkjenning(2.vedtaksperiode)
                håndterUtbetalt()

                assertTilstander(
                    2.vedtaksperiode,
                    AVVENTER_BLOKKERENDE_PERIODE,
                    AVVENTER_HISTORIKK,
                    AVVENTER_VILKÅRSPRØVING,
                    AVVENTER_HISTORIKK,
                    AVVENTER_SIMULERING,
                    AVVENTER_GODKJENNING,
                    TIL_UTBETALING,
                    AVSLUTTET
                )
            }
        )
    }
}