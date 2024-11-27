package no.nav.helse.utbetalingstidslinje

import no.nav.helse.inspectors.inspektør
import no.nav.helse.testhelpers.AP
import no.nav.helse.testhelpers.ARB
import no.nav.helse.testhelpers.FRI
import no.nav.helse.testhelpers.HELG
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.UTELATE
import no.nav.helse.testhelpers.tidslinjeOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class TidslinjefusjonTest {
    @Test
    fun `slå sammen tilstøtende betalingstider`() {
        val inspektør = (tidslinjeOf(10.NAV) + tidslinjeOf(10.UTELATE, 10.NAV)).inspektør
        assertEquals(20, inspektør.size)
        assertEquals(15, inspektør.navDagTeller)
        assertEquals(5, inspektør.navHelgDagTeller)
    }

    @Test
    fun `slå sammen betalingstidslinjer som ikke er tilstøtende`() {
        val inspektør = (tidslinjeOf(15.NAV) + tidslinjeOf(15.UTELATE, 3.UTELATE, 15.NAV)).inspektør
        assertEquals(33, inspektør.size)
        assertEquals(22, inspektør.navDagTeller)
        assertEquals(8, inspektør.navHelgDagTeller)
        assertEquals(0, inspektør.ukjentDagTeller)
        assertEquals(3, inspektør.arbeidsdagTeller)
        assertEquals(0, inspektør.fridagTeller)
    }

    @Test
    fun `slå sammen ikke-tilstøtende betalingstider med helger`() {
        val inspektør = (tidslinjeOf(15.NAV) + tidslinjeOf(15.UTELATE, 7.UTELATE, 15.NAV)).inspektør
        assertEquals(37, inspektør.size)
        assertEquals(22, inspektør.navDagTeller)
        assertEquals(8, inspektør.navHelgDagTeller)
        assertEquals(0, inspektør.ukjentDagTeller)
        assertEquals(5, inspektør.arbeidsdagTeller)
        assertEquals(2, inspektør.fridagTeller)
    }

    @Test
    fun `overskriver ikke dager`() {
        // den siste tidslinjen består av to mindre dager.
        // når utbetalingstidslinjen legges sammen så vil den siste tidslinjen utvides med to dager, og de to siste dagene er en helg
        val inspektør = (tidslinjeOf(5.NAV, 2.ARB) + tidslinjeOf(5.FRI)).inspektør
        assertEquals(7, inspektør.size)
        assertEquals(5, inspektør.navDagTeller)
        assertEquals(2, inspektør.arbeidsdagTeller)
    }

    @Test
    fun `bli med i flere utbetalingstidslinjer`() {
        val inspektør =
            (
                tidslinjeOf(15.NAV) +
                    tidslinjeOf(15.UTELATE, 7.UTELATE, 15.NAV) +
                    tidslinjeOf(15.UTELATE, 22.UTELATE, 7.UTELATE, 15.NAV)
            ).inspektør
        assertEquals(59, inspektør.size)
        assertEquals(33, inspektør.navDagTeller)
        assertEquals(12, inspektør.navHelgDagTeller)
        assertEquals(0, inspektør.ukjentDagTeller)
        assertEquals(10, inspektør.arbeidsdagTeller)
        assertEquals(4, inspektør.fridagTeller)
    }

    // The following tests handle overlapping Utbetalingstidslinjer. This should only be for multiple arbeitsgivere

    @Test
    fun `NAV-utbetalinger har prioritet`() {
        val inspektør = (tidslinjeOf(15.ARB) + tidslinjeOf(15.NAV)).inspektør
        assertEquals(15, inspektør.size)
        assertEquals(11, inspektør.navDagTeller)
        assertEquals(4, inspektør.navHelgDagTeller)
        assertEquals(0, inspektør.arbeidsdagTeller)
    }

    @Test
    fun `NAV-utbetalinger for flere arbeidsgivere`() {
        val inspektør =
            (
                tidslinjeOf(
                    15.NAV,
                    15.ARB,
                ) + tidslinjeOf(10.ARB, 10.NAV, 10.ARB, 10.NAV)
            ).inspektør
        assertEquals(40, inspektør.size)
        assertEquals(23, inspektør.navDagTeller)
        assertEquals(7, inspektør.navHelgDagTeller)
        assertEquals(10, inspektør.arbeidsdagTeller)
        assertEquals(0, inspektør.ukjentDagTeller)
    }

    @Test
    fun `feriedag vs arbeidsdag`() {
        val result = tidslinjeOf(1.FRI) + tidslinjeOf(1.ARB)
        assertEquals(Utbetalingsdag.Fridag::class, result.single()::class)
    }

    @Test
    fun `dagens forrang`() {
        val inspektør =
            (
                tidslinjeOf(1.FRI, 1.ARB, 1.AP, 1.HELG, 1.NAV) +
                    tidslinjeOf(
                        1.UTELATE,
                        1.FRI,
                        1.ARB,
                        1.AP,
                        1.HELG,
                        1.NAV,
                    ) +
                    tidslinjeOf(
                        2.UTELATE,
                        1.FRI,
                        1.ARB,
                        1.AP,
                        1.HELG,
                        1.NAV,
                    ) +
                    tidslinjeOf(
                        3.UTELATE,
                        1.FRI,
                        1.ARB,
                        1.AP,
                        1.HELG,
                        1.NAV,
                    ) +
                    tidslinjeOf(
                        4.UTELATE,
                        1.FRI,
                        1.ARB,
                        1.AP,
                        1.HELG,
                        1.NAV,
                    )
            ).inspektør
        assertEquals(9, inspektør.size)
        assertEquals(3, inspektør.navDagTeller)
        assertEquals(3, inspektør.navHelgDagTeller)
        assertEquals(1, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(0, inspektør.arbeidsdagTeller)
        assertEquals(2, inspektør.fridagTeller)
    }

    @Test
    fun `legger til tom utbetalingstidslinje`() {
        val inspektør = (tidslinjeOf(10.NAV) + tidslinjeOf()).inspektør
        assertEquals(10, inspektør.size)
        assertEquals(8, inspektør.navDagTeller)
        assertEquals(2, inspektør.navHelgDagTeller)
    }

    @Test
    fun `legger utbetalingstidslinje til en tom tidslinje`() {
        val inspektør = (tidslinjeOf() + tidslinjeOf(10.NAV)).inspektør
        assertEquals(10, inspektør.size)
        assertEquals(8, inspektør.navDagTeller)
        assertEquals(2, inspektør.navHelgDagTeller)
    }

    @Test
    fun `adderer to tomme utbetalingstidslinjer`() {
        val inspektør = (tidslinjeOf() + tidslinjeOf()).inspektør
        assertEquals(0, inspektør.size)
    }
}
