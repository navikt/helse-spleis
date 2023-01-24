package no.nav.helse.serde.api

import no.nav.helse.serde.api.speil.dagligAvrundet
import no.nav.helse.serde.api.speil.måndligAvrundet
import no.nav.helse.serde.api.speil.årligAvrundet
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SpeilAvrunderTest {

    private val Inntekt.daglig get() = reflection { _, _, daglig, _ -> daglig }
    private val Inntekt.månedlig get() = reflection { _, månedlig, _, _ -> månedlig }
    private val Inntekt.årlig get() = reflection { årlig, _, _, _ -> årlig }

    @Test
    fun `tallet lagret i person-JSON`() {
        val tallet = 46006.170000000006.månedlig
        assertEquals(2123.36, tallet.dagligAvrundet)
        assertEquals(2123.3616923076925, tallet.daglig)

        assertEquals(46006.17, tallet.måndligAvrundet)
        assertEquals(46006.170000000006, tallet.månedlig)

        assertEquals(552074.04, tallet.årligAvrundet)
        assertEquals(552074.04, tallet.årlig)
    }

    @Test
    fun `runder opp`() {
        val tallet = 46006.555.månedlig
        assertEquals(2123.38, tallet.dagligAvrundet)
        assertEquals(2123.379461538462, tallet.daglig)

        assertEquals(46006.56, tallet.måndligAvrundet)
        assertEquals(46006.555, tallet.månedlig)

        assertEquals(552078.66, tallet.årligAvrundet)
        assertEquals(552078.66, tallet.årlig)
    }

    @Test
    fun `runder ned`() {
        val tallet = 46006.544.månedlig
        assertEquals(2123.38, tallet.dagligAvrundet)
        assertEquals(2123.378953846154, tallet.daglig)

        assertEquals(46006.54, tallet.måndligAvrundet)
        assertEquals(46006.544, tallet.månedlig)

        assertEquals(552078.53, tallet.årligAvrundet)
        assertEquals(552078.528, tallet.årlig)
    }

    @Test
    fun `hel-tall`() {
        val tallet = 102.daglig
        assertEquals(102.0, tallet.dagligAvrundet)
        assertEquals(102.0, tallet.daglig)

        assertEquals(2210.00, tallet.måndligAvrundet)
        assertEquals(2210.00, tallet.månedlig)

        assertEquals(26520.0, tallet.årligAvrundet)
        assertEquals(26520.0, tallet.årlig)
    }

    @Test
    fun `Revurdering av inntekt og eller refusjon`() {
        val lagretISpleis = 500.544.månedlig
        val saksbehandlerSenderInn = lagretISpleis.måndligAvrundet
        assertEquals(500.54, saksbehandlerSenderInn)
        assertEquals(lagretISpleis, saksbehandlerSenderInn.månedlig)
        /*
        Expected :[Årlig: 6006.528, Månedlig: 500.54400000000004, Daglig: 23.10203076923077]
        Actual   :[Årlig: 6006.4800000000005, Månedlig: 500.54, Daglig: 23.101846153846157]
         */
    }
}