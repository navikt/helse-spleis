package no.nav.helse.person

import no.nav.helse.hendelser.til
import no.nav.helse.testhelpers.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class HistoriePingPongTest : HistorieTest() {

    @Test
    fun `infotrygd - spleis`() {
        historie(refusjon(1.januar, 31.januar))
        historie.add(AG1, sykedager(1.februar, 28.februar))
        assertFalse(historie.erPingPong(AG1, 1.februar til 28.februar))
    }

    @Test
    fun `infotrygd - gap - spleis - gap - infotrygd - spleis - spleis`() {
        historie(refusjon(1.januar, 31.januar), refusjon(9.april, 30.april))
        historie.add(AG1, sykedager(1.mars, 30.mars))
        historie.add(AG1, sykedager(1.mai, 31.mai))
        historie.add(AG1, sykedager(1.juni, 30.juni))
        assertFalse(historie.erPingPong(AG1, 1.mars til 31.mars))
        assertFalse(historie.erPingPong(AG1, 1.mai til 31.mai))
        assertFalse(historie.erPingPong(AG1, 1.juni til 30.juni))
    }

    @Test
    fun `infotrygd - spleis - spleis`() {
        historie(refusjon(1.januar, 31.januar))
        historie.add(AG1, sykedager(1.februar, 28.februar))
        historie.add(AG1, sykedager(1.mars, 31.mars))
        assertFalse(historie.erPingPong(AG1, 1.februar til 28.februar))
        assertFalse(historie.erPingPong(AG1, 1.februar til 28.februar))
    }

    @Test
    fun `infotrygd - spleis - infotrygd - spleis`() {
        historie(refusjon(1.januar, 31.januar), refusjon(1.mars, 31.mars))
        historie.add(AG1, navdager(1.februar, 28.februar))
        historie.add(AG1, sykedager(1.april, 30.april))
        assertFalse(historie.erPingPong(AG1, 1.februar til 28.februar))
        assertTrue(historie.erPingPong(AG1, 1.april til 30.april))
    }

    @Test
    fun `er ikke ping-pong eldre enn 6 mnd tilbake i infotrygdforlengelser`() {
        historie(refusjon(1.januar, 31.januar), refusjon(1.mars, 31.mars))
        historie.add(AG1, navdager(1.februar, 28.februar))
        historie.add(AG1, navdager(1.april, 30.april))
        historie.add(AG1, navdager(1.mai, 31.mai))
        historie.add(AG1, navdager(1.juni, 30.juni))
        historie.add(AG1, navdager(1.juli, 31.juli))
        historie.add(AG1, navdager(1.august, 31.august))
        historie.add(AG1, navdager(1.september, 30.september))
        assertFalse(historie.erPingPong(AG1, 1.februar til 28.februar))
        assertTrue(historie.erPingPong(AG1, 1.april til 30.april))
        assertFalse(historie.erPingPong(AG1, 1.september til 30.september))
    }

    @Test
    fun `er ikke ping-pong eldre enn 6 mnd tilbake i spleisforlengelser`() {
        historie(refusjon(1.februar, 28.februar), refusjon(1.april, 30.april))
        historie.add(AG1, navdager(1.januar, 31.januar))
        historie.add(AG1, navdager(1.mars, 31.mars))
        historie.add(AG1, navdager(1.mai, 31.mai))
        historie.add(AG1, navdager(1.juni, 30.juni))
        historie.add(AG1, navdager(1.juli, 31.juli))
        historie.add(AG1, navdager(1.august, 31.august))
        historie.add(AG1, navdager(1.september, 30.september))
        historie.add(AG1, navdager(1.oktober, 31.oktober))
        assertTrue(historie.erPingPong(AG1, 1.mars til 31.mars))
        assertTrue(historie.erPingPong(AG1, 1.mai til 31.mai))
        assertFalse(historie.erPingPong(AG1, 1.oktober til 31.oktober))
    }

    @Test
    fun `spleis - infotrygd - spleis`() {
        historie(refusjon(1.februar, 28.februar))
        historie.add(AG1, navdager(1.januar, 31.januar))
        historie.add(AG1, sykedager(1.mars, 31.mars))
        assertTrue(historie.erPingPong(AG1, 1.mars til 31.mars))
    }

    @Test
    fun `infotrygd - gap - spleis`() {
        historie(refusjon(1.februar, 27.februar))
        historie.add(AG1, sykedager(1.mars, 31.mars))
        assertFalse(historie.erPingPong(AG1, 1.mars til 31.mars))
    }
}
