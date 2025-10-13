package no.nav.helse.utbetalingstidslinje

import java.util.*
import no.nav.helse.Grunnbeløp.Companion.`2G`
import no.nav.helse.Grunnbeløp.Companion.halvG
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.testhelpers.AVV
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingstidslinje.MinsteinntektfilterTest.Minsteinntekt.OPPFYLLER_IKKE_KRAV_TIL_67
import no.nav.helse.utbetalingstidslinje.MinsteinntektfilterTest.Minsteinntekt.OPPFYLLER_KRAV_FØR_OG_ETTER_67
import no.nav.helse.utbetalingstidslinje.MinsteinntektfilterTest.Minsteinntekt.OPPFYLLER_KRAV_TIL_67_MEN_IKKE_ETTER
import no.nav.helse.utbetalingstidslinje.Minsteinntektsvurdering.Companion.lagMinsteinntektsvurdering
import no.nav.helse.økonomi.Inntekt.Companion.daglig
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
        val skjæringstidspunkt = 1.januar
        val inntekt = when (minsteinntektsituasjon) {
            OPPFYLLER_IKKE_KRAV_TIL_67 -> halvG.minsteinntekt(skjæringstidspunkt) - 1.daglig
            OPPFYLLER_KRAV_TIL_67_MEN_IKKE_ETTER -> `2G`.minsteinntekt(skjæringstidspunkt) - 1.daglig
            OPPFYLLER_KRAV_FØR_OG_ETTER_67 -> `2G`.minsteinntekt(skjæringstidspunkt)
        }
        val minsteinntektsvurdering = lagMinsteinntektsvurdering(skjæringstidspunkt, inntekt)

        val filter = Minsteinntektfilter(
            sekstisyvårsdagen = sekstisyvårsdagen,
            erUnderMinsteinntektskravTilFylte67 = minsteinntektsvurdering.erUnderMinsteinntektskravTilFylte67,
            erUnderMinsteinntektEtterFylte67 = minsteinntektsvurdering.erUnderMinsteinntektEtterFylte67
        )
        val arbeidsgivere = tidslinjer.mapIndexed { index, it ->
            Arbeidsgiverberegning(
                yrkesaktivitet = Arbeidsgiverberegning.Yrkesaktivitet.Arbeidstaker("a${index+1}"),
                vedtaksperioder = listOf(
                    Vedtaksperiodeberegning(
                        vedtaksperiodeId = UUID.randomUUID(),
                        utbetalingstidslinje = it
                    )
                ),
                ghostOgAndreInntektskilder = emptyList()
            )
        }
        val avviste = filter.filter(arbeidsgivere)
        return avviste.flatMap {
            it.vedtaksperioder.single().utbetalingstidslinje.inspektør.avvistedager
        }
    }
}
