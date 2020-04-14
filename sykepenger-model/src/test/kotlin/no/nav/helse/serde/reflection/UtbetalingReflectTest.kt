package no.nav.helse.serde.reflection

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class UtbetalingReflectTest {

    protected companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val ORGNUMMER = "987654321"
    }

    private lateinit var map: MutableMap<String, Any?>

    @Test internal fun `Reflect mapper riktige verdier`() {
        map = UtbetalingReflect(Utbetaling(
            UNG_PERSON_FNR_2018,
            ORGNUMMER,
            tidslinjeMedDagsats(tidslinjeOf(4.NAV)),
            4.januar,
            Aktivitetslogg(),
            null
        )
        ).toMap()
        assertUtbetalingslinjer(ORGNUMMER, "mottaker")
        assertUtbetalingslinjer("SPREF", "fagområde")
        assertUtbetalingslinjer("NY", "endringskode")
        assertUtbetalingslinjer(-874556451, "sjekksum")
        assertUtbetalingslinje(0, 1.januar.toString(), "fom")
        assertUtbetalingslinje(0, 4.januar.toString(), "tom")
        assertUtbetalingslinje(0, 1, "delytelseId")
        assertUtbetalingslinje(0, null, "refDelytelseId")
        assertUtbetalingslinje(0, "NY", "endringskode")
        assertUtbetalingslinje(0, "SPREFAG-IOP", "klassekode")
    }

    private fun assertUtbetalingslinje(index: Int, expected: Any?, key: String) {
        assertEquals(expected, ((map
            ["arbeidsgiverOppdrag"] as Map<String, String>)
            ["linjer"] as List<Map<String, String>>)
            [index]
            [key]
        )
    }

    private fun assertUtbetalingslinjer(expected: Any?, key: String) {
        assertEquals(expected, ((map
            ["arbeidsgiverOppdrag"] as Map<String, String>)
            [key]
        ))
    }

    private fun tidslinjeMedDagsats(tidslinje: Utbetalingstidslinje) =
        tidslinje.onEach { if (it is NavDag) it.utbetaling = it.inntekt.toInt()  }
}
