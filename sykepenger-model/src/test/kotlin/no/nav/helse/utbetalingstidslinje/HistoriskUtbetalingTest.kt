package no.nav.helse.utbetalingstidslinje

import no.nav.helse.fixtures.*
import no.nav.helse.fixtures.UtbetalingstidslinjeInspektør
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class HistoriskUtbetalingTest {
    private lateinit var builder: UtbetalingstidslinjerBuilder
    private lateinit var inspektør: UtbetalingstidslinjeInspektør

    @Test internal fun `enkelt historisk betaling`(){
        undersøke(1.januar, 31.desember, 1.mandag.to(1.fredag))
        assertEquals(5, inspektør.size)
        assertEquals(5, inspektør.navDagTeller)
    }

    @Test internal fun `utbetalingslinje spenner helgen`() {
        undersøke(1.januar, 31.desember, 1.mandag.to(2.fredag))
        assertEquals(12, inspektør.size)
        assertEquals(10, inspektør.navDagTeller)
        assertEquals(2, inspektør.navHelgDagTeller)
    }

    @Test internal fun `avkort første periode`() {
        undersøke(4.januar, 31.desember, 1.mandag.to(2.fredag))
        assertEquals(9, inspektør.size)
        assertEquals(7, inspektør.navDagTeller)
        assertEquals(2, inspektør.navHelgDagTeller)
    }

    @Test internal fun `avkort siste periode`() {
        undersøke(1.januar, 10.januar, 1.mandag.to(2.fredag))
        assertEquals(10, inspektør.size)
        assertEquals(8, inspektør.navDagTeller)
        assertEquals(2, inspektør.navHelgDagTeller)
    }

    @Test internal fun `håndtere flere perioder`() {
        undersøke(4.januar, 16.februar, 1.januar.to(12.januar), 23.januar.to(1.februar), 14.februar.to(22.februar) )
        assertEquals(3, builder.results().size)
        assertEquals(44, inspektør.size)
        assertEquals(18, inspektør.navDagTeller)
        assertEquals(4, inspektør.navHelgDagTeller)
    }

    @Test internal fun `avvis utbetalingslinjer etter periode`() {
        undersøke(4.januar, 31.januar, 1.januar.to(12.januar), 23.januar.to(1.februar), 14.februar.to(22.februar) )
        assertEquals(2, builder.results().size)
        assertEquals(28, inspektør.size)
        assertEquals(14, inspektør.navDagTeller)
        assertEquals(4, inspektør.navHelgDagTeller)
    }

    @Test internal fun `avvis utbetalingslinjer før periode`() {
        undersøke(20.januar, 16.februar, 1.januar.to(12.januar), 23.januar.to(1.februar), 14.februar.to(22.februar) )
        assertEquals(2, builder.results().size)
        assertEquals(25, inspektør.size)
        assertEquals(11, inspektør.navDagTeller)
        assertEquals(2, inspektør.navHelgDagTeller)
    }

    private fun undersøke(rangeFom: LocalDate, rangeTom: LocalDate, vararg perioder: Pair<LocalDate, LocalDate>) {
        undersøke(rangeFom, rangeTom, perioder.toList())
    }

    private fun undersøke(rangeFom: LocalDate, rangeTom: LocalDate, maksdato: LocalDate, vararg perioder: Pair<LocalDate, LocalDate>) {
        undersøke(rangeFom, rangeTom, perioder.toList(), maksdato)
    }

    private fun undersøke(rangeFom: LocalDate, rangeTom: LocalDate, perioder: List<Pair<LocalDate, LocalDate>>, maksdato: LocalDate = 31.desember(2019)) {
        builder = UtbetalingstidslinjerBuilder(rangeFom, rangeTom, perioder.map { (fom, tom) -> HistoriskUtbetaling(
            987654321,
            fom,
            tom
        ) })
        inspektør = UtbetalingstidslinjeInspektør(builder.results().reduce(Utbetalingstidslinje::plus)).result()
    }

}
