package no.nav.helse.spleis.e2e.inntektsmelding

import no.nav.helse.assertForventetFeil
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class BegrunnelseForReduksjonEllerIkkeUtbetaltTest: AbstractDslTest() {

    @Test
    fun `arbeidsgiverperioden strekker seg over to perioder og inntektsmelding kommer etter søknadene`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent))
            håndterSøknad(Sykdom(11.januar, 17.januar, 100.prosent))
            assertEquals("SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT, begrunnelseForReduksjonEllerIkkeUtbetalt = "ManglerOpptjening")
            assertEquals("NNNNNHH NNNNNHH NNS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        }
    }

    @Test
    fun `arbeidsgiverperioden strekker seg over to perioder og inntektsmelding kommer før siste søknad`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent))
            assertEquals("SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT, begrunnelseForReduksjonEllerIkkeUtbetalt = "ManglerOpptjening")
            assertEquals("NNNNNHH NNN", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
            håndterSøknad(Sykdom(11.januar, 17.januar, 100.prosent))
            assertForventetFeil(
                forklaring = "Når IM skal lage SykNav-dager kommer før vi har mottatt søknad blir det ikke lagt inn som SykNav når søknaden kommer",
                nå = {
                    assertEquals("NNNNNHH NNNSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
                },
                ønsket = {
                    assertEquals("NNNNNHH NNNNNHH NNS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
                }
            )
        }
    }
}