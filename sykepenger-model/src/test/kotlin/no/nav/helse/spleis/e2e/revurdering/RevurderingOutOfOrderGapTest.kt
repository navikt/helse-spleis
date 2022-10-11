package no.nav.helse.spleis.e2e.revurdering

import java.time.LocalDate
import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import no.nav.helse.april
import no.nav.helse.assertForventetFeil
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
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
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.Varselkode.RV_OO_1
import no.nav.helse.person.Varselkode.RV_OO_2
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertForkastetPeriodeTilstander
import no.nav.helse.spleis.e2e.assertFunksjonellFeil
import no.nav.helse.spleis.e2e.assertInfo
import no.nav.helse.spleis.e2e.assertIngenInfo
import no.nav.helse.spleis.e2e.assertIngenVarsel
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertUtbetalingsbeløp
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.finnSkjæringstidspunkt
import no.nav.helse.spleis.e2e.forlengTilGodkjenning
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.forlengelseTilGodkjenning
import no.nav.helse.spleis.e2e.grunnlag
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.håndterPåminnelse
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikk
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.nyeVedtak
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.spleis.e2e.repeat
import no.nav.helse.spleis.e2e.sammenligningsgrunnlag
import no.nav.helse.spleis.e2e.tilGodkjenning
import no.nav.helse.spleis.e2e.tilGodkjent
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EnableToggle(Toggle.RevurderOutOfOrder::class)
internal class RevurderingOutOfOrderGapTest : AbstractEndToEndTest() {

    @Test
    fun `out-of-order søknad medfører revurdering -- AvventerVilksprøvingRevurdering`() {
        nyPeriode(1.februar til 10.februar)
        håndterUtbetalingshistorikk(1.vedtaksperiode, besvart = LocalDate.EPOCH.atStartOfDay())
        håndterInntektsmelding(arbeidsgiverperioder = listOf(20.januar til 4.februar), førsteFraværsdag = 20.januar)
        håndterYtelser(1.vedtaksperiode, besvart = LocalDate.EPOCH.atStartOfDay())
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING_REVURDERING)

        nyPeriode(1.januar til 18.januar)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)

        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        assertUtbetalingsbeløp(
            1.vedtaksperiode,
            forventetArbeidsgiverbeløp = 1431,
            forventetArbeidsgiverRefusjonsbeløp = 1431,
            subset = 5.februar til 10.februar
        )
    }

    @Test
    fun `out of order periode trigger revurdering`() {
        nyttVedtak(1.mai, 31.mai)
        forlengVedtak(1.juni, 30.juni)
        nullstillTilstandsendringer()
        nyttVedtak(1.januar, 31.januar)
        assertSisteTilstand(3.vedtaksperiode, AVSLUTTET)

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
    fun `out of order periode uten utbetaling trigger ikke revurdering`() {
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
            AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            AVSLUTTET
        )
    }

    @Test
    fun `out of order periode uten utbetaling trigger ikke revurdering -- flere ag`() {
        nyeVedtak(1.mai, 31.mai, a1, a2)
        nullstillTilstandsendringer()
        nyPeriode(1.januar til 15.januar, a1)
        håndterUtbetalingshistorikk(2.vedtaksperiode, orgnummer = a1)
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVSLUTTET_UTEN_UTBETALING,
            orgnummer = a1
        )
        assertTilstander(
            1.vedtaksperiode,
            AVSLUTTET,
            orgnummer = a1
        )
        assertTilstander(
            1.vedtaksperiode,
            AVSLUTTET,
            orgnummer = a2
        )
    }

    @Test
    fun `Burde revurdere utbetalt periode dersom det kommer en eldre periode fra en annen AG`() = Toggle.RevurderOutOfOrder.enable {
        nyttVedtak(1.mars, 31.mars, orgnummer = a2)
        nyPeriode(1.januar til 31.januar, a1)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a1)
    }

    @Test
    fun `out of order som overlapper med eksisterende -- flere ag`() {
        nyttVedtak(1.mars, 31.mars, orgnummer = a2)
        nyPeriode(20.februar til 15.mars, a1)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD, orgnummer = a1)
    }

    @Test
    fun `out of order som overlapper med eksisterende`() {
        nyttVedtak(1.mars, 31.mars, orgnummer = a1)
        nyPeriode(20.februar til 15.mars, a1)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(2.vedtaksperiode, TIL_INFOTRYGD, orgnummer = a1)
    }

    @Test
    fun `To arbeidsgivere gikk inn i en bar - og første arbeidsgiver ble ferdig behandlet før vi mottok sykmelding på neste arbeidsgiver`() = Toggle.RevurderOutOfOrder.enable {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 30000.månedlig, orgnummer = a1)

        val inntektsvurdering = Inntektsvurdering(
            listOf(
                sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(12)),
                sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 35000.månedlig.repeat(12))
            )
        )
        val ivForSykepengegrunnlag = InntektForSykepengegrunnlag(
            inntekter = listOf(
                grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(3)),
                grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 35000.månedlig.repeat(3))
            )
            , arbeidsforhold = emptyList()
        )
        val arbeidsforhold = listOf(Arbeidsforhold(a1, LocalDate.EPOCH), Arbeidsforhold(a2, LocalDate.EPOCH))

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            orgnummer = a1,
            inntektsvurdering = inntektsvurdering,
            inntektsvurderingForSykepengegrunnlag = ivForSykepengegrunnlag,
            arbeidsforhold = arbeidsforhold
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)
        assertUtbetalingsbeløp(
            1.vedtaksperiode,
            forventetArbeidsgiverbeløp = 997,
            forventetArbeidsgiverRefusjonsbeløp = 1385,
            orgnummer = a1,
            subset = 17.januar til 31.januar
        )

        assertEquals(1, inspektør(a1).arbeidsgiverOppdrag.size)
        assertEquals(0, inspektør(a2).arbeidsgiverOppdrag.size)

        nullstillTilstandsendringer()
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a2)
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 30000.månedlig, orgnummer = a2)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt()

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            orgnummer = a2
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertUtbetalingsbeløp(
            1.vedtaksperiode,
            forventetArbeidsgiverbeløp = 1081,
            forventetArbeidsgiverRefusjonsbeløp = 1385,
            orgnummer = a1,
            subset = 17.januar til 31.januar
        )
        assertUtbetalingsbeløp(
            1.vedtaksperiode,
            forventetArbeidsgiverbeløp = 1080,
            forventetArbeidsgiverRefusjonsbeløp = 1385,
            orgnummer = a2,
            subset = 17.januar til 31.januar
        )
        assertEquals(2, inspektør(a1).utbetalinger.size)
        assertEquals(1, inspektør(a2).utbetalinger.size)
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

    @Test
    fun `kort periode, lang periode kommer out of order - kort periode trenger ikke å sendes til saksbehandler`() {
        nyPeriode(1.mars til 16.mars)
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        nyPeriode(1.januar til 31.januar)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        assertIngenInfo("Revurdering førte til at sykefraværstilfellet trenger inntektsmelding", 1.vedtaksperiode.filter())

        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
    }

    @Test
    fun `kort periode, lang periode kommer out of order og fører til utbetaling på kort periode som nå trenger IM`() {
        nyPeriode(1.mars til 16.mars)
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        nyPeriode(1.februar til 25.februar)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)

        assertInfo("Revurdering førte til at sykefraværstilfellet trenger inntektsmelding", 1.vedtaksperiode.filter())

        håndterInntektsmelding(listOf(1.februar til 16.februar))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        håndterInntektsmelding(listOf(1.februar til 16.februar), førsteFraværsdag = 1.mars)
        håndterYtelser(1.vedtaksperiode)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING_REVURDERING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)

        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
    }

    @Test
    fun `out-of-order med error skal ikke medføre revurdering`() {
        nyttVedtak(1.mars, 31.mars)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), andreInntektskilder = listOf(
            Søknad.Inntektskilde(
                true,
                "FRILANSER"
            )
        ))
        assertFunksjonellFeil("Søknaden inneholder andre inntektskilder enn ANDRE_ARBEIDSFORHOLD", 2.vedtaksperiode.filter())
        assertForkastetPeriodeTilstander(2.vedtaksperiode, START, TIL_INFOTRYGD)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
    }

    @Test
    fun `out-of-order som fører til nådd maksdato skal avslå riktige dager`() {
        createKorttidsPerson(UNG_PERSON_FNR_2018, 1.januar(1992), maksSykedager = 16)

        nyttVedtak(1.januar, 30.januar)
        assertEquals(6, inspektør.gjenståendeSykedager(1.vedtaksperiode))

        nyttVedtak(1.mai, 24.mai)
        assertEquals(0, inspektør.gjenståendeSykedager(2.vedtaksperiode))

        nyttVedtak(1.mars, 26.mars)
        håndterYtelser(2.vedtaksperiode)

        //Når out-of-order perioden for mars kommer inn, så er det dager i mai som skal bli avvist pga maksdato
        assertEquals(0, inspektør.gjenståendeSykedager(3.vedtaksperiode))
        assertEquals(0, inspektør.utbetalingstidslinjer(3.vedtaksperiode).inspektør.avvistDagTeller)
        assertEquals(6, inspektør.utbetalingstidslinjer(2.vedtaksperiode).inspektør.avvistDagTeller)
    }

    @Test
    fun `Warning ved out-of-order - én warning for perioden som trigger out-of-order, én warning for de som blir påvirket av out-of-order`() {
        nyttVedtak(1.mars, 31.mars)
        forlengVedtak(1.april, 30.april)
        forlengVedtak(1.mai, 31.mai)

        nyttVedtak(1.januar, 31.januar)

        assertVarsel(RV_OO_1, 4.vedtaksperiode.filter())
        assertIngenVarsel(RV_OO_2, 4.vedtaksperiode.filter())
        assertVarsel(RV_OO_2, 1.vedtaksperiode.filter())
        assertVarsel(RV_OO_2, 2.vedtaksperiode.filter())
        assertVarsel(RV_OO_2, 3.vedtaksperiode.filter())
    }

    @Test
    fun `Warning ved out-of-order - dukker ikke opp i revurderinger som ikke er out-of-order`() {
        nyttVedtak(1.mars, 31.mars)
        forlengVedtak(1.april, 30.april)
        forlengVedtak(1.mai, 31.mai)

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.mars, Dagtype.Sykedag, 50)))

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)

        assertIngenVarsel(RV_OO_1, 1.vedtaksperiode.filter())
        assertIngenVarsel(RV_OO_1, 2.vedtaksperiode.filter())
        assertIngenVarsel(RV_OO_1, 3.vedtaksperiode.filter())
    }

    @Test
    fun `Out of order kastes ut når det finnes en forkastet periode senere i tid`() {
        tilGodkjenning(1.februar, 25.februar, ORGNUMMER)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, false)
        assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)

        nyPeriode(1.januar til 25.januar)
        assertSisteTilstand(2.vedtaksperiode, TIL_INFOTRYGD)
    }

    @Test
    fun `Korte perioder skal ikke revurderes dersom de forblir innenfor AGP`() {
        nyPeriode(1.mars til 10.mars)
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        nyPeriode(11.mars til 16.mars)
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        nyttVedtak(1.januar, 31.januar)

        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        assertVarsel(RV_OO_1, 3.vedtaksperiode.filter())
        assertIngenVarsel(RV_OO_1, 2.vedtaksperiode.filter())
        assertIngenVarsel(RV_OO_1, 1.vedtaksperiode.filter())
    }

    @Test
    fun `Out of order gjør at AUU revurderes fordi de ikke lenger er innen AGP - ber om inntektsmelding`() {
        nyPeriode(1.mars til 10.mars)
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        nyPeriode(11.mars til 16.mars)
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        nyttVedtak(1.februar, 25.februar)

        assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)

        assertEquals(2, observatør.manglendeInntektsmeldingVedtaksperioder.size) // For 1. februar og 1.mars
        assertInfo("Revurdering førte til at sykefraværstilfellet trenger inntektsmelding", 1.vedtaksperiode.filter())

        håndterInntektsmelding(listOf(1.februar til 16.februar), førsteFraværsdag = 1.mars)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING)

        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
    }

    @Test
    fun `Out of order gjør at senere periode revurderes - ber ikke om ny inntektsmelding`() {
        nyttVedtak(1.mars, 31.mars)

        nyPeriode(1.februar til 25.februar)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)

        assertEquals(2, observatør.manglendeInntektsmeldingVedtaksperioder.size)
        assertIngenInfo("Revurdering førte til at sykefraværstilfellet trenger inntektsmelding", 1.vedtaksperiode.filter())
    }

    @Test
    fun `Ber om inntektsmelding ved påminnelse i AvventerRevurdering ved manglende IM`() {
        nyPeriode(1.mars til 15.mars)
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        nyttVedtak(1.februar, 20.februar)
        assertEquals(2, observatør.manglendeInntektsmeldingVedtaksperioder.size)
        assertEquals(2, observatør.trengerIkkeInntektsmeldingVedtaksperioder.size)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
        håndterPåminnelse(1.vedtaksperiode, AVVENTER_REVURDERING)
        assertEquals(3, observatør.manglendeInntektsmeldingVedtaksperioder.size)
    }

    @Test
    fun `Ber ikke om inntektsmelding ved påminnelse i AvventerRevurdering når vi har nødvendig IM`() {
        nyttVedtak(1.mars, 31.mars)

        nyPeriode(1.januar til 31.januar)
        assertEquals(2, observatør.manglendeInntektsmeldingVedtaksperioder.size)
        assertEquals(1, observatør.trengerIkkeInntektsmeldingVedtaksperioder.size)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
        håndterPåminnelse(1.vedtaksperiode, AVVENTER_REVURDERING)
        assertEquals(2, observatør.manglendeInntektsmeldingVedtaksperioder.size)
    }
}