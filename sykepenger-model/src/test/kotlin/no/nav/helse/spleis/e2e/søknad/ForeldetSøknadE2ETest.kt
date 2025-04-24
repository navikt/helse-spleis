package no.nav.helse.spleis.e2e.søknad

import no.nav.helse.februar
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
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_2
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.håndterArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
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
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), mottatt = 1.januar(2019).atStartOfDay())
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.januar(2019))
        assertVarsel(RV_SØ_2, 1.vedtaksperiode.filter())
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING)
    }

    @Test
    fun `forledet søknad med inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), mottatt = 1.januar(2019).atStartOfDay())
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.januar(2019))
        håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        assertVarsel(RV_SØ_2, 1.vedtaksperiode.filter())
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning()
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING,
            AVSLUTTET
        )
    }

    @Test
    fun `foreldet dag utenfor agp -- må gå til manuell`() {
        håndterSykmelding(Sykmeldingsperiode(15.januar, 16.februar))
        håndterSøknad(
            Sykdom(15.januar, 16.februar, 100.prosent),
            Ferie(1.februar, 16.februar),
            sendtTilNAVEllerArbeidsgiver = 1.mai
        )
        håndterArbeidsgiveropplysninger(listOf(15.januar til 30.januar), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        assertVarsel(RV_SØ_2, 1.vedtaksperiode.filter())
        assertEquals(Dag.ForeldetSykedag::class, inspektør.vedtaksperiodeSykdomstidslinje(1.vedtaksperiode)[31.januar]::class)
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING,
            AVSLUTTET
        )
    }

    @Test
    fun `foreldet dag innenfor agp -- kan lukkes uten manuell behandling`() {
        håndterSykmelding(Sykmeldingsperiode(16.januar, 16.februar))
        håndterSøknad(
            Sykdom(16.januar, 16.februar, 100.prosent),
            Ferie(1.februar, 16.februar),
            sendtTilNAVEllerArbeidsgiver = 1.mai
        )
        håndterInntektsmelding(listOf(16.januar til 31.januar))
        assertVarsel(RV_SØ_2, 1.vedtaksperiode.filter())
        assertEquals(Dag.ForeldetSykedag::class, inspektør.vedtaksperiodeSykdomstidslinje(1.vedtaksperiode)[31.januar]::class)
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
    }

    @Test
    fun `forledet søknad ved forlengelse`() {
        nyttVedtak(januar)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar), mottatt = 1.februar(2019).atStartOfDay())
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.februar(2019))
        assertVarsel(RV_SØ_2, 2.vedtaksperiode.filter())
        assertTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK)
    }

    @Test
    fun `foreldet søknad etter annen foreldet søknad - samme arbeidsgiverperiode - deler korrelasjonsId`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 19.januar))
        håndterSykmelding(Sykmeldingsperiode(24.januar, 31.januar))

        // foreldet søknad :(
        håndterSøknad(Sykdom(1.januar, 19.januar, 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.mai)
        håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)

        // foreldet søknad :(
        håndterSøknad(Sykdom(24.januar, 31.januar, 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.mai)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 24.januar
        )

        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        assertVarsel(RV_SØ_2, 1.vedtaksperiode.filter())
        assertVarsel(RV_SØ_2, 2.vedtaksperiode.filter())

        val utbetalingJanuarFørste = inspektør.utbetaling(0)
        val utbetalingRevurdering = inspektør.utbetaling(1)
        val utbetalingJanuarSiste = inspektør.utbetaling(2)

        assertEquals(utbetalingJanuarFørste.korrelasjonsId, utbetalingRevurdering.korrelasjonsId)
        assertNotEquals(utbetalingJanuarFørste.korrelasjonsId, utbetalingJanuarSiste.korrelasjonsId)
        assertEquals(0, utbetalingJanuarFørste.arbeidsgiverOppdrag.size)
        assertEquals(0, utbetalingJanuarFørste.personOppdrag.size)
        assertEquals(0, utbetalingRevurdering.arbeidsgiverOppdrag.size)
        assertEquals(0, utbetalingRevurdering.personOppdrag.size)
        assertEquals(0, utbetalingJanuarSiste.arbeidsgiverOppdrag.size)
        assertEquals(0, utbetalingJanuarSiste.personOppdrag.size)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
    }

    @Test
    fun `foreldet søknad etter annen foreldet søknad - ulike arbeidsgiverperioder - deler ikke korrelasjonsId`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 19.januar))
        håndterSykmelding(Sykmeldingsperiode(19.februar, 12.mars))

        // foreldet søknad :(
        håndterSøknad(Sykdom(1.januar, 19.januar, 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.juni)
        håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)

        // foreldet søknad :(
        håndterSøknad(Sykdom(19.februar, 12.mars, 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.juli)
        håndterArbeidsgiveropplysninger(
            listOf(19.februar til 6.mars),
            vedtaksperiodeIdInnhenter = 2.vedtaksperiode
        )
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        assertVarsel(RV_SØ_2, 1.vedtaksperiode.filter())
        assertVarsel(RV_SØ_2, 2.vedtaksperiode.filter())

        val førsteUtbetaling = inspektør.utbetaling(0)
        val andreUtbetaling = inspektør.utbetaling(1)

        assertNotEquals(førsteUtbetaling.korrelasjonsId, andreUtbetaling.korrelasjonsId)
        assertEquals(0, førsteUtbetaling.arbeidsgiverOppdrag.size)
        assertEquals(0, førsteUtbetaling.personOppdrag.size)
        assertEquals(0, andreUtbetaling.arbeidsgiverOppdrag.size)
        assertEquals(0, andreUtbetaling.personOppdrag.size)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
    }
}
