package no.nav.helse.spleis.e2e

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ArbeidsledigTest : AbstractDslTest() {

    @Test
    fun `arbeidsledigsøknad gir error`() {
        arbeidsledig {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            assertFunksjonelleFeil()
            assertForkastetPeriodeTilstander(1.vedtaksperiode, START, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `avbrutt arbeidsledig-søknad fjerner sykmeldingsperiode`() {
        arbeidsledig {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterAvbruttSøknad(1.januar til 31.januar)
            assertTrue(inspektør.sykmeldingsperioder().isEmpty())
        }
    }

}
