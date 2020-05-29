package no.nav.helse.økonomi

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingstidslinje.MaksimumUtbetaling
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ØkonomiDagTest {

    @Test fun `Beløp er ikke 6G-begrenset`() {
        val a = tidslinjeOf(2.NAV(500))
        val b = tidslinjeOf(2.NAV(500))
        val c = tidslinjeOf(2.NAV(500))
        MaksimumUtbetaling(listOf(a, b, c), Aktivitetslogg()).betal()
        a.forEach {
            it.økonomi.toMap().also { map ->
                assertEquals(500, map["arbeidsgiverbeløp"])
                assertEquals(0, map["personbeløp"])
            }
        }
    }

    @Test fun `Beløp er 6G-begrenset`() {
        val a = tidslinjeOf(2.NAV(1200))
        val b = tidslinjeOf(2.NAV(1200))
        val c = tidslinjeOf(2.NAV(1200))
        MaksimumUtbetaling(listOf(a, b, c), Aktivitetslogg()).betal()
        a.forEach {
            it.økonomi.toMap().also { map ->
                assertEquals(721, map["arbeidsgiverbeløp"])
                assertEquals(0, map["personbeløp"])
            }
        }
        b.forEach {
            it.økonomi.toMap().also { map ->
                assertEquals(720, map["arbeidsgiverbeløp"])
                assertEquals(0, map["personbeløp"])
            }
        }
    }
}
