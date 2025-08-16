package no.nav.helse.spleis.e2e.søknad

import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.januar
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_30
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertForkastetPeriodeTilstander
import no.nav.helse.spleis.e2e.assertFunksjonellFeil
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertVarsler
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class SendTilGosysFlaggSattE2ETest : AbstractEndToEndTest() {

    @Test
    fun `søknad med flagg sendTilGosys ignoreres og kastes ut`() {
        håndterSykmelding(januar)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), sendTilGosys = true)
        assertFunksjonellFeil(RV_SØ_30, 1.vedtaksperiode.filter())
        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, TIL_INFOTRYGD)
    }

    @Test
    fun `søknad med flagg sendTilGosys ignoreres og kastes ut - prøver oss ikke på forlengelsen uten flagg`() {
        håndterSykmelding(januar)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), sendTilGosys = true)
        assertFunksjonellFeil(RV_SØ_30, 1.vedtaksperiode.filter())
        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, TIL_INFOTRYGD)

        håndterSykmelding(januar)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), sendTilGosys = false)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, START, TIL_INFOTRYGD)
    }

    @Test
    fun `Overlapper med utbetalt - søknad med flagg sendTilGosys lager varsel`() {
        nyttVedtak(januar)
        nullstillTilstandsendringer()
        håndterSykmelding(januar)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), sendTilGosys = true)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertVarsler(listOf(RV_SØ_30), 1.vedtaksperiode.filter())
    }
}
