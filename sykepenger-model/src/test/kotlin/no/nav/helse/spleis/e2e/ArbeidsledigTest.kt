package no.nav.helse.spleis.e2e

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ArbeidsledigTest : AbstractDslTest() {

    @Test
    fun `arbeidsledigsøknad gir error`() {
        arbeidsledig {
            håndterSøknad(januar)
            assertFunksjonelleFeil()
            assertForkastetPeriodeTilstander(1.vedtaksperiode, START, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `avbrutt arbeidsledig-søknad fjerner sykmeldingsperiode`() {
        arbeidsledig {
            håndterSykmelding(januar)
            håndterAvbruttSøknad(januar)
            assertTrue(inspektør.sykmeldingsperioder().isEmpty())
        }
    }

}
