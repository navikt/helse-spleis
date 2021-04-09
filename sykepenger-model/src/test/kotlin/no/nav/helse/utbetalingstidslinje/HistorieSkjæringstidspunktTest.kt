package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.til
import no.nav.helse.testhelpers.april
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class HistorieSkjæringstidspunktTest : HistorieTest() {

    @Test
    fun `infotrygd - spleis`() {
        historie(refusjon(1.januar, 31.januar))
        historie.add(AG1, sykedager(1.februar, 28.februar))
        assertEquals(1.januar, skjæringstidspunkt(31.januar))
        assertEquals(1.januar, skjæringstidspunkt(28.februar))
        assertEquals(1.januar, skjæringstidspunkt(28.februar))
        assertSkjæringstidspunkter(1.januar)
        assertTrue(historie.forlengerInfotrygd(AG1, 1.februar til 28.februar))
    }

    @Test
    fun `spleis - infotrygd - spleis`() {
        historie(refusjon(1.februar, 28.februar))
        historie.add(AG1, navdager(1.januar, 31.januar))
        historie.add(AG1, sykedager(1.mars, 31.mars))
        assertEquals(1.januar, skjæringstidspunkt(31.januar))
        assertEquals(1.januar, skjæringstidspunkt(28.februar))
        assertEquals(1.januar, skjæringstidspunkt(31.mars))
    }

    @Test
    fun `infotrygd - spleis - infotrygd - spleis`() {
        historie(refusjon(1.januar, 31.januar), refusjon(1.mars, 31.mars))
        historie.add(AG1, navdager(1.februar, 28.februar))
        historie.add(AG1, sykedager(1.april, 30.april))
        assertEquals(1.januar, skjæringstidspunkt(31.januar))
        assertEquals(1.januar, skjæringstidspunkt(28.februar))
        assertEquals(1.januar, skjæringstidspunkt(31.mars))
    }

    @Test
    fun `infotrygd - gap - spleis`() {
        historie(refusjon(1.februar, 27.februar))
        historie.add(AG1, sykedager(1.mars, 31.mars))
        assertEquals(1.februar, skjæringstidspunkt(28.februar))
        assertEquals(1.mars, skjæringstidspunkt(31.mars))
        assertSkjæringstidspunkter(1.mars, 1.februar)
    }

    @Test
    fun `spleis - gap - infotrygd - spleis`() {
        historie(refusjon(1.februar, 28.februar))
        historie.add(AG1, navdager(1.januar, 30.januar))
        historie.add(AG1, sykedager(1.mars, 31.mars))
        assertEquals(1.januar, skjæringstidspunkt(31.januar))
        assertEquals(1.februar, skjæringstidspunkt(28.februar))
        assertEquals(1.februar, skjæringstidspunkt(31.mars))
        assertSkjæringstidspunkter(1.februar, 1.januar)
        assertTrue(historie.forlengerInfotrygd(AG1, 1.mars til 31.mars))
    }

    @Test
    fun `spleis - infotrygd - gap - spleis`() {
        historie(refusjon(1.februar, 28.februar))
        historie.add(AG1, navdager(1.januar, 31.januar))
        historie.add(AG1, sykedager(2.mars, 31.mars))
        assertEquals(1.januar, skjæringstidspunkt(31.januar))
        assertEquals(1.januar, skjæringstidspunkt(1.mars))
        assertEquals(2.mars, skjæringstidspunkt(31.mars))
        assertFalse(historie.forlengerInfotrygd(AG1, 2.mars til 31.mars))
    }

    @Test
    fun `infotrygd - gap - infotrygdTilBbruker - spleis`() {
        historie(refusjon(1.januar, 31.januar), bruker(2.februar, 28.februar))
        historie.add(AG1, sykedager(1.mars, 31.mars))
        assertEquals(1.januar, skjæringstidspunkt(1.februar))
        assertEquals(2.februar, skjæringstidspunkt(28.februar))
        assertEquals(2.februar, skjæringstidspunkt(31.mars))
        assertTrue(historie.forlengerInfotrygd(AG1, 1.mars til 31.mars))
    }

    @Test
    fun `infotrygdferie - infotrygd - spleis`() {
        historie(ferie(1.januar, 31.januar), bruker(1.februar, 28.februar))
        historie.add(AG1, sykedager(1.mars, 31.mars))
        assertEquals(1.februar, skjæringstidspunkt(1.februar))
        assertEquals(1.februar, skjæringstidspunkt(28.februar))
        assertEquals(1.februar, skjæringstidspunkt(31.mars))
        assertTrue(historie.forlengerInfotrygd(AG1, 1.mars til 31.mars))
    }

    @Test
    fun `infotrygd - infotrygdferie - spleis`() {
        historie(refusjon(1.januar, 31.januar), ferie(1.februar, 10.februar))
        historie.add(AG1, sykedager(11.februar, 28.februar))
        assertEquals(1.januar, skjæringstidspunkt(1.februar))
        assertEquals(1.januar, skjæringstidspunkt(10.februar))
        assertEquals(1.januar, skjæringstidspunkt(28.februar))
        assertTrue(historie.forlengerInfotrygd(AG1, 11.februar til 28.februar))
    }

    @Test
    fun `sykedag på fredag og feriedag på fredag`() {
        historie(
            refusjon(2.januar, 12.januar, 1000.daglig,  100.prosent,  AG1),
            ferie(15.januar, 19.januar),
            refusjon(22.januar, 31.januar, 1000.daglig,  100.prosent,  AG1),
        )

        assertEquals(null, skjæringstidspunkt(1.januar))
        assertEquals(2.januar, skjæringstidspunkt(21.januar))
        assertEquals(2.januar, skjæringstidspunkt(31.januar))
        assertTrue(historie.forlengerInfotrygd(AG1, 1.februar til 28.februar))
        assertFalse(historie.forlengerInfotrygd(AG2, 22.januar til 31.januar))
    }

    @Test
    fun `skjæringstidspunkt med flere arbeidsgivere`() {
        historie(
            refusjon(2.januar, 12.januar, 1000.daglig,  100.prosent,  AG1),
            ferie(15.januar, 19.januar),
            refusjon(22.januar, 31.januar, 1000.daglig,  100.prosent,  AG2),
        )

        assertEquals(null, skjæringstidspunkt(1.januar))
        assertEquals(2.januar, skjæringstidspunkt(21.januar))
        assertEquals(2.januar, skjæringstidspunkt(31.januar))
        assertFalse(historie.forlengerInfotrygd(AG2, 16.januar til 31.januar))
        assertTrue(historie.forlengerInfotrygd(AG2, 1.februar til 28.februar))
    }

    @Test
    fun `skjæringstidspunkt med avviste, foreldet og feriedager`() {
        historie(
            refusjon(1.januar, 5.januar, 1000.daglig,  100.prosent,  AG1),
            ferie(8.januar, 12.januar)
        )
        historie.add(AG1, foreldetdager(15. januar, 19.januar))
        historie.add(AG1, feriedager(22. januar, 26.januar))
        historie.add(AG1, avvistedager(29. januar, 3.februar))
        historie.add(AG1, navdager(5.februar, 10.februar))

        assertEquals(1.januar, skjæringstidspunkt(28.februar))
    }

    @Test
    fun `skjæringstidspunkt brytes opp av arbeidsdag`() {
        historie(
            refusjon(1.januar, 5.januar, 1000.daglig,  100.prosent,  AG1),
            ferie(8.januar, 12.januar)
        )
        historie.add(AG1, foreldetdager(15. januar, 19.januar))
        historie.add(AG1, feriedager(22. januar, 26.januar))
        historie.add(AG1, arbeidsdager(27.januar, 28.januar))
        historie.add(AG1, navdager(29.januar, 10.februar))

        assertEquals(29.januar, skjæringstidspunkt(28.februar))
    }

    @Test
    fun `innledende ferie som avsluttes på fredag`() {
        historie(
            ferie(1.januar, 5.januar),
            refusjon(8.januar, 12.januar, 1000.daglig,  100.prosent,  AG1)
        )

        assertEquals(8.januar, skjæringstidspunkt(12.januar))
    }

    @Test
    fun `avgrenset periode for en arbeidsgiver`() {
        historie(
            refusjon(1.januar, 20.januar, 1000.daglig,  100.prosent,  AG1),
            refusjon(1.januar, 31.januar, 1000.daglig,  100.prosent,  AG2),
        )
        historie.add(AG1, sykedager(1.februar, 28.februar))
        assertEquals(1.februar til 28.februar, historie.avgrensetPeriode(AG1, 1.februar til 28.februar))
    }

    @Test
    fun `avgrenset periode går ikke lengre tilbake enn skjæringstidspunkt for arbeidsgiveren`() {
        historie(
            refusjon(17.januar, 31.januar, 1000.daglig,  100.prosent,  AG1)
        )
        historie.add(AG1, sykedager(1.januar, 16.januar))
        historie.add(AG1, sykedager(2.februar, 28.februar))
        assertEquals(2.februar til 28.februar, historie.avgrensetPeriode(AG1, 1.januar til 28.februar))
    }
}
