package no.nav.helse.spleis.e2e.søknad

import no.nav.helse.FeilerMedHåndterInntektsmeldingOppdelt
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.Varselkode.RV_SØ_2
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikk
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class ForeldetSøknadE2ETest : AbstractEndToEndTest() {
    @Test
    fun `forledet søknad`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), mottatt = 1.januar(2019).atStartOfDay())
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.januar(2019))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        assertVarsel(RV_SØ_2)
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
    }

    @Test
    fun `forledet søknad med inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), mottatt = 1.januar(2019).atStartOfDay())
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.januar(2019))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        assertVarsel(RV_SØ_2)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning()
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING,
            AVSLUTTET
        )
    }

    @Test
    fun `foreldet dag utenfor agp -- må gå til manuell`() {
        håndterSykmelding(Sykmeldingsperiode(15.januar, 16.februar, 100.prosent))
        håndterSøknad(
            Sykdom(15.januar, 16.februar, 100.prosent),
            Ferie(1.februar, 16.februar),
            sendtTilNAVEllerArbeidsgiver = 1.mai
        )
        håndterInntektsmelding(listOf(15.januar til 30.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        assertEquals(Dag.ForeldetSykedag::class, inspektør.vedtaksperiodeSykdomstidslinje(1.vedtaksperiode)[31.januar]::class)
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING,
            AVSLUTTET
        )
    }
    @Test
    @FeilerMedHåndterInntektsmeldingOppdelt("AventerIm->AUU utenom AvventerBlokkerende")
    fun `foreldet dag innenfor agp -- kan lukkes uten manuell behandling`() {
        håndterSykmelding(Sykmeldingsperiode(16.januar, 16.februar, 100.prosent))
        håndterSøknad(
            Sykdom(16.januar, 16.februar, 100.prosent),
            Ferie(1.februar, 16.februar),
            sendtTilNAVEllerArbeidsgiver = 1.mai
        )
        håndterInntektsmelding(listOf(16.januar til 31.januar))
        assertEquals(Dag.ForeldetSykedag::class, inspektør.vedtaksperiodeSykdomstidslinje(1.vedtaksperiode)[31.januar]::class)
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVSLUTTET_UTEN_UTBETALING
        )
    }

    @Test
    fun `forledet søknad ved forlengelse`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), mottatt = 1.februar(2019).atStartOfDay())
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.februar(2019))
        håndterUtbetalingshistorikk(2.vedtaksperiode)
        assertVarsel(RV_SØ_2)
        assertTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK)
    }

    @Test
    fun `foreldet søknad forlenger annen foreldet søknad - deler korrelasjonsId`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 19.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(20.januar, 31.januar, 100.prosent))

        // foreldet søknad :(
        håndterSøknad(Sykdom(1.januar, 19.januar, 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.mai)
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)

        // foreldet søknad :(
        håndterSøknad(Sykdom(20.januar, 31.januar, 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.mai)
        håndterYtelser(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        val førsteUtbetaling = inspektør.utbetaling(0).inspektør
        val andreUtbetaling = inspektør.utbetaling(1).inspektør

        assertEquals(førsteUtbetaling.korrelasjonsId, andreUtbetaling.korrelasjonsId)
        assertEquals(0, førsteUtbetaling.arbeidsgiverOppdrag.size)
        assertEquals(0, førsteUtbetaling.personOppdrag.size)
        assertEquals(0, andreUtbetaling.arbeidsgiverOppdrag.size)
        assertEquals(0, andreUtbetaling.personOppdrag.size)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
    }

    @Test
    fun `foreldet søknad etter annen foreldet søknad - samme arbeidsgiverperiode - deler korrelasjonsId`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 19.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(24.januar, 31.januar, 100.prosent))

        // foreldet søknad :(
        håndterSøknad(Sykdom(1.januar, 19.januar, 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.mai)
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)

        // foreldet søknad :(
        håndterSøknad(Sykdom(24.januar, 31.januar, 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.mai)
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 24.januar)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        val førsteUtbetaling = inspektør.utbetaling(0).inspektør
        val andreUtbetaling = inspektør.utbetaling(1).inspektør

        assertEquals(førsteUtbetaling.korrelasjonsId, andreUtbetaling.korrelasjonsId)
        assertEquals(0, førsteUtbetaling.arbeidsgiverOppdrag.size)
        assertEquals(0, førsteUtbetaling.personOppdrag.size)
        assertEquals(0, andreUtbetaling.arbeidsgiverOppdrag.size)
        assertEquals(0, andreUtbetaling.personOppdrag.size)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
    }

    @Test
    fun `foreldet søknad etter annen foreldet søknad - ulike arbeidsgiverperioder - deler ikke korrelasjonsId`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 19.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(19.februar, 12.mars, 100.prosent))

        // foreldet søknad :(
        håndterSøknad(Sykdom(1.januar, 19.januar, 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.juni)
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)

        // foreldet søknad :(
        håndterSøknad(Sykdom(19.februar, 12.mars, 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.juli)
        håndterInntektsmelding(listOf(19.februar til 6.mars))
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        val førsteUtbetaling = inspektør.utbetaling(0).inspektør
        val andreUtbetaling = inspektør.utbetaling(1).inspektør

        assertNotEquals(førsteUtbetaling.korrelasjonsId, andreUtbetaling.korrelasjonsId)
        assertEquals(0, førsteUtbetaling.arbeidsgiverOppdrag.size)
        assertEquals(0, førsteUtbetaling.personOppdrag.size)
        assertEquals(0, andreUtbetaling.arbeidsgiverOppdrag.size)
        assertEquals(0, andreUtbetaling.personOppdrag.size)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
    }
}
