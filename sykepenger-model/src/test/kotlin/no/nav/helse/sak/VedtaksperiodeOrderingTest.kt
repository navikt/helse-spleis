package no.nav.helse.sak

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtaksperiodeOrderingTest {

    companion object {
        const val FØR = -1
        const val ETTER = 1
        const val LIK = 0
    }

    @Test
    fun `Left og Right har samme fom og tom betyr at de sorteres likt`() {
        assertEquals(
            LIK, Vedtaksperiode.compare(
                leftFom = LocalDate.now(),
                leftTom = LocalDate.now(),
                rightFom = LocalDate.now(),
                rightTom = LocalDate.now()
            )
        )
    }

    @Test
    fun `Left og Right mangler fom og tom betyr at de sorteres likt`() {
        assertEquals(
            LIK, Vedtaksperiode.compare(
                leftFom = null,
                leftTom = null,
                rightFom = null,
                rightTom = null
            )
        )
    }

    @Test
    fun `Left har fom før Right betyr at Left er først`() {
        val leftFom = LocalDate.now().minusDays(1)
        val rightFom = leftFom.plusDays(2)
        assertTrue(
            FØR >= Vedtaksperiode.compare(
                leftFom = leftFom,
                leftTom = LocalDate.now(),
                rightFom = rightFom,
                rightTom = LocalDate.now()
            )
        )
    }

    @Test
    fun `Left har fom etter Right betyr at Right er først`() {
        val leftFom = LocalDate.now().minusDays(1)
        val rightFom = leftFom.minusDays(2)
        assertTrue(
            ETTER <= Vedtaksperiode.compare(
                leftFom = leftFom,
                leftTom = LocalDate.now(),
                rightFom = rightFom,
                rightTom = LocalDate.now()
            )
        )
    }

    @Test
    fun `Left har ikke fom, Right har fom betyr at Left er først`() {
        val rightFom = LocalDate.now().minusDays(1)
        assertTrue(
            FØR >= Vedtaksperiode.compare(
                leftFom = null,
                leftTom = LocalDate.now(),
                rightFom = rightFom,
                rightTom = LocalDate.now()
            ),
            "Left skal sorteres før Right"
        )
    }

    @Test
    fun `Left har fom, Right har ikke fom betyr at Right er først`() {
        val leftFom = LocalDate.now().minusDays(1)
        assertTrue(
            ETTER <= Vedtaksperiode.compare(
                leftFom = leftFom,
                leftTom = LocalDate.now(),
                rightFom = null,
                rightTom = LocalDate.now()
            )
        )
    }

    @Nested
    inner class `Left og Right har samme fom`() {
        val leftFom = LocalDate.now().minusDays(1)
        val rightFom = leftFom

        @Test
        fun `Left har tom før Right betyr at Left er først`() {
            val leftTom = leftFom.plusDays(1)
            val rightTom = leftFom.plusWeeks(1)

            assertTrue(
                FØR >= Vedtaksperiode.compare(
                    leftFom = leftFom,
                    leftTom = leftTom,
                    rightFom = rightFom,
                    rightTom = rightTom
                )
            )
        }

        @Test
        fun `Left har tom etter Right betyr at Right er først`() {
            val leftTom = leftFom.plusWeeks(1)
            val rightTom = leftFom.minusDays(1)

            assertTrue(
                ETTER <= Vedtaksperiode.compare(
                    leftFom = leftFom,
                    leftTom = leftTom,
                    rightFom = rightFom,
                    rightTom = rightTom
                )
            )
        }

        @Test
        fun `Left har ikke tom, Right har tom betyr at Right er først`() {
            val rightTom = rightFom.plusWeeks(1)

            assertTrue(
                ETTER <= Vedtaksperiode.compare(
                    leftFom = leftFom,
                    leftTom = null,
                    rightFom = rightFom,
                    rightTom = rightTom
                )
            )
        }

        @Test
        fun `Left har tom, Right har ikke tom betyr at Left er først`() {
            val leftTom = leftFom.plusWeeks(1)

            assertTrue(
                FØR >= Vedtaksperiode.compare(
                    leftFom = leftFom,
                    leftTom = leftTom,
                    rightFom = rightFom,
                    rightTom = null
                )
            )
        }
    }
}
