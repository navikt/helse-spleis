package no.nav.helse.utbetalingslinjer

import no.nav.helse.testhelpers.januar
import no.nav.helse.utbetalingslinjer.Fagområde.SPREF
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class UtbetalingForskjellTest {

    protected companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val ORGNUMMER = "987654321"
    }

    @Test internal fun `ingen forskjell`() {
        val original = 1.januar to 5.januar grad 100 dagsats 1200
        val recalculated = 1.januar to 5.januar grad 100 dagsats 1200
        val expected = Utbetalingslinjer(ORGNUMMER, SPREF)
        assertUtbetalinger(expected, recalculated forskjell original)
    }

    private fun assertUtbetalinger(expected: Utbetalingslinjer, actual: Utbetalingslinjer) {
        assertEquals(expected.size, actual.size, "Utbetalingslinjer are different sizes")
        (expected zip actual).forEach { (a, b) ->
            assertEquals(a.fom, b.fom, "fom stemmer ikke overens")
            assertEquals(a.tom, b.tom, "tom stemmer ikke overens")
            assertEquals(a.dagsats, b.dagsats, "dagsats stemmer ikke overens")
            assertEquals(a.grad, b.grad, "grad stemmer ikke overens")
        }
    }

    private class TestUtbetalingslinjer(
        private val fom: LocalDate,
        private val tom: LocalDate
    ) {
        private var grad: Double = 100.0
        private var dagsats = 1200

        internal infix fun grad(percentage: Number): TestUtbetalingslinjer {
            grad = percentage.toDouble()
            return this
        }

        internal infix fun dagsats(amount: Int): TestUtbetalingslinjer {
            dagsats = amount
            return this
        }

        internal infix fun forskjell(other: TestUtbetalingslinjer) = this forskjell other.asUtbetalingslinjer()

        internal infix fun forskjell(other: Utbetalingslinjer) = this.asUtbetalingslinjer() forskjell other

        private fun asUtbetalingslinjer() =
            Utbetalingslinjer(ORGNUMMER, SPREF, listOf(Utbetalingslinje(fom, tom, dagsats, grad)))
    }

    private infix fun LocalDate.to(other: LocalDate) = TestUtbetalingslinjer(this, other)
}

