package no.nav.helse.utbetalingstidslinje

import java.util.*
import kotlin.collections.mapIndexed
import no.nav.helse.etterlevelse.BehandlingSubsumsjonslogg
import no.nav.helse.etterlevelse.Regelverkslogg.Companion.EmptyLog
import no.nav.helse.februar
import no.nav.helse.hendelser.Periode
import no.nav.helse.inspectors.UtbetalingstidslinjeInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.MinimumSykdomsgradsvurdering
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.testhelpers.AP
import no.nav.helse.testhelpers.ARB
import no.nav.helse.testhelpers.AVV
import no.nav.helse.testhelpers.FRI
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.tidslinjeOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SykdomsgradfilterTest {

    private lateinit var inspektør: UtbetalingstidslinjeInspektør
    private lateinit var aktivitetslogg: Aktivitetslogg

    private val subsumsjonslogg = BehandlingSubsumsjonslogg(EmptyLog, "fnr", "orgnr", UUID.randomUUID(), UUID.randomUUID())

    @Test
    fun `sykdomsgrad over 20 prosent`() {
        val tidslinjer = listOf(tidslinjeOf(16.AP, 5.NAV(1200, 50.0)))
        val periode = Periode(1.januar, 21.januar)
        undersøke(tidslinjer, periode)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(3, inspektør.navDagTeller)
        assertEquals(2, inspektør.navHelgDagTeller)
        assertFalse(aktivitetslogg.harVarslerEllerVerre())
    }

    @Test
    fun `alle dager fom første dag med total sykdomsgrad under 20 prosent skal avvises`() {
        val tidslinjer = listOf(tidslinjeOf(16.AP, 5.NAV(1200, 19.0), 10.NAV))
        val periode = Periode(1.januar, 31.januar)
        undersøke(tidslinjer, periode)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(3, inspektør.avvistDagTeller)
        assertEquals(4, inspektør.navHelgDagTeller)
        assertEquals(8, inspektør.navDagTeller)
        assertTrue(aktivitetslogg.harVarslerEllerVerre())
    }

    @Test
    fun `ikke warning når de avviste dagene er utenfor perioden`() {
        val tidslinjer = listOf(tidslinjeOf(16.AP, 5.NAV(1200, 19.0), 10.NAV))
        val periode = Periode(22.januar, 31.januar)
        undersøke(tidslinjer, periode)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(3, inspektør.avvistDagTeller)
        assertEquals(4, inspektør.navHelgDagTeller)
        assertEquals(8, inspektør.navDagTeller)
        assertFalse(aktivitetslogg.harVarslerEllerVerre())
    }

    @Test
    fun `ikke warning når de avviste dagene gjelder forrige arbeidsgiverperiode`() {
        val tidslinjer = listOf(tidslinjeOf(16.AP, 5.NAV(1200, 19.0), 20.ARB, 16.AP, 10.NAV))
        val periode = Periode(11.februar, 9.mars)
        undersøke(tidslinjer, periode)
        assertEquals(32, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(3, inspektør.avvistDagTeller)
        assertEquals(4, inspektør.navHelgDagTeller)
        assertEquals(8, inspektør.navDagTeller)
        assertFalse(aktivitetslogg.harVarslerEllerVerre())
    }

    @Test
    fun `avvis dager for begge tidslinjer`() {
        val tidslinjer = listOf(
            tidslinjeOf(16.AP, 5.NAV(1200, 19.0)),
            tidslinjeOf(16.AP, 5.NAV(1200, 19.0))
        )
        val periode = Periode(1.januar, 21.januar)
        val resultat = undersøke(tidslinjer, periode)
        assertEquals(3, resultat.inspektør(0).avvistDagTeller)
        assertEquals(2, resultat.inspektør(0).navHelgDagTeller)
        assertEquals(3, resultat.inspektør(1).avvistDagTeller)
        assertEquals(2, resultat.inspektør(1).navHelgDagTeller)
    }

    @Test
    fun `avviser utbetaling når samlet grad er under 20 prosent`() {
        val tidslinjer = listOf(
            tidslinjeOf(16.AP, 5.NAV, 1.NAV(1200, 39)),
            tidslinjeOf(16.AP, 5.NAV, 1.FRI)
        )
        val periode = Periode(1.januar, 22.januar)
        val resultat = undersøke(tidslinjer, periode)
        assertEquals(3, resultat.inspektør(0).navDagTeller)
        assertEquals(2, resultat.inspektør(0).navHelgDagTeller)
        assertEquals(1, resultat.inspektør(0).avvistDagTeller)
        assertEquals(3, resultat.inspektør(1).navDagTeller)
        assertEquals(2, resultat.inspektør(1).navHelgDagTeller)
        assertEquals(1, resultat.inspektør(1).fridagTeller)
    }

    @Test
    fun `avviser ikke utbetaling når samlet grad er minst 20 prosent`() {
        val tidslinjer = listOf(
            tidslinjeOf(16.AP, 5.NAV, 1.NAV(1200, 40)),
            tidslinjeOf(16.AP, 5.NAV, 1.FRI)
        )
        val periode = Periode(1.januar, 22.januar)
        val resultat = undersøke(tidslinjer, periode)
        assertEquals(4, resultat.inspektør(0).navDagTeller)
        assertEquals(2, resultat.inspektør(0).navHelgDagTeller)
        assertEquals(0, resultat.inspektør(0).avvistDagTeller)
        assertEquals(3, resultat.inspektør(1).navDagTeller)
        assertEquals(2, resultat.inspektør(1).navHelgDagTeller)
        assertEquals(1, resultat.inspektør(1).fridagTeller)
    }

    @Test
    fun `avviser ikke andre ytelser`() {
        val tidslinjer = listOf(
            tidslinjeOf(16.AP, 6.AVV(grad = 0, dekningsgrunnlag = 0, begrunnelse = Begrunnelse.AndreYtelserForeldrepenger))
        )
        val periode = Periode(1.januar, 22.januar)
        undersøke(tidslinjer, periode)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(6, inspektør.avvistDagTeller)
        assertEquals(listOf(Begrunnelse.AndreYtelserForeldrepenger), inspektør.begrunnelse(17.januar))
    }

    private fun undersøke(tidslinjer: List<Utbetalingstidslinje>, periode: Periode): List<Utbetalingstidslinje> {
        aktivitetslogg = Aktivitetslogg()
        val input = tidslinjer.mapIndexed { index, it ->
            Arbeidsgiverberegning(
                orgnummer = "a${index + 1}",
                vedtaksperioder = listOf(
                    Vedtaksperiodeberegning(
                        vedtaksperiodeId = UUID.randomUUID(),
                        utbetalingstidslinje = it
                    )
                ),
                ghostOgAndreInntektskilder = emptyList()
            )
        }
        val resultat = Sykdomsgradfilter(MinimumSykdomsgradsvurdering(), subsumsjonslogg, aktivitetslogg)
            .filter(input, periode)
            .map { it.samletVedtaksperiodetidslinje }
        inspektør = resultat.inspektør(0)
        return resultat
    }

    private fun List<Utbetalingstidslinje>.inspektør(index: Int) = this[index].inspektør
}
