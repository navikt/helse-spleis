package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Søknad
import no.nav.helse.januar
import no.nav.helse.person.Varselkode
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class VarslerE2ETest: AbstractEndToEndTest() {

    @Test
    fun `varsel - Sykmeldingen er tilbakedatert, vurder fra og med dato for utbetaling`() {
        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent),
            merknaderFraSykmelding = listOf(Søknad.Merknad("UGYLDIG_TILBAKEDATERING"))
        )
        assertVarsel(Varselkode.RV_SØ_3, 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel - Søknaden inneholder permittering, Vurder om permittering har konsekvens for rett til sykepenger`() {
        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent),
            permittert = true
        )
        assertVarsel(Varselkode.RV_SØ_1, 1.vedtaksperiode.filter())
    }
}