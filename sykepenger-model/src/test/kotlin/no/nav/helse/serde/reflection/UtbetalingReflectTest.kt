package no.nav.helse.serde.reflection

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingslinjer.Utbetaling
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class UtbetalingReflectTest {
    private lateinit var map: MutableMap<String, MutableMap<String, out Any?>>

    @Test internal fun `a`() {
        map = UtbetalingReflect(Utbetaling(tidslinjeOf(4.NAV), 4.januar, Aktivitetslogg())).toMap()
        assertUtbetalingslinje(0, 1.januar, "fom")
        assertUtbetalingslinje(0, 4.januar, "tom")
        assertUtbetalingslinje(0, 1, "delytelseId")
        assertUtbetalingslinje(0, null, "refDelytelseId")
    }

    private fun assertUtbetalingslinje(index: Int, expected: Any?, key: String) {
        assertEquals(expected, ((map
            ["arbeidsgiverUtbetalingslinjer"] as Map<String, String>)
            ["linjer"] as List<Map<String, String>>)
            [index]
            [key]
        )
    }
}
