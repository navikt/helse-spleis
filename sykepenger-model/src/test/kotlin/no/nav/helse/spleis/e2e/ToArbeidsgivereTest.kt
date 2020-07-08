package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Test

internal class ToArbeidsgivereTest : AbstractEndToEndTest() {

    internal companion object {
        val redCompany = "redCompany"
        val blueCompany = "blueCompany"
    }

    @Test
    fun `overlappende arbeidsgivere sendt til infotrygd`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100), orgnummer = redCompany))
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100), orgnummer = blueCompany))
        assertNoErrors(inspektør)
        person.håndter(inntektsmelding(listOf(Periode(1.januar, 16.januar)), orgnummer = redCompany))
        person.håndter(søknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100), orgnummer = redCompany))
        person.håndter(vilkårsgrunnlag(redCompany.id(0), INNTEKT, orgnummer = redCompany))
    }
}
