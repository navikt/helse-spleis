package no.nav.helse.spleis.e2e.søknad

import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
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
import org.junit.jupiter.api.Test

internal class ForeldetSøknadE2ETest : AbstractEndToEndTest() {
    @Test
    fun `forledet søknad`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), mottatt = 1.januar(2019).atStartOfDay())
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.januar(2019))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        assertVarsel("Minst én dag er avslått på grunn av foreldelse. Vurder å sende vedtaksbrev fra Infotrygd")
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
    }

    @Test
    fun `forledet søknad med inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), mottatt = 1.januar(2019).atStartOfDay())
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.januar(2019))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        assertVarsel("Minst én dag er avslått på grunn av foreldelse. Vurder å sende vedtaksbrev fra Infotrygd")
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning()
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
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
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        assertEquals(Dag.ForeldetSykedag::class, inspektør.vedtaksperiodeSykdomstidslinje(1.vedtaksperiode)[31.januar]::class)
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING,
            AVSLUTTET
        )
    }
    @Test
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
        assertVarsel("Minst én dag er avslått på grunn av foreldelse. Vurder å sende vedtaksbrev fra Infotrygd")
        assertTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK)
    }
}
