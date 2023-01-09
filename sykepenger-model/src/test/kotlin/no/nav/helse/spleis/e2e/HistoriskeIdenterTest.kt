package no.nav.helse.spleis.e2e

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.Varselkode
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class HistoriskeIdenterTest : AbstractDslTest() {

    @Test
    fun `historiske identer test`() {
        medTidligereBehandledeIdenter()
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 26.januar, 100.prosent))
        assertFunksjonellFeil(Varselkode.RV_AN_5)
        a1 {
            assertForkastetPeriodeTilstander(
                1.vedtaksperiode,
                START,
                TIL_INFOTRYGD
            )
        }
    }

}
