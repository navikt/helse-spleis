package no.nav.helse.spleis.e2e

import no.nav.helse.april
import no.nav.helse.februar
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_UTBETALING
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class DeleGrunnlagsdataTest : AbstractEndToEndTest() {

    @Test
    fun `vilkårsgrunnlag deles med påfølgende tilstøtende perioder`() {
        håndterSykmelding(januar)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
        håndterSykmelding(Sykmeldingsperiode(5.april, 30.april))
        håndterSøknad(januar)
        håndterSøknad(februar)
        håndterSøknad(mars)
        håndterSøknad(5.april til 30.april)
        val inntektsmelding1Id = håndterArbeidsgiveropplysninger(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        val inntektsmelding2Id = håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            førsteFraværsdag = 5.april
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()
        håndterVilkårsgrunnlag(4.vedtaksperiode)
        håndterYtelser(4.vedtaksperiode)
        håndterSimulering(4.vedtaksperiode)
        håndterUtbetalingsgodkjenning(4.vedtaksperiode)
        håndterUtbetalt()

        assertNotNull(inspektør.vilkårsgrunnlag(1.vedtaksperiode))
        assertSame(inspektør.vilkårsgrunnlag(1.vedtaksperiode), inspektør.vilkårsgrunnlag(2.vedtaksperiode))
        assertSame(inspektør.vilkårsgrunnlag(1.vedtaksperiode), inspektør.vilkårsgrunnlag(3.vedtaksperiode))
        assertNotSame(inspektør.vilkårsgrunnlag(3.vedtaksperiode), inspektør.vilkårsgrunnlag(4.vedtaksperiode))
        assertTrue(inntektsmelding1Id in inspektør.hendelseIder(1.vedtaksperiode))
        assertTrue(inntektsmelding1Id in inspektør.hendelseIder(2.vedtaksperiode))
        assertTrue(inntektsmelding1Id in inspektør.hendelseIder(3.vedtaksperiode))
        assertTrue(inntektsmelding2Id in inspektør.hendelseIder(4.vedtaksperiode))
    }

    @Test
    fun `vilkårsgrunnlag deles med foregående`() {
        håndterSykmelding(januar)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
        håndterSykmelding(Sykmeldingsperiode(5.april, 30.april))
        håndterSøknad(januar)
        håndterArbeidsgiveropplysninger(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterSøknad(februar)
        håndterSøknad(mars)
        håndterSøknad(5.april til 30.april)

        assertNotNull(inspektør.vilkårsgrunnlag(1.vedtaksperiode))
        assertSame(inspektør.vilkårsgrunnlag(1.vedtaksperiode), inspektør.vilkårsgrunnlag(2.vedtaksperiode))
        assertSame(inspektør.vilkårsgrunnlag(1.vedtaksperiode), inspektør.vilkårsgrunnlag(3.vedtaksperiode))
        assertNull(inspektør.vilkårsgrunnlag(4.vedtaksperiode))
    }

    @Test
    fun `inntektsmelding bryter ikke opp forlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
        håndterSøknad(februar)
        håndterArbeidsgiveropplysninger(listOf(Periode(18.januar, 1.februar)), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
        håndterSøknad(mars)
        nullstillTilstandsendringer()
        håndterInntektsmelding(
            listOf(Periode(18.januar, 1.februar)),
            førsteFraværsdag = 4.mars
        )

        assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING)

        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        håndterYtelser(2.vedtaksperiode)
        assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
        assertVarsler(listOf(), 1.vedtaksperiode.filter())
        assertVarsler(listOf(), 2.vedtaksperiode.filter())
        assertSame(inspektør.vilkårsgrunnlag(1.vedtaksperiode), inspektør.vilkårsgrunnlag(2.vedtaksperiode))
        assertEquals(18.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertEquals(18.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
    }

    @Test
    fun `setter ikke inntektsmeldingId flere ganger`() {
        håndterSykmelding(Sykmeldingsperiode(20.februar, 28.februar))
        håndterSøknad(20.februar til 28.februar)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
        val søknadId = håndterSøknad(mars)

        val inntektsmeldingId = håndterInntektsmelding(listOf(Periode(20.februar, 7.mars)))
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVSLUTTET_UTEN_UTBETALING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVSLUTTET_UTEN_UTBETALING
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
        assertEquals(2, inspektør.hendelseIder(2.vedtaksperiode).size)
        assertTrue(inspektør.hendelseIder(2.vedtaksperiode).containsAll(listOf(søknadId, inntektsmeldingId)))
    }
}
