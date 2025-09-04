package no.nav.helse.spleis.e2e.ytelser

import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.februar
import no.nav.helse.hendelser.GradertPeriode
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_UTBETALING
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class PleiepengerE2ETest : AbstractDslTest() {

    @Test
    fun `Periode for person der det ikke foreligger pleiepengerytelse blir behandlet og sendt til godkjenning`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar(2020), 31.januar(2020)))
            håndterSøknad(Sykdom(1.januar(2020), 31.januar(2020), 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(Periode(1.januar(2020), 16.januar(2020))),
                vedtaksperiodeId = 1.vedtaksperiode
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode, pleiepenger = emptyList())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
            håndterUtbetalt(Oppdragstatus.AKSEPTERT)

            assertTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INFOTRYGDHISTORIKK,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_VILKÅRSPRØVING,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                TIL_UTBETALING,
                AVSLUTTET
            )
        }
    }

    @Test
    fun `Periode som overlapper med pleiepengerytelse får varsel`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(Periode(1.januar, 16.januar)), vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode, pleiepenger = listOf(GradertPeriode(januar, 100)))
            assertVarsel(Varselkode.RV_AY_6, 1.vedtaksperiode.filter())
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
        }
    }

    @Test
    fun `Periode som overlapper med pleiepengerytelse i starten av perioden får varsel`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(Periode(1.januar, 16.januar)), vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode, pleiepenger = listOf(GradertPeriode(1.desember(2017) til 1.januar, 100)))
            assertVarsel(Varselkode.RV_AY_6, 1.vedtaksperiode.filter())
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
        }
    }

    @Test
    fun `Periode som overlapper med pleiepengerytelse i slutten av perioden får varsel`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(Periode(1.januar, 16.januar)), vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode, pleiepenger = listOf(GradertPeriode(31.januar til 14.februar, 100)))
            assertVarsel(Varselkode.RV_AY_6, 1.vedtaksperiode.filter())
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
        }
    }

    @Test
    fun `Pleiepenger starter mindre enn 4 uker før sykefraværstilfellet`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(3.mars, 19.mars))
            håndterSøknad(Sykdom(3.mars, 19.mars, 100.prosent))
            håndterArbeidsgiveropplysninger(listOf(3.mars til 18.mars), vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode, pleiepenger = listOf(GradertPeriode(3.februar til 20.februar, 100)))
            assertVarsel(Varselkode.RV_AY_6, 1.vedtaksperiode.filter())
            assertTilstand(1.vedtaksperiode, AVVENTER_SIMULERING)
        }
    }

    @Test
    fun `Pleiepenger starter mer enn 4 uker før sykefraværstilfellet`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(3.mars, 19.mars))
            håndterSøknad(Sykdom(3.mars, 19.mars, 100.prosent))
            håndterArbeidsgiveropplysninger(listOf(3.mars til 18.mars), vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode, pleiepenger = listOf(GradertPeriode(3.januar til 20.januar, 100)))
            assertIngenFunksjonelleFeil()
        }
    }
}
