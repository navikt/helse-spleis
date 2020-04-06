package no.nav.helse.serde.reflection

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingslinjer.Utbetaling
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class UtbetalingReflectTest {

    protected companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val ORGNUMMER = "987654321"
    }

    private lateinit var map: MutableMap<String, MutableMap<String, out Any?>>

    @Test internal fun `Reflect mapper riktige verdier`() {
        map = UtbetalingReflect(Utbetaling(
            UNG_PERSON_FNR_2018,
            ORGNUMMER,
            tidslinjeOf(4.NAV), 4.januar, Aktivitetslogg())
        ).toMap()
        assertUtbetalingslinjer(ORGNUMMER, "mottaker")
        assertUtbetalingslinjer("ARBEIDSGIVER", "mottakertype")
        assertUtbetalingslinjer("NY", "linjertype")
        assertUtbetalingslinjer(-877852851, "sjekksum")
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

    private fun assertUtbetalingslinjer(expected: Any?, key: String) {
        assertEquals(expected, ((map
            ["arbeidsgiverUtbetalingslinjer"] as Map<String, String>)
            [key]
        ))
    }
}
