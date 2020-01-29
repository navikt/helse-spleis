package no.nav.helse.utbetalingstidslinje

import no.nav.helse.testhelpers.fredag
import no.nav.helse.testhelpers.mandag
import no.nav.helse.testhelpers.tirsdag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalArgumentException

internal class UtbetalingslinjerJoinTest {

    @Test
    fun `tom utbetalingstidslinje forblir tom`() {
        assertUtbetalingslinjer(emptyList())
    }

    @Test
    fun `en utbetalingstidslinje forblir uendret`() {
        val utbetalingslinje =
            Utbetalingslinje(1.mandag, 1.fredag, 0)
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

