package no.nav.helse
import no.nav.helse.tournament.historiskDagturnering
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SykdomstidslinjeTestHelpersTest {

    @Test
    internal fun `enkeltdager`() {
        assertEquals(1, 1.sykedager.length())
        assertEquals(2, 2.sykedager.length())
    }

    @Test
    internal fun `sammenhengende dager`() {
        (2.sykedager + 2.sykedager).also {
            assertEquals(4, it.length())
        }.flatten().also {
            assertEquals(it[0].dagen.plusDays(1), it[1].dagen)
            assertEquals(it[1].dagen.plusDays(1), it[2].dagen)
            assertEquals(it[2].dagen.plusDays(1), it[3].dagen)
        }
    }

    @Test
    internal fun `periode med egendefinert startdato`() {
        2.sykedager.fra(mandag).also {
            assertEquals(mandag, it.førsteDag())
            assertEquals(tirsdag, it.sisteDag())
        }

        (2.sykedager.fra(torsdag) + 2.sykedager).also {
            assertEquals(torsdag, it.førsteDag())
            assertEquals(søndag, it.sisteDag())
        }

        (2.sykedager.fra(mandag) + 2.sykedager).also {
            assertEquals(4, it.length())
        }.flatten().also {
            assertEquals(it[0].dagen.plusDays(1), it[1].dagen)
            assertEquals(it[1].dagen.plusDays(1), it[2].dagen)
            assertEquals(it[2].dagen.plusDays(1), it[3].dagen)
        }

        (2.sykedager.fra(torsdag) + 2.sykHelgdager + 1.sykedager).merge(2.feriedager.fra(onsdag), historiskDagturnering).also {
            assertEquals(6, it.length())
        }.flatten().also {
            assertEquals(onsdag, it.first().dagen)
            assertEquals(søndag.plusDays(1), it.last().dagen)
        }
    }

    @Test
    internal fun `overlappende perioder`() {
        4.sykedager.fra(mandag).merge(2.feriedager.fra(mandag), historiskDagturnering).also {
            assertEquals(4, it.length())
        }.flatten().also {
            assertEquals(it[0].dagen.plusDays(1), it[1].dagen)
            assertEquals(it[1].dagen.plusDays(1), it[2].dagen)
            assertEquals(it[2].dagen.plusDays(1), it[3].dagen)
        }
    }
}
