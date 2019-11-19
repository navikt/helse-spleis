package no.nav.helse.utbetalingstidslinje

import no.nav.helse.sykdomstidslinje.Utbetalingslinje
import no.nav.helse.sykdomstidslinje.joinForOppdrag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalArgumentException
import java.time.LocalDate

internal class UtbetalingslinjerJoinTest {

    @Test
    fun `tom utbetalingstidslinje forblir tom`() {
        assertUtbetalingslinjer(emptyList())
    }

    @Test
    fun `en utbetalingstidslinje forblir uendret`() {
        val utbetalingslinje = Utbetalingslinje(1.mandag, 1.fredag, 0)
        assertUtbetalingslinjer(listOf(utbetalingslinje), utbetalingslinje)
    }

    @Test
    fun `to tilstøtende utbetalingstidslinjer slås sammen`() {
        assertUtbetalingslinjer(
            listOf(
                Utbetalingslinje(1.mandag, 2.fredag, 0)
            ), Utbetalingslinje(1.mandag, 1.fredag, 0),
            Utbetalingslinje(2.mandag, 2.fredag, 0)
        )
    }

    @Test
    fun `to utbetalingstidslinjer med større opphold enn en helg forblir uendret`() {
        assertUtbetalingslinjer(
            listOf(
                Utbetalingslinje(1.mandag, 1.fredag, 0),
                Utbetalingslinje(2.tirsdag, 2.fredag, 0)
            ), Utbetalingslinje(1.mandag, 1.fredag, 0),
            Utbetalingslinje(2.tirsdag, 2.fredag, 0)
        )
    }

    @Test
    fun hybrid() {
        assertUtbetalingslinjer(
            listOf(
                Utbetalingslinje(1.mandag, 2.fredag, 0),
                Utbetalingslinje(3.tirsdag, 3.fredag, 0)
            ), Utbetalingslinje(1.mandag, 1.fredag, 0),
            Utbetalingslinje(2.mandag, 2.fredag, 0),
            Utbetalingslinje(3.tirsdag, 3.fredag, 0)
        )
    }

    @Test
    fun `uventet dagsats`() {
        assertThrows<IllegalArgumentException> {
            listOf(
                Utbetalingslinje(1.mandag, 1.fredag, 0),
                Utbetalingslinje(2.mandag, 2.fredag, 1)
            ).joinForOppdrag()
        }
    }

    private fun assertUtbetalingslinjer(expected: List<Utbetalingslinje>, vararg utbetalingslinjer: Utbetalingslinje) {
        val actual = utbetalingslinjer.toList().joinForOppdrag()
        assertEquals(expected.size, actual.size)
        assertEquals(expected, actual)
    }

}

internal val Int.mandag
    get() = LocalDate.of(2018, 1, 1)
        .plusWeeks(this.toLong() - 1L)
internal val Int.tirsdag get() = this.mandag.plusDays(1)
internal val Int.onsdag get() = this.mandag.plusDays(2)
internal val Int.torsdag get() = this.mandag.plusDays(3)
internal val Int.fredag get() = this.mandag.plusDays(4)
internal val Int.lørdag get() = this.mandag.plusDays(5)
internal val Int.søndag get() = this.mandag.plusDays(6)

