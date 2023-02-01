package no.nav.helse.spleis.e2e.revurdering

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.FeilerMedHåndterInntektsmeldingOppdelt
import no.nav.helse.Toggle
import no.nav.helse.assertForventetFeil
import no.nav.helse.august
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.inspectors.personLogg
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.person.Inntektskilde.FLERE_ARBEIDSGIVERE
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
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING
import no.nav.helse.person.TilstandType.REVURDERING_FEILET
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_2
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_4
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IT_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IT_3
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_2
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_RV_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_13
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.person.infotrygdhistorikk.PersonUtbetalingsperiode
import no.nav.helse.person.inntekt.Inntektsmelding
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.sisteBehov
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertForkastetPeriodeTilstander
import no.nav.helse.spleis.e2e.assertFunksjonellFeil
import no.nav.helse.spleis.e2e.assertInfo
import no.nav.helse.spleis.e2e.assertIngenFunksjonelleFeil
import no.nav.helse.spleis.e2e.assertIngenVarsel
import no.nav.helse.spleis.e2e.assertIngenVarsler
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.finnSkjæringstidspunkt
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.grunnlag
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterInntektsmeldingMedValidering
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
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.spleis.e2e.repeat
import no.nav.helse.spleis.e2e.sammenligningsgrunnlag
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class ReberegningAvAvsluttetUtenUtbetalingNyE2ETest : AbstractEndToEndTest() {

    @Test
    fun `omgjøre kort periode etter mottatt im - med eldre utbetalt periode`() {
        nyttVedtak(1.januar, 31.januar)

        nyPeriode(10.august til 20.august)
        håndterUtbetalingshistorikk(2.vedtaksperiode)
        håndterInntektsmelding(listOf(1.august til 16.august))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        val førsteUtbetaling = inspektør.utbetaling(0).inspektør
        inspektør.utbetaling(1).inspektør.also { utbetalingInspektør ->
            assertNotEquals(førsteUtbetaling.korrelasjonsId, utbetalingInspektør.korrelasjonsId)
            assertNotEquals(førsteUtbetaling.arbeidsgiverOppdrag.inspektør.fagsystemId(), utbetalingInspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
            assertNotEquals(førsteUtbetaling.personOppdrag.inspektør.fagsystemId(), utbetalingInspektør.personOppdrag.inspektør.fagsystemId())
            assertEquals(17.august til 20.august, utbetalingInspektør.periode)
        }
    }

    @Test
    fun `inntektsmelding på kort periode gjør at en nyere kort periode skal utbetales`() {
        håndterSykmelding(Sykmeldingsperiode(10.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(10.januar, 20.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterSykmelding(Sykmeldingsperiode(9.februar, 20.februar, 100.prosent))
        val søknadId = håndterSøknad(Sykdom(9.februar, 20.februar, 100.prosent))
        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(10.januar til 25.januar))
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_REVURDERING)
        assertNotNull(observatør.manglendeInntektsmeldingVedtaksperioder.single { it.søknadIder == setOf(søknadId) })
    }

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
    fun `infotrygd har utbetalt perioden - vi har kun arbeidsgiverperiode`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, besvart = LocalDateTime.MIN)

        håndterSykmelding(Sykmeldingsperiode(21.januar, 27.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 27.januar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, besvart = LocalDateTime.MIN)

        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT)
        håndterYtelser(2.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 27.januar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true)
        ))
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        nullstillTilstandsendringer()

        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET)
    }

    @Test
    fun `infotrygd har utbetalt perioden - vi har ingenting`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, besvart = LocalDateTime.MIN)

        håndterSykmelding(Sykmeldingsperiode(21.januar, 27.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 27.januar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, besvart = LocalDateTime.MIN)

        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT)
        håndterYtelser(2.vedtaksperiode, PersonUtbetalingsperiode(ORGNUMMER, 1.januar, 27.januar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, false)
        ))
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        nullstillTilstandsendringer()
        håndterYtelser(2.vedtaksperiode)

        assertForkastetPeriodeTilstander(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING, REVURDERING_FEILET)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, REVURDERING_FEILET)
    }

    @Test
    fun `infotrygd har utbetalt perioden - vi har ingenting - flere ag`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode, besvart = LocalDateTime.MIN, orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(1.vedtaksperiode, besvart = LocalDateTime.MIN, orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(21.januar, 27.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(21.januar, 27.januar, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(2.vedtaksperiode, besvart = LocalDateTime.MIN, orgnummer = a2)

        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT, orgnummer = a2)
        håndterYtelser(2.vedtaksperiode, PersonUtbetalingsperiode(a2, 1.januar, 27.januar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
            Inntektsopplysning(a2, 1.januar, INNTEKT, false)
        ), orgnummer = a2)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT, orgnummer = a2)
        nullstillTilstandsendringer()
        håndterYtelser(2.vedtaksperiode, orgnummer = a2)

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a1)
        assertForkastetPeriodeTilstander(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING, REVURDERING_FEILET, orgnummer = a2)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, REVURDERING_FEILET, orgnummer = a2)
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

        assertTilstander(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
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
    @FeilerMedHåndterInntektsmeldingOppdelt("💡ufullstendig validering: 'riktig' vedtaksperiode håndter inntekt. der er FF=skjæringstidspunkt. Er det OK at dette automatiseres?")
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
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertEquals(0, observatør.manglendeInntektsmeldingVedtaksperioder.size)

        assertTrue(inspektør.sykdomstidslinje[21.januar] is Dag.FriskHelgedag)
        assertTrue(inspektør.sykdomstidslinje[27.januar] is Dag.FriskHelgedag)

        håndterYtelser(2.vedtaksperiode)

        assertVarsel(RV_IM_2, 1.vedtaksperiode.filter(a1))
        assertInfo(RV_RV_1.varseltekst, 2.vedtaksperiode.filter(a1))

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_VILKÅRSPRØVING_REVURDERING)
    }

    @Test
    fun `støtter ikke omgjøring om det er utbetalt en senere periode på samme skjæringstidspunkt`() {
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
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `støtter ikke omgjøring om det er utbetalt en senere periode på nyere skjæringstidspunkt`()  {
        håndterSykmelding(Sykmeldingsperiode(19.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(18.januar, 20.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(21.januar, 3.februar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 3.februar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        nyttVedtak(1.mai, 31.mai)
        nullstillTilstandsendringer()

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(3.vedtaksperiode, AVSLUTTET)

        håndterInntektsmelding(listOf(10.januar til 20.januar, 28.januar til 1.februar))

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET)
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
        håndterYtelser(2.vedtaksperiode)

        assertIngenVarsler(1.vedtaksperiode.filter(a1))
        assertVarsel(RV_IV_2, 2.vedtaksperiode.filter(a1))

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_VILKÅRSPRØVING_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING)
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

        assertIngenVarsler(1.vedtaksperiode.filter(a1))
        assertInfo(RV_RV_1.varseltekst, 2.vedtaksperiode.filter(a1))
        // assertNoWarnings(2.vedtaksperiode.filter(a1))
        assertIngenVarsler(3.vedtaksperiode.filter(a1))

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_VILKÅRSPRØVING_REVURDERING)
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
        håndterVilkårsgrunnlag(3.vedtaksperiode)
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)

        nullstillTilstandsendringer()

        håndterInntektsmelding(listOf(10.januar til 25.januar))
        håndterYtelser(2.vedtaksperiode)

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_VILKÅRSPRØVING_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE)
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
        val søknadId1 = håndterSøknad(Sykdom(23.januar, 25.januar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(29.januar, 29.januar, 100.prosent))
        val søknadId2 = håndterSøknad(Sykdom(29.januar, 29.januar, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode)

        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(5.januar til 20.januar))

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_REVURDERING)
        assertEquals(2, observatør.manglendeInntektsmeldingVedtaksperioder.size)
        assertEquals(setOf(søknadId1), observatør.manglendeInntektsmeldingVedtaksperioder.first().søknadIder)
        assertEquals(setOf(søknadId2), observatør.manglendeInntektsmeldingVedtaksperioder.last().søknadIder)
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
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_REVURDERING)
    }

    @Test
    @FeilerMedHåndterInntektsmeldingOppdelt("✅AventerIm->AUU utenom AvventerBlokkerende")
    fun `gjenopptar behandling på neste periode dersom inntektsmelding treffer avsluttet periode`() {
        håndterSykmelding(Sykmeldingsperiode(12.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 20.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(21.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(5.januar til 20.januar))
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
    }

    @Test
    @FeilerMedHåndterInntektsmeldingOppdelt("✅ Burde være ok med ekstra AUU-transisjoner her")
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
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    @FeilerMedHåndterInntektsmeldingOppdelt("✅ Burde være ok med ekstra AUU-transisjoner her")
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
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_BLOKKERENDE_PERIODE)
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
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)
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

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a1)
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
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a2)

        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(3.januar til 19.januar), orgnummer = a2)

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        assertEquals("FLERE_ARBEIDSGIVERE", person.personLogg.sisteBehov(Godkjenning).detaljer()["inntektskilde"] as? String)
        assertEquals(FLERE_ARBEIDSGIVERE, inspektør(a1).inntektskilde(1.vedtaksperiode))

        assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_VILKÅRSPRØVING_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)
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
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a2)

        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a2)
        assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_REVURDERING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)

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

        assertEquals("FLERE_ARBEIDSGIVERE", person.personLogg.sisteBehov(Godkjenning).detaljer()["inntektskilde"] as? String)
        assertEquals(FLERE_ARBEIDSGIVERE, inspektør(a1).inntektskilde(1.vedtaksperiode))

        assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_VILKÅRSPRØVING_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_REVURDERING, orgnummer = a2)
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
    fun `arbeidsgiver 1 er utenfor arbeidsgiverperioden, men ikke arbeidsgiver 2`() {
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
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)
        assertTilstander(3.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a1)
        assertTilstander(4.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a1)
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
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)
        assertTilstander(3.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a1)
        assertTilstander(4.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a1)
    }

    @Test
    fun `infotrygd har plutselig utbetalt`() {
        håndterSykmelding(Sykmeldingsperiode(10.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(10.januar, 20.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, besvart = LocalDateTime.now().minusHours(24))
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode, besvart = LocalDateTime.now().minusHours(24))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, besvart = LocalDateTime.now().minusHours(24))
        håndterSimulering(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)

        nullstillTilstandsendringer()
        håndterPåminnelse(1.vedtaksperiode, påminnetTilstand = AVVENTER_GODKJENNING_REVURDERING)
        val utbetalinger = listOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 20.januar, 100.prosent, INNTEKT))
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true))
        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalinger.toTypedArray(), inntektshistorikk = inntektshistorikk)
        håndterYtelser(1.vedtaksperiode)

        assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING)
        assertVarsel(RV_IT_3, 1.vedtaksperiode.filter())
        assertIngenVarsel(RV_IT_1, 1.vedtaksperiode.filter())
    }

    @Test
    fun `infotrygd har plutselig utbetalt, flere arbeidsgivere`() {
        håndterSykmelding(Sykmeldingsperiode(10.januar, 20.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(10.januar, 20.januar, 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode, besvart = LocalDateTime.now().minusHours(24), orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(10.januar, 20.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(10.januar, 20.januar, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(1.vedtaksperiode, besvart = LocalDateTime.now().minusHours(24), orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)

        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, besvart = LocalDateTime.now().minusHours(24), orgnummer = a1)
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, besvart = LocalDateTime.now().minusHours(24), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)

        nullstillTilstandsendringer()
        håndterPåminnelse(1.vedtaksperiode, påminnetTilstand = AVVENTER_GODKJENNING_REVURDERING)
        val utbetalinger = listOf(ArbeidsgiverUtbetalingsperiode(a1, 17.januar, 20.januar, 100.prosent, INNTEKT))
        val inntektshistorikk = listOf(Inntektsopplysning(a1, 1.januar, INNTEKT, true))
        håndterUtbetalingshistorikk(1.vedtaksperiode, *utbetalinger.toTypedArray(), inntektshistorikk = inntektshistorikk)
        håndterYtelser(1.vedtaksperiode)

        assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING)
        assertVarsel(RV_IT_3, 1.vedtaksperiode.filter())
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
                assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            },
            ønsket = {
                assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING)
            }
        )
        assertForkastetPeriodeTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, TIL_INFOTRYGD)
    }

    @Test
    fun `endrer arbeidsgiverperiode etter igangsatt revurdering`() = Toggle.HåndterInntektsmeldingOppdelt.enable {
        val forMyeInntekt = INNTEKT * 1.2
        val riktigInntekt = INNTEKT

        håndterSykmelding(Sykmeldingsperiode(5.februar, 11.februar, 100.prosent))
        håndterSøknad(Sykdom(5.februar, 11.februar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterSykmelding(Sykmeldingsperiode(12.februar, 20.februar, 100.prosent))
        håndterSøknad(Sykdom(12.februar, 20.februar, 100.prosent))
        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(
            24.januar til 8.februar
        ), beregnetInntekt = forMyeInntekt)
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterInntektsmelding(listOf(
            22.januar til 6.februar
        ), beregnetInntekt = riktigInntekt)
        assertEquals(24.januar til 11.februar, inspektør.periode(1.vedtaksperiode))
        assertEquals("UUUGG UUUUUGG SSSSSHH SSSSSHH SS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        val vilkårsgrunnlag = inspektør.vilkårsgrunnlag(2.vedtaksperiode)

        assertInfo(RV_RV_1.varseltekst, 1.vedtaksperiode.filter(ORGNUMMER))
        assertInfo(RV_RV_1.varseltekst, 2.vedtaksperiode.filter(ORGNUMMER))

        assertNotNull(vilkårsgrunnlag)

        val sykepengegrunnlagInspektør = vilkårsgrunnlag.inspektør.sykepengegrunnlag.inspektør

        sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
            assertEquals(riktigInntekt, it.inntektsopplysning.inspektør.beløp)
            assertEquals(Inntektsmelding::class, it.inntektsopplysning::class)
        }

        assertEquals(riktigInntekt, vilkårsgrunnlag.inspektør.sykepengegrunnlag.inspektør.sykepengegrunnlag)

        val utbetaling = inspektør.gjeldendeUtbetalingForVedtaksperiode(2.vedtaksperiode)
        val utbetalingInspektør = utbetaling.inspektør

        val førsteUtbetalingsdag = utbetalingInspektør.utbetalingstidslinje[7.februar]
        assertEquals(riktigInntekt, førsteUtbetalingsdag.økonomi.inspektør.aktuellDagsinntekt)
        assertEquals(riktigInntekt, førsteUtbetalingsdag.økonomi.inspektør.arbeidsgiverRefusjonsbeløp)
        assertEquals(24.januar til 11.februar, inspektør.periode(1.vedtaksperiode))

        assertTilstander(
            1.vedtaksperiode,
            AVSLUTTET_UTEN_UTBETALING,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING
        )
        assertTilstander(
            2.vedtaksperiode,
            AVSLUTTET_UTEN_UTBETALING,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_VILKÅRSPRØVING_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING
        )
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
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `revvurdering med ghost`() {
        val beregnetInntektA1 = 31000.månedlig

        håndterSykmelding(Sykmeldingsperiode(10.januar, 25.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(10.januar, 25.januar, 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = beregnetInntektA1, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), beregnetInntektA1.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 32000.månedlig.repeat(3))
        )

        val arbeidsforhold = listOf(
            Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
            Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null)
        )
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), beregnetInntektA1.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 32000.månedlig.repeat(12))
                )
            ),
            orgnummer = a1,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = inntekter, arbeidsforhold = emptyList()),
            arbeidsforhold = arbeidsforhold
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        inspektør(a1).sisteUtbetalingUtbetalingstidslinje()[17.januar].let {
            assertEquals(1063.daglig, it.økonomi.inspektør.arbeidsgiverbeløp)
            assertEquals(0.daglig, it.økonomi.inspektør.personbeløp)
            assertEquals(beregnetInntektA1, it.økonomi.inspektør.aktuellDagsinntekt)
        }

        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVSLUTTET_UTEN_UTBETALING,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_VILKÅRSPRØVING_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING
        )
    }

    @Test
    fun `revurdere etter at én arbeidsgiver har blitt til to`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode, orgnummer = a1,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, 1.januar, 31000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, 1.januar, 31000.månedlig.repeat(12)),
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                listOf(
                    grunnlag(a1, 1.januar, 31000.månedlig.repeat(3)),
                    grunnlag(a2, 1.januar, 31000.månedlig.repeat(3)),
                ), arbeidsforhold = emptyList()
            ),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH)
            )
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        forlengVedtak(1.februar, 28.februar, orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(10.mars, 22.mars, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(10.mars, 22.mars, 100.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)

        håndterOverstyrTidslinje(listOf(
            ManuellOverskrivingDag(27.februar, Dagtype.Feriedag)
        ), orgnummer = a1)

        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterSimulering(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        nullstillTilstandsendringer()

        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstander(3.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)
    }

    @Test
    fun `Skal ikke forkaste vedtaksperioder i revurdering som kun består av AUU`() {
        håndterSykmelding(Sykmeldingsperiode(10.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(10.januar, 20.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(21.januar, 25.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 25.januar, 100.prosent))

        håndterInntektsmelding(listOf(2.januar til 17.januar))
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)

        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent))

        assertFunksjonellFeil(RV_SØ_13, 1.vedtaksperiode.filter())
        assertForkastetPeriodeTilstander(3.vedtaksperiode, START, TIL_INFOTRYGD)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `Skal ikke forkaste vedtaksperioder i revurdering hvor en vedtaksperiode har utbetaling`() {
        håndterSykmelding(Sykmeldingsperiode(10.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(10.januar, 20.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(21.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 31.januar, 100.prosent))

        håndterInntektsmelding(listOf(5.januar til 20.januar))
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)

        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        assertEquals(5.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertEquals(5.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))

        håndterInntektsmelding(listOf(2.januar til 17.januar), beregnetInntekt = INNTEKT*1.2)
        assertEquals(5.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertEquals(5.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)


        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent))
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        assertForkastetPeriodeTilstander(3.vedtaksperiode, START, TIL_INFOTRYGD)
        assertIngenFunksjonelleFeil(2.vedtaksperiode.filter())
        håndterYtelser(2.vedtaksperiode)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)

        assertEquals(5.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertEquals(5.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
        assertFunksjonellFeil(RV_SØ_13, 1.vedtaksperiode.filter())
        assertVarsel(RV_IM_4, 2.vedtaksperiode.filter())
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `Inntektsmelding oppgir tidligere egenmeldingsdager før kort periode - omgjøring?`() {
        nyPeriode(4.desember(2022) til 11.desember(2022))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        nyPeriode(14.desember(2022) til 8.januar(2023))
        håndterInntektsmelding(listOf(4.november(2022) til 19.november(2022)), førsteFraværsdag = 14.desember(2022))

        assertEquals("UGG UUUUUGG UUUUUG? ??????? ??????H SSSSSHH ??SSSHH SSSSSHH SSSSSHH SSSSSHH", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())

        assertForventetFeil(
            forklaring = "1.vedtaksperiode burde omgjøres her pga de implisitte egenemeldingsdagene 4.november til 19.november, som er 15 dager før den korte perioden",
            nå = {
                assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
                assertSisteTilstand(2.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
                håndterVilkårsgrunnlag(2.vedtaksperiode)
                assertThrows<IllegalStateException>("Fant ikke vilkårsgrunnlag for 2022-12-05. Må ha et vilkårsgrunnlag for å legge til utbetalingsopplysninger. Har vilkårsgrunnlag på skjæringstidspunktene [2022-12-14]") {
                    håndterYtelser(2.vedtaksperiode)
                }
            },
            ønsket = {
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
                assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            }
        )
    }
}
