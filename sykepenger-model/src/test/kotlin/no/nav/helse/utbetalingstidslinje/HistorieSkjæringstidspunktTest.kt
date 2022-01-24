package no.nav.helse.utbetalingstidslinje

import no.nav.helse.april
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class HistorieSkjæringstidspunktTest : HistorieTest() {

    @Test
    fun `infotrygd - spleis`() {
        historie(utbetaling(1.januar, 31.januar))
        addSykdomshistorikk(AG1, sykedager(1.februar, 28.februar))
        assertEquals(1.januar, skjæringstidspunkt(31.januar))
        assertEquals(1.januar, skjæringstidspunkt(28.februar))
        assertEquals(1.januar, skjæringstidspunkt(28.februar))
        assertSkjæringstidspunkter(1.januar)
    }

    @Test
    fun `spleis - infotrygd - spleis`() {
        historie(utbetaling(1.februar, 28.februar))
        addTidligereUtbetaling(AG1, navdager(1.januar, 31.januar))
        addSykdomshistorikk(AG1, sykedager(1.mars, 31.mars))
        assertEquals(1.januar, skjæringstidspunkt(31.januar))
        assertEquals(1.januar, skjæringstidspunkt(28.februar))
        assertEquals(1.januar, skjæringstidspunkt(31.mars))
    }

    @Test
    fun `infotrygd - spleis - infotrygd - spleis`() {
        historie(utbetaling(1.januar, 31.januar), utbetaling(1.mars, 31.mars))
        addTidligereUtbetaling(AG1, navdager(1.februar, 28.februar))
        addSykdomshistorikk(AG1, sykedager(1.april, 30.april))
        assertEquals(1.januar, skjæringstidspunkt(31.januar))
        assertEquals(1.januar, skjæringstidspunkt(28.februar))
        assertEquals(1.januar, skjæringstidspunkt(31.mars))
    }

    @Test
    fun `infotrygd - gap - spleis`() {
        historie(utbetaling(1.februar, 27.februar))
        addSykdomshistorikk(AG1, sykedager(1.mars, 31.mars))
        assertEquals(1.februar, skjæringstidspunkt(28.februar))
        assertEquals(1.mars, skjæringstidspunkt(31.mars))
        assertSkjæringstidspunkter(1.mars, 1.februar)
    }

    @Test
    fun `spleis - gap - infotrygd - spleis`() {
        historie(utbetaling(1.februar, 28.februar))
        addTidligereUtbetaling(AG1, navdager(1.januar, 30.januar))
        addSykdomshistorikk(AG1, sykedager(1.mars, 31.mars))
        assertEquals(1.januar, skjæringstidspunkt(31.januar))
        assertEquals(1.februar, skjæringstidspunkt(28.februar))
        assertEquals(1.februar, skjæringstidspunkt(31.mars))
        assertSkjæringstidspunkter(1.februar, 1.januar)
    }

    @Test
    fun `spleis - infotrygd - gap - spleis`() {
        historie(utbetaling(1.februar, 28.februar))
        addTidligereUtbetaling(AG1, navdager(1.januar, 31.januar))
        addSykdomshistorikk(AG1, sykedager(2.mars, 31.mars))
        assertEquals(1.januar, skjæringstidspunkt(31.januar))
        assertEquals(1.januar, skjæringstidspunkt(1.mars))
        assertEquals(2.mars, skjæringstidspunkt(31.mars))
    }

    @Test
    fun `infotrygd - gap - infotrygdTilBbruker - spleis`() {
        historie(utbetaling(1.januar, 31.januar), utbetaling(2.februar, 28.februar))
        addSykdomshistorikk(AG1, sykedager(1.mars, 31.mars))
        assertEquals(1.januar, skjæringstidspunkt(1.februar))
        assertEquals(2.februar, skjæringstidspunkt(28.februar))
        assertEquals(2.februar, skjæringstidspunkt(31.mars))
    }

    @Test
    fun `infotrygdferie - infotrygd - spleis`() {
        historie(ferie(1.januar, 31.januar), utbetaling(1.februar, 28.februar))
        addSykdomshistorikk(AG1, sykedager(1.mars, 31.mars))
        assertEquals(1.februar, skjæringstidspunkt(1.februar))
        assertEquals(1.februar, skjæringstidspunkt(28.februar))
        assertEquals(1.februar, skjæringstidspunkt(31.mars))
    }

    @Test
    fun `infotrygd - infotrygdferie - spleis`() {
        historie(utbetaling(1.januar, 31.januar), ferie(1.februar, 10.februar))
        addSykdomshistorikk(AG1, sykedager(11.februar, 28.februar))
        assertEquals(1.januar, skjæringstidspunkt(1.februar))
        assertEquals(1.januar, skjæringstidspunkt(10.februar))
        assertEquals(1.januar, skjæringstidspunkt(28.februar))
    }

    @Test
    fun `sykedag på fredag og feriedag på fredag`() {
        historie(
            utbetaling(2.januar, 12.januar, 1000.daglig,  100.prosent,  AG1),
            ferie(15.januar, 19.januar),
            utbetaling(22.januar, 31.januar, 1000.daglig,  100.prosent,  AG1),
        )

        assertEquals(1.januar, skjæringstidspunkt(1.januar))
        assertEquals(2.januar, skjæringstidspunkt(21.januar))
        assertEquals(2.januar, skjæringstidspunkt(31.januar))
    }

    @Test
    fun `skjæringstidspunkt med flere arbeidsgivere`() {
        historie(
            utbetaling(2.januar, 12.januar, 1000.daglig,  100.prosent,  AG1),
            ferie(15.januar, 19.januar),
            utbetaling(22.januar, 31.januar, 1000.daglig,  100.prosent,  AG2),
        )

        assertEquals(1.januar, skjæringstidspunkt(1.januar))
        assertEquals(2.januar, skjæringstidspunkt(21.januar))
        assertEquals(2.januar, skjæringstidspunkt(31.januar))
    }

    @Test
    fun `skjæringstidspunkt med avviste, foreldet og feriedager`() {
        historie(
            utbetaling(1.januar, 5.januar, 1000.daglig,  100.prosent,  AG1),
            ferie(8.januar, 12.januar)
        )
        addTidligereUtbetaling(AG1, foreldetdager(15. januar, 19.januar))
        addTidligereUtbetaling(AG1, feriedager(22. januar, 26.januar))
        addTidligereUtbetaling(AG1, avvistedager(29. januar, 3.februar))
        addTidligereUtbetaling(AG1, navdager(5.februar, 10.februar))

        assertEquals(1.januar, skjæringstidspunkt(28.februar))
    }

    @Test
    fun `skjæringstidspunkt brytes opp av arbeidsdag`() {
        historie(
            utbetaling(1.januar, 5.januar, 1000.daglig,  100.prosent,  AG1),
            ferie(8.januar, 12.januar)
        )
        addTidligereUtbetaling(AG1, foreldetdager(15. januar, 19.januar))
        addTidligereUtbetaling(AG1, feriedager(22. januar, 26.januar))
        addTidligereUtbetaling(AG1, arbeidsdager(27.januar, 28.januar))
        addTidligereUtbetaling(AG1, navdager(29.januar, 10.februar))

        assertEquals(29.januar, skjæringstidspunkt(28.februar))
    }

    @Test
    fun `innledende ferie som avsluttes på fredag`() {
        historie(
            ferie(1.januar, 5.januar),
            utbetaling(8.januar, 12.januar, 1000.daglig,  100.prosent,  AG1)
        )

        assertEquals(8.januar, skjæringstidspunkt(12.januar))
    }
}
