package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ToArbeidsgivereTest : AbstractEndToEndTest() {

    internal companion object {
        private const val rød = "rød"
        private const val blå = "blå"
    }

    @Test
    fun `overlappende arbeidsgivere sendt til infotrygd`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100), orgnummer = rød))
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100), orgnummer = blå))

        person.håndter(inntektsmelding(listOf(Periode(1.januar, 16.januar)), orgnummer = rød))
        person.håndter(søknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100), orgnummer = rød))
        person.håndter(vilkårsgrunnlag(rød.id(0), INNTEKT, orgnummer = rød))
        assertNoErrors(inspektør)

        person.håndter(ytelser(rød.id(0), orgnummer = rød))
        TestArbeidsgiverInspektør(person, rød).also { inspektør ->
            assertErrors(inspektør)
            assertEquals(TIL_INFOTRYGD, inspektør.sisteForkastetTilstand(0))
        }
        TestArbeidsgiverInspektør(person, blå).also { inspektør ->
            assertErrors(inspektør)
            assertEquals(TIL_INFOTRYGD, inspektør.sisteForkastetTilstand(0))
        }
    }
}
