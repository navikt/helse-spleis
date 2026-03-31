package no.nav.helse.spleis.e2e.infotrygd

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.TestPerson
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
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class InfotrygdhistorikkEndretTest : AbstractDslTest() {
    private val utbetalinger = listOf(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar))

    @Test
    fun `infotrygdhistorikken var tom`() {
        a1 {
            periodeTilGodkjenning()
            håndterUtbetalingshistorikkEtterInfotrygdendring(*utbetalinger.toTypedArray())
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        }
    }

    @Test
    fun `cachet infotrygdhistorikk på periode grunnet påminnelse på annen vedtaksperiode, skal fortsatt reberegne`() {
        a1 {
            periodeTilGodkjenning()
            håndterSykmelding(Sykmeldingsperiode(1.mai, 31.mai))
            håndterSøknad(Sykdom(fom = 1.mai, tom = 31.mai, 100.prosent))
            håndterUtbetalingshistorikkEtterInfotrygdendring(*utbetalinger.toTypedArray())
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        }
    }

    @Test
    fun `infotrygdhistorikken blir tom`() {
        a1 {
            periodeTilGodkjenning(utbetalinger)
            håndterUtbetalingshistorikkEtterInfotrygdendring()
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        }
    }

    @Test
    fun `infotrygdhistorikken er uendret`() {
        a1 {
            periodeTilGodkjenning(utbetalinger)
            håndterUtbetalingshistorikkEtterInfotrygdendring(*utbetalinger.toTypedArray())
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        }
    }

    private fun TestPerson.TestArbeidsgiver.periodeTilGodkjenning(perioder: List<Infotrygdperiode> = emptyList()) {
        this {
            håndterUtbetalingshistorikkEtterInfotrygdendring(*perioder.toTypedArray())
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
            håndterSøknad(mars)
            håndterArbeidsgiveropplysninger(listOf(1.mars til 16.mars), vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterPåminnelse(1.vedtaksperiode, AVVENTER_GODKJENNING)
        }
    }
}
