package no.nav.helse.spleis.e2e.søknad

import no.nav.helse.februar
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertForkastetPeriodeTilstander
import no.nav.helse.spleis.e2e.assertFunksjonellFeil
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.nyttVedtak
import org.junit.jupiter.api.Test

internal class NyEllerOverlappendeSøknadTest : AbstractEndToEndTest() {

    @Test
    fun `out-of-order søknad får riktig error` () {
        nyttVedtak(1.februar, 28.februar)
        nyPeriode(1.januar til 31.januar)

        assertFunksjonellFeil("Mottatt søknad out of order")
        assertForkastetPeriodeTilstander(2.vedtaksperiode, START, TIL_INFOTRYGD)
    }

    @Test
    fun `overlappende søknad får riktig error` () {
        nyttVedtak(1.januar, 31.januar, orgnummer = a1)
        nyPeriode(30.januar til 20.februar, a2)

        assertFunksjonellFeil("Mottatt overlappende søknad")
        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, TIL_INFOTRYGD, orgnummer = a2)
    }
}