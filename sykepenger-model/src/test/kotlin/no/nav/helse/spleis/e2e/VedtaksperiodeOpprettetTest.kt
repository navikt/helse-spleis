package no.nav.helse.spleis.e2e

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.nyPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.oktober
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VedtaksperiodeOpprettetTest : AbstractDslTest() {

    @Test
    fun `to førstegangsbehandlinger`() {
        a1 {
            nyPeriode(1.januar til 20.januar)
        }
        a2 {
            nyPeriode(10.oktober til 21.oktober)
        }

        assertEquals(2, observatør.vedtaksperiodeOpprettetEventer.size)
        assertEquals(1.januar, observatør.vedtaksperiodeOpprettetEventer.first().skjæringstidspunkt)
        assertEquals(10.oktober, observatør.vedtaksperiodeOpprettetEventer.last().skjæringstidspunkt)
    }
}
