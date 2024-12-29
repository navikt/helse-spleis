package no.nav.helse.spleis.e2e.søknad

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_YS_1
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class YrkesskadeE2ETest : AbstractDslTest() {

    @Test
    fun `søknad med yrkesskade gir varsel om yrkesskade`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), yrkesskade = true)
            assertVarsel(RV_YS_1, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `søknad uten yrkesskade gir ikke varsel om yrkesskade`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), yrkesskade = false)
            assertVarsler(emptyList(), 1.vedtaksperiode.filter())
        }
    }
}
