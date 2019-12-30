package no.nav.helse.utbetalingstidslinje

import no.nav.helse.fixtures.*
import no.nav.helse.fixtures.UtbetalingstidslinjeInspektør
import no.nav.helse.sykdomstidslinje.Utbetalingslinje
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker

internal class ArvTidslinjeTest {

    @Test internal fun `enkel hverdags tidslinje`() {
        val inspektør = UtbetalingstidslinjeInspektør(Utbetalingslinje(1.mandag, 1.fredag, 1200).toTidslinje()).result()
        assertEquals(5, inspektør.size)
        assertEquals(5, inspektør.navDagTeller)
    }

    @Test internal fun `utbetalingslinje spenner helgen`() {
        val inspektør = UtbetalingstidslinjeInspektør(Utbetalingslinje(1.mandag, 2.fredag, 1200).toTidslinje()).result()
        assertEquals(12, inspektør.size)
        assertEquals(10, inspektør.navDagTeller)
        assertEquals(2, inspektør.navHelgDagTeller)
    }
}
