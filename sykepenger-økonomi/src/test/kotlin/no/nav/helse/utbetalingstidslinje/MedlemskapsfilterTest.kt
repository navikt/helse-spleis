package no.nav.helse.utbetalingstidslinje

import java.util.*
import no.nav.helse.inspectors.inspektør
import no.nav.helse.testhelpers.AVV
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.tidslinjeOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MedlemskapsfilterTest {
    @Test
    fun `avviser ikke dager dersom vurdert ok`() {
        val tidslinjer = listOf(tidslinjeOf(1.NAV))
        assertEquals(0, avvisteDager(tidslinjer).size)
    }

    @Test
    fun `avviser dager uten medlemskap`() {
        val tidslinjer = listOf(tidslinjeOf(1.NAV))
        val avvisteDager = avvisteDager(tidslinjer, manglerMedlemskap = true)
        assertEquals(1, avvisteDager.size)
        assertEquals(listOf(Begrunnelse.ManglerMedlemskap), avvisteDager.single().begrunnelser)
    }

    @Test
    fun `avviser med flere begrunnelser`() {
        val tidslinjer = listOf(tidslinjeOf(1.AVV(dekningsgrunnlag = 1000, begrunnelse = Begrunnelse.MinimumInntekt)))
        val avvisteDager = avvisteDager(tidslinjer, manglerMedlemskap = true)
        assertEquals(1, avvisteDager.size)
        assertEquals(listOf(Begrunnelse.MinimumInntekt, Begrunnelse.ManglerMedlemskap), avvisteDager.single().begrunnelser)
    }

    private fun avvisteDager(
        tidslinjer: List<Utbetalingstidslinje>,
        manglerMedlemskap: Boolean = false
    ): List<Utbetalingsdag.AvvistDag> {
        val filter = Medlemskapsfilter(
            erMedlemAvFolketrygden = !manglerMedlemskap
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
