package no.nav.helse.utbetalingslinjer

import no.nav.helse.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

internal class UtbetalingslinjeTest {

    @Test
    fun `kutte helg`() {
        val linje =
            Utbetalingslinje(fom = 1.januar(2018), tom = 7.januar(2018), grad = null, beløp = null)

        val nyLinje = linje.kuttHelg() ?: fail { "forventet linje" }
        assertEquals(1.januar(2018), nyLinje.inspektør.fom)
        assertEquals(5.januar(2018), nyLinje.inspektør.tom)
    }

    @Test
    fun `kutte helg - linje forsvinner`() {
        val linje =
            Utbetalingslinje(fom = 6.januar(2018), tom = 7.januar(2018), grad = null, beløp = null)

        assertNull(linje.kuttHelg())
    }
}

internal val Utbetalingslinje.inspektør
    get() = UtbetalingslinjeInspektør(this)

internal class UtbetalingslinjeInspektør(linje: Utbetalingslinje) {
    val fom = linje.fom
    val tom = linje.tom
}
