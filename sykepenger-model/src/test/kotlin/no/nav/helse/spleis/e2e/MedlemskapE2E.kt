package no.nav.helse.spleis.e2e

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class MedlemskapE2E : AbstractDslTest() {
    @Test
    fun `søknad med arbeidUtenforNorge gir varsel om medlemskap`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), arbeidUtenforNorge = true)
            assertVarsel(Varselkode.RV_MV_3, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `søknad uten arbeidUtenforNorge gir ikke varsel om medlemskap`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), arbeidUtenforNorge = false)
            assertIngenVarsler(1.vedtaksperiode.filter())
        }
    }
}