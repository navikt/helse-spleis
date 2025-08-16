package no.nav.helse.spleis.e2e

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.arbeidsledig
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ArbeidsledigTest : AbstractDslTest() {

    @Test
    fun `håndterer at sykmelding kommer som arbeidsledig, mens søknaden kommer på arbeidsgiver`() {
        arbeidsledig {
            håndterSykmelding(januar)
        }
        a1 {
            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        }
    }

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
