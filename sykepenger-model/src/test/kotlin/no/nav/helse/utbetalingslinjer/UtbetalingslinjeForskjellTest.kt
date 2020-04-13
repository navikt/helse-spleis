package no.nav.helse.utbetalingslinjer

import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.utbetalingslinjer.Fagområde.SPREF
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class UtbetalingslinjeForskjellTest {

    protected companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val ORGNUMMER = "987654321"
    }

    @Test internal fun `helt separate utbetalingslinjer`() {
        val original = linjer(1.januar to 5.januar)
        val recalculated = linjer(5.februar to 9.februar)
        val actual = recalculated forskjell original
        assertUtbetalinger(linjer(5.februar to 9.februar), actual)
        assertNotEquals(original.referanse, actual.referanse)
        assertEquals(Linjetype.NY, actual.linjertype)
    }

    @Test internal fun `fullstendig overskriv`() {
        val original = linjer(8.januar to 13.januar)
        val recalculated = linjer(1.januar to 9.februar)
        val actual = recalculated forskjell original
        assertUtbetalinger(linjer(1.januar to 9.februar), actual)
        assertEquals(original.referanse, actual.referanse)
        assertEquals(Linjetype.UEND, actual.linjertype)
    }

    @Test internal fun `ny tom`() {
        val original = linjer(1.januar to 5.januar)
        val recalculated = linjer(1.januar to 13.januar)
        val actual = recalculated forskjell original
        assertUtbetalinger(linjer(1.januar to 13.januar), actual)
        assertEquals(original.referanse, actual.referanse)
        assertEquals(Linjetype.UEND, actual.linjertype)
        assertEquals(Linjetype.ENDR, actual[0].linjetype)
    }

    @Test internal fun `bare flere utbetalingslinjer`() {
        val original = linjer(1.januar to 5.januar)
        val recalculated = linjer(1.januar to 5.januar, 15.januar to 19.januar)
        val actual = recalculated forskjell original
        assertUtbetalinger(recalculated, actual)
        assertEquals(original.referanse, actual.referanse)
        assertEquals(Linjetype.UEND, actual.linjertype)
        assertEquals(Linjetype.UEND, actual[0].linjetype)
        assertEquals(Linjetype.NY, actual[1].linjetype)
    }

    @Test internal fun `grad endres`() {
        val original = linjer(1.januar to 5.januar)
        val recalculated = linjer(1.januar to 5.januar grad 80, 15.januar to 19.januar)
        val actual = recalculated forskjell original
        assertUtbetalinger(recalculated, actual)
        assertEquals(original.referanse, actual.referanse)
        assertEquals(Linjetype.UEND, actual.linjertype)
        assertEquals(Linjetype.NY, actual[0].linjetype)
        assertEquals(Linjetype.NY, actual[1].linjetype)
        assertEquals(original[0].id + 1, actual[0].id)  // chained off of last of original
        assertEquals(actual[0].id + 1, actual[1].id)
    }

    @Test internal fun `dagsats endres`() {
        val original = linjer(1.januar to 5.januar)
        val recalculated = linjer(1.januar to 5.januar dagsats 1000, 15.januar to 19.januar)
        val actual = recalculated forskjell original
        assertUtbetalinger(recalculated, actual)
        assertEquals(original.referanse, actual.referanse)
        assertEquals(Linjetype.UEND, actual.linjertype)
        assertEquals(Linjetype.NY, actual[0].linjetype)
        assertEquals(Linjetype.NY, actual[1].linjetype)
        assertEquals(original[0].id + 1, actual[0].id)  // chained off of last of original
        assertEquals(actual[0].id + 1, actual[1].id)
    }

    @Test internal fun `potpourri`() {
        val original = linjer(
            1.januar to 5.januar,
            6.januar to 12.januar grad 50,
            13.januar to 19.januar grad 80
        )
        val recalculated = linjer(
            1.januar to 5.januar,
            6.januar to 17.januar grad 50,  // extended tom
            18.januar to 19.januar grad 80,
            1.februar to 9.februar
        )
        val actual = recalculated forskjell original
        assertUtbetalinger(recalculated, actual)
        assertEquals(original.referanse, actual.referanse)
        assertEquals(Linjetype.UEND, actual.linjertype)
        assertEquals(Linjetype.UEND, actual[0].linjetype)
        assertEquals(Linjetype.ENDR, actual[1].linjetype)
        assertEquals(Linjetype.NY, actual[2].linjetype)
        assertEquals(Linjetype.NY, actual[3].linjetype)
        assertEquals(original[1].id, actual[1].id)      // picks up id from original
        assertEquals(original[2].id + 1, actual[2].id)  // chained off of last of original
        assertEquals(actual[2].id + 1, actual[3].id)
    }

    @Test internal fun `potpourri2`() {
        val original = linjer(
            1.januar to 5.januar,
            6.januar to 12.januar grad 50,
            13.januar to 19.januar grad 80,
            1.februar to 3.februar,
            4.februar to 6.februar,
            7.februar to 8.februar
        )
        val recalculated = linjer(
            1.januar to 5.januar,
            6.januar to 17.januar grad 50,  // extended tom
            18.januar to 19.januar grad 80,
            1.februar to 9.februar
        )
        val actual = recalculated forskjell original
        assertUtbetalinger(recalculated, actual)
        assertEquals(original.referanse, actual.referanse)
        assertEquals(Linjetype.UEND, actual.linjertype)
        assertEquals(Linjetype.UEND, actual[0].linjetype)
        assertEquals(Linjetype.ENDR, actual[1].linjetype)
        assertEquals(Linjetype.NY, actual[2].linjetype)
        assertEquals(Linjetype.NY, actual[3].linjetype)
        assertEquals(original[1].id, actual[1].id)      // picks up id from original
        assertEquals(original[5].id + 1, actual[2].id)  // chained off of last of original
        assertEquals(actual[2].id + 1, actual[3].id)
    }

    private val Utbetalingslinjer.linjertype get() = this.get<Linjetype>("linjertype")

    private val Utbetalingslinje.linjetype get() = this.get<Linjetype>("linjetype")

    private val Utbetalingslinje.id get() = this.get<Int>("delytelseId")

    private val Utbetalingslinjer.referanse get() = this.get<String>("utbetalingsreferanse")

    private fun assertUtbetalinger(expected: Utbetalingslinjer, actual: Utbetalingslinjer) {
        assertEquals(expected.size, actual.size, "Utbetalingslinjer er i forskjellige størrelser")
        (expected zip actual).forEach { (a, b) ->
            assertEquals(a.fom, b.fom, "fom stemmer ikke overens")
            assertEquals(a.tom, b.tom, "tom stemmer ikke overens")
            assertEquals(a.dagsats, b.dagsats, "dagsats stemmer ikke overens")
            assertEquals(a.grad, b.grad, "grad stemmer ikke overens")
        }
    }

    private fun linjer(vararg linjer: TestUtbetalingslinje) =
        Utbetalingslinjer(ORGNUMMER, SPREF, linjer.map { it.asUtbetalingslinje() }).also {
            it.zipWithNext { a, b -> b.linkTo(a) }
        }

    private fun linjer(vararg linjer: Utbetalingslinje) =
        Utbetalingslinjer(ORGNUMMER, SPREF, linjer.toList()).also {
            it.zipWithNext { a, b -> b.linkTo(a) }
        }

    private inner class TestUtbetalingslinje(
        private val fom: LocalDate,
        private val tom: LocalDate
    ) {
        private var grad: Double = 100.0
        private var dagsats = 1200

        internal infix fun grad(percentage: Number): TestUtbetalingslinje {
            grad = percentage.toDouble()
            return this
        }

        internal infix fun dagsats(amount: Int): TestUtbetalingslinje {
            dagsats = amount
            return this
        }

        internal infix fun forskjell(other: TestUtbetalingslinje) = this forskjell other.asUtbetalingslinjer()

        internal infix fun forskjell(other: Utbetalingslinjer) = this.asUtbetalingslinjer() forskjell other

        internal fun asUtbetalingslinje() = Utbetalingslinje(fom, tom, dagsats, grad)

        private fun asUtbetalingslinjer() = linjer(asUtbetalingslinje())
    }

    private infix fun LocalDate.to(other: LocalDate) = TestUtbetalingslinje(this, other)
}

