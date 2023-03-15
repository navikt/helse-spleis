package no.nav.helse.spleis.e2e

import no.nav.helse.assertForventetFeil
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.hendelser.Søknad
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
            assertForventetFeil(
                forklaring = "avventer avklaring på varseltekst",
                nå = {
                    assertInfo("Bruker har oppgitt arbeid utenfor Norge.", 1.vedtaksperiode.filter())
                    assertIngenVarsler(1.vedtaksperiode.filter())
                },
                ønsket = {
                    // todo: det innføre nytt varsel
                    assertVarsel(Varselkode.RV_MV_1, 1.vedtaksperiode.filter())
                }
            )
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