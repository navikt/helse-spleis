package no.nav.helse.utbetalingstidslinje

import no.nav.helse.fixtures.*
import no.nav.helse.fixtures.UtbetalingstidslinjeInspektør
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class HistoriskUtbetalingTest {
    private lateinit var inspektør: UtbetalingstidslinjeInspektør

    @Test internal fun `enkelt historisk betaling`(){
        undersøke(1.januar, 31.desember, 1.mandag.to(1.fredag))
        Assertions.assertEquals(5, inspektør.size)
        Assertions.assertEquals(5, inspektør.navDagTeller)
    }

    @Test internal fun `utbetalingslinje spenner helgen`() {
        undersøke(1.januar, 31.desember, 1.mandag.to(2.fredag))
        Assertions.assertEquals(12, inspektør.size)
        Assertions.assertEquals(10, inspektør.navDagTeller)
        Assertions.assertEquals(2, inspektør.navHelgDagTeller)
    }

    @Test internal fun `avkort første periode`() {
        undersøke(4.januar, 31.desember, 1.mandag.to(2.fredag))
        Assertions.assertEquals(9, inspektør.size)
        Assertions.assertEquals(7, inspektør.navDagTeller)
        Assertions.assertEquals(2, inspektør.navHelgDagTeller)
    }

    @Test internal fun `avkort siste periode`() {
        undersøke(1.januar, 10.januar, 1.mandag.to(2.fredag))
        Assertions.assertEquals(10, inspektør.size)
        Assertions.assertEquals(8, inspektør.navDagTeller)
        Assertions.assertEquals(2, inspektør.navHelgDagTeller)
    }

    @Test internal fun `håndtere flere perioder`() {
        undersøke(4.januar, 16.februar, 1.januar.to(12.januar), 23.januar.to(1.februar), 14.februar.to(22.februar) )
        Assertions.assertEquals(44, inspektør.size)
        Assertions.assertEquals(18, inspektør.navDagTeller)
        Assertions.assertEquals(4, inspektør.navHelgDagTeller)
    }

    private fun undersøke(rangeFom: LocalDate, rangeTom: LocalDate, vararg perioder: Pair<LocalDate, LocalDate>) {
        val builder = UtbetalingstidslinjerBuilder(rangeFom, rangeTom, perioder.map { (fom, tom) -> HistoriskUtbetaling(fom, tom, 987654321) })
        inspektør = UtbetalingstidslinjeInspektør(builder.results().reduce(Utbetalingstidslinje::plus)).result()
    }

}
