package no.nav.helse.spleis.e2e.infotrygd

import no.nav.helse.dsl.a1
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.håndterArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterPåminnelse
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikkEtterInfotrygdendring
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class InfotrygdhistorikkEndretTest : AbstractEndToEndTest() {
    private val utbetalinger = listOf(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar))

    @Test
    fun `infotrygdhistorikken var tom`() {
        periodeTilGodkjenning()
        this@InfotrygdhistorikkEndretTest.håndterUtbetalingshistorikkEtterInfotrygdendring(*utbetalinger.toTypedArray())
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
    }

    @Test
    fun `cachet infotrygdhistorikk på periode grunnet påminnelse på annen vedtaksperiode, skal fortsatt reberegne`() {
        periodeTilGodkjenning()
        håndterSykmelding(Sykmeldingsperiode(1.mai, 31.mai))
        håndterSøknad(Sykdom(fom = 1.mai, tom = 31.mai, 100.prosent))
        this@InfotrygdhistorikkEndretTest.håndterUtbetalingshistorikkEtterInfotrygdendring(*utbetalinger.toTypedArray())
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
    }

    @Test
    fun `infotrygdhistorikken blir tom`() {
        periodeTilGodkjenning(utbetalinger)
        this@InfotrygdhistorikkEndretTest.håndterUtbetalingshistorikkEtterInfotrygdendring()
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
    }

    @Test
    fun `infotrygdhistorikken er uendret`() {
        periodeTilGodkjenning(utbetalinger)
        this@InfotrygdhistorikkEndretTest.håndterUtbetalingshistorikkEtterInfotrygdendring(*utbetalinger.toTypedArray())
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
    }

    private fun periodeTilGodkjenning(perioder: List<Infotrygdperiode> = emptyList()) {
        this@InfotrygdhistorikkEndretTest.håndterUtbetalingshistorikkEtterInfotrygdendring(*perioder.toTypedArray())
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
        håndterSøknad(mars)
        håndterArbeidsgiveropplysninger(listOf(1.mars til 16.mars), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@InfotrygdhistorikkEndretTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@InfotrygdhistorikkEndretTest.håndterPåminnelse(1.vedtaksperiode, AVVENTER_GODKJENNING)
    }
}
