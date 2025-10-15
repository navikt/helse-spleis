package no.nav.helse.utbetalingstidslinje

import java.util.UUID
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.testhelpers.AVV
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingstidslinje.MinsteinntektfilterTest.Minsteinntekt.OPPFYLLER_IKKE_KRAV_TIL_67
import no.nav.helse.utbetalingstidslinje.MinsteinntektfilterTest.Minsteinntekt.OPPFYLLER_KRAV_FØR_OG_ETTER_67
import no.nav.helse.utbetalingstidslinje.MinsteinntektfilterTest.Minsteinntekt.OPPFYLLER_KRAV_TIL_67_MEN_IKKE_ETTER
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MinsteinntektfilterTest {
    private companion object {
        private val sekstisyvårsdagen = 1.januar
    }

    @Test
    fun `avviser ikke dager dersom vurdert ok`() {
        val tidslinjer = listOf(tidslinjeOf(2.NAV, startDato = sekstisyvårsdagen))
        assertEquals(0, avvisteDager(tidslinjer, OPPFYLLER_KRAV_FØR_OG_ETTER_67).size)
    }

    @Test
    fun `ikke tilstrekkelig inntekt over 67 år`() {
        val tidslinjer = listOf(tidslinjeOf(2.NAV, startDato = sekstisyvårsdagen))
        val avvisteDager = avvisteDager(tidslinjer, OPPFYLLER_KRAV_TIL_67_MEN_IKKE_ETTER)
        assertEquals(1, avvisteDager.size)
        assertEquals(listOf(Begrunnelse.MinimumInntektOver67), avvisteDager.single().begrunnelser)
    }

    @Test
    fun `ikke tilstrekkelig inntekt til 67 år`() {
        val tidslinjer = listOf(tidslinjeOf(2.NAV, startDato = sekstisyvårsdagen))
        val avvisteDager = avvisteDager(tidslinjer, OPPFYLLER_IKKE_KRAV_TIL_67)
        assertEquals(2, avvisteDager.size)
        assertEquals(listOf(Begrunnelse.MinimumInntekt), avvisteDager.first().begrunnelser)
        assertEquals(listOf(Begrunnelse.MinimumInntektOver67), avvisteDager.last().begrunnelser)
    }

    @Test
    fun `avviser med flere begrunnelser`() {
        val tidslinjer = listOf(tidslinjeOf(1.AVV(dekningsgrunnlag = 1000, begrunnelse = Begrunnelse.ManglerOpptjening)))
        val avvisteDager = avvisteDager(tidslinjer, OPPFYLLER_IKKE_KRAV_TIL_67)
        assertEquals(1, avvisteDager.size)
        assertEquals(listOf(Begrunnelse.ManglerOpptjening, Begrunnelse.MinimumInntekt), avvisteDager.single().begrunnelser)
    }

    private enum class Minsteinntekt {
        OPPFYLLER_KRAV_FØR_OG_ETTER_67,
        OPPFYLLER_KRAV_TIL_67_MEN_IKKE_ETTER,
        OPPFYLLER_IKKE_KRAV_TIL_67
    }

    private fun avvisteDager(
        tidslinjer: List<Utbetalingstidslinje>,
        minsteinntektsituasjon: Minsteinntekt
    ): List<Utbetalingsdag.AvvistDag> {
        val erUnderMinsteinntektskravTilFylte67 = when (minsteinntektsituasjon) {
            OPPFYLLER_KRAV_FØR_OG_ETTER_67,
            OPPFYLLER_KRAV_TIL_67_MEN_IKKE_ETTER -> false

            OPPFYLLER_IKKE_KRAV_TIL_67 -> true
        }
        val erUnderMinsteinntektEtterFylte67 = when (minsteinntektsituasjon) {
            OPPFYLLER_KRAV_FØR_OG_ETTER_67 -> false
            OPPFYLLER_KRAV_TIL_67_MEN_IKKE_ETTER,
            OPPFYLLER_IKKE_KRAV_TIL_67 -> true
        }
        val arbeidsgivere = tidslinjer.mapIndexed { index, it ->
            Arbeidsgiverberegning(
                yrkesaktivitet = Arbeidsgiverberegning.Yrkesaktivitet.Arbeidstaker("a${index + 1}"),
                vedtaksperioder = listOf(
                    Vedtaksperiodeberegning(
                        vedtaksperiodeId = UUID.randomUUID(),
                        utbetalingstidslinje = it
                    )
                ),
                ghostOgAndreInntektskilder = emptyList()
            )
        }

        val avviste = arbeidsgivere.avvisMinsteinntekt(
            sekstisyvårsdagen = sekstisyvårsdagen,
            erUnderMinsteinntektskravTilFylte67 = erUnderMinsteinntektskravTilFylte67,
            erUnderMinsteinntektEtterFylte67 = erUnderMinsteinntektEtterFylte67
        )
        return avviste.flatMap {
            it.vedtaksperioder.single().utbetalingstidslinje.inspektør.avvistedager
        }
    }
}
