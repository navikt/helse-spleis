package no.nav.helse.spleis.e2e.søknad

import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Permisjon
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Utlandsopphold
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_8
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertIngenVarsel
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.assertVarsler
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class OppholdUtenforEØSTest : AbstractEndToEndTest() {

    @Test
    fun `Søknad med utenlandsopphold gir warning`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSøknad(Sykdom(3.januar, 26.januar, 100.prosent), Utlandsopphold(11.januar, 15.januar), Permisjon(11.januar, 11.januar), Ferie(12.januar, 12.januar))
        assertVarsler()
        assertVarsel(RV_SØ_8, 1.vedtaksperiode.filter())
    }

    @Test
    fun `Søknad med all utenlandsopphold i ferie gir ikke warning`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSøknad(Sykdom(3.januar, 26.januar, 100.prosent), Utlandsopphold(11.januar, 15.januar), Ferie(11.januar, 15.januar))
        assertIngenVarsel(RV_SØ_8, 1.vedtaksperiode.filter())
    }

    @Test
    fun `Søknad med all utenlandsopphold i permisjon gir ikke warning`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSøknad(Sykdom(3.januar, 26.januar, 100.prosent), Utlandsopphold(11.januar, 15.januar), Permisjon(11.januar, 15.januar))
        assertIngenVarsel(RV_SØ_8, 1.vedtaksperiode.filter())
    }

    @Test
    fun `Søknad med all utenlandsopphold i ferie eller permisjon gir ikke warning`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSøknad(Sykdom(3.januar, 26.januar, 100.prosent), Utlandsopphold(11.januar, 15.januar), Permisjon(11.januar, 12.januar), Ferie(13.januar, 15.januar))
        assertIngenVarsel(RV_SØ_8, 1.vedtaksperiode.filter())
    }
}