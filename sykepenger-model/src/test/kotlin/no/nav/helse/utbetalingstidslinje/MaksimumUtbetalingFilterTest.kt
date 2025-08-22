package no.nav.helse.utbetalingstidslinje

import java.util.UUID
import no.nav.helse.Grunnbeløp.Companion.`6G`
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class MaksimumUtbetalingFilterTest {
    private lateinit var aktivitetslogg: Aktivitetslogg

    @BeforeEach
    internal fun setup() {
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `når inntekt er under 6G blir utbetaling lik inntekt`() {
        val inntekt = 1200.daglig
        val tidslinje = tidslinjeOf(12.NAV(inntekt)).betal(inntekt)
        assertEquals(12000.0, tidslinje.inspektør.totalUtbetaling())
    }

    @Test
    fun `når inntekt er over 6G blir utbetaling lik 6G`() {
        val inntektOver6G = 3500.daglig
        val `6G`= 2161.daglig
        val tidslinje = tidslinjeOf(12.NAV(inntektOver6G)).betal(`6G`)
        assertEquals(21610.0, tidslinje.inspektør.totalUtbetaling())
    }

    @Test
    fun `utbetaling for tidslinje med ulike daginntekter blir kalkulert per dag`() {
        val sykepengegrunnlag= 3500.daglig
        val tidslinje = tidslinjeOf(12.NAV(3500.0), 14.NAV(1200.0))
        val m = assertThrows<IllegalStateException> {
            tidslinje.betal(sykepengegrunnlag)
        }
        assertEquals("Det er et restbeløp på kr [Årlig: 598000.0, Månedlig: 49833.333333333336, Daglig: 2300.0] etter all fordeling", m.message)
    }

    @Test
    fun `selv om utbetaling blir begrenset til 6G får utbetaling for tidslinje med gradert sykdom gradert utbetaling`() {
        val sykepengegrunnlag= `6G`.beløp(1.januar)
        val tidslinje = tidslinjeOf(12.NAV(3500.0, 50.0)).betal(sykepengegrunnlag)
        assertEquals(10800.0, tidslinje.inspektør.totalUtbetaling())
    }

    @Test
    fun `utbetaling for tidslinje med gradert sykdom får gradert utbetaling`() {
        val sykepengegrunnlag= 1200.daglig
        val tidslinje = tidslinjeOf(12.NAV(1200.0, 50.0)).betal(sykepengegrunnlag)
        assertEquals(6000.0, tidslinje.inspektør.totalUtbetaling())
        assertTrue(aktivitetslogg.aktiviteter.isNotEmpty())
        assertFalse(aktivitetslogg.harVarslerEllerVerre())
    }

    private fun Utbetalingstidslinje.betal(sykepengegrunnlag: Inntekt): Utbetalingstidslinje {
        val input = listOf(
            Arbeidsgiverberegning(
                orgnummer = "a1",
                vedtaksperioder = listOf(
                    Vedtaksperiodeberegning(
                        vedtaksperiodeId = UUID.randomUUID(),
                        utbetalingstidslinje = this
                    )
                ),
                ghostOgAndreInntektskilder = emptyList()
            )
        )
        val filter = MaksimumUtbetalingFilter(sykepengegrunnlag, false, aktivitetslogg)

        return filter
            .filter(input, this.periode())
            .single()
            .vedtaksperioder
            .single()
            .utbetalingstidslinje
    }
}
