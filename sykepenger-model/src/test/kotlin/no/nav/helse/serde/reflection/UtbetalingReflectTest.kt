package no.nav.helse.serde.reflection

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingslinjer.Utbetaling
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class UtbetalingReflectTest {

    @Test internal fun `a`() {
        val map = UtbetalingReflect(Utbetaling(tidslinjeOf(1.NAV), 1.januar, Aktivitetslogg())).toMap()
        assertEquals(1.januar, map["arbeidsgiverUtbetalingslinjer"] ) //["linjer"][0]["fom"].asLocalDate)
    }
}
