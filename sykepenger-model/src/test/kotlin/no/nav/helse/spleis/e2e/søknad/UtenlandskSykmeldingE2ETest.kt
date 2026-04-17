package no.nav.helse.spleis.e2e.søknad

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_29
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class UtenlandskSykmeldingE2ETest : AbstractDslTest() {

    @Test
    fun `søknad med flagg utenlandskSykmelding ignoreres og kastes ut`() {
        a1 {

            håndterSykmelding(januar)
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), utenlandskSykmelding = true)
            assertFunksjonellFeil(RV_SØ_29, 1.vedtaksperiode.filter())
            assertForkastetPeriodeTilstander(1.vedtaksperiode, START, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `søknad med flagg utenlandskSykmelding ignoreres og kastes ut - prøver oss ikke på forlengelsen uten flagg`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), utenlandskSykmelding = true)
            assertFunksjonellFeil(RV_SØ_29, 1.vedtaksperiode.filter())
            assertForkastetPeriodeTilstander(1.vedtaksperiode, START, TIL_INFOTRYGD)

            håndterSykmelding(januar)
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), utenlandskSykmelding = false)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, START, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `Overlapper med utbetalt - søknad med flagg utenlandskSykmelding lager varsel`() {
        a1 {
            nyttVedtak(januar)
            nullstillTilstandsendringer()
            håndterSykmelding(januar)
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), utenlandskSykmelding = true)
            assertVarsler(listOf(RV_SØ_29), 1.vedtaksperiode.filter())
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        }
    }
}
