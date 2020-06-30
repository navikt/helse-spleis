package no.nav.helse.serde.reflection

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.økonomi.betal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class UtbetalingReflectTest {

    private companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val ORGNUMMER = "987654321"
    }

    private lateinit var map: MutableMap<String, Any?>

    @Test
    internal fun `Reflect mapper riktige verdier`() {
        map = UtbetalingReflect(
            Utbetaling(
                UNG_PERSON_FNR_2018,
                ORGNUMMER,
                tidslinjeMedDagsats(tidslinjeOf(4.NAV)),
                4.januar,
                Aktivitetslogg(),
                emptyList()
            )
        ).toMap()
        assertUtbetalingslinjer(ORGNUMMER, "mottaker")
        assertUtbetalingslinjer("SPREF", "fagområde")
        assertUtbetalingslinjer("NY", "endringskode")
        assertUtbetalingslinje(0, 1.januar.toString(), "fom")
        assertUtbetalingslinje(0, 4.januar.toString(), "tom")
        assertUtbetalingslinje(0, 1, "delytelseId")
        assertUtbetalingslinje(0, null, "refDelytelseId")
        assertUtbetalingslinje(0, "NY", "endringskode")
        assertUtbetalingslinje(0, "SPREFAG-IOP", "klassekode")
    }

    @Test
    internal fun `Reflect mapper riktige verdierm med opphør`() {
        val tidligereUtbetaling = Utbetaling(
            UNG_PERSON_FNR_2018,
            ORGNUMMER,
            tidslinjeMedDagsats(tidslinjeOf(4.NAV)),
            4.januar,
            Aktivitetslogg(),
            emptyList()
        )

        map = UtbetalingReflect(
            Utbetaling(
                UNG_PERSON_FNR_2018,
                ORGNUMMER,
                tidslinjeMedDagsats(tidslinjeOf(2.NAV)),
                2.januar,
                Aktivitetslogg(),
                listOf(tidligereUtbetaling)
            )
        ).toMap()

        assertUtbetalingslinjer(ORGNUMMER, "mottaker")
        assertUtbetalingslinjer("SPREF", "fagområde")
        assertUtbetalingslinjer("ENDR", "endringskode")
        assertUtbetalingslinje(0, 1.januar.toString(), "fom")
        assertUtbetalingslinje(0, 4.januar.toString(), "tom")
        assertUtbetalingslinje(0, "OPPH", "statuskode")
        assertUtbetalingslinje(0, 1.januar, "datoStatusFom")
        assertUtbetalingslinje(0, 1, "delytelseId")
        assertUtbetalingslinje(0, null, "refDelytelseId")
        assertUtbetalingslinje(0, "ENDR", "endringskode")
        assertUtbetalingslinje(0, "SPREFAG-IOP", "klassekode")

        assertUtbetalingslinje(1, 1.januar.toString(), "fom")
        assertUtbetalingslinje(1, 2.januar.toString(), "tom")
        assertUtbetalingslinje(1, null, "statuskode")
        assertUtbetalingslinje(1, null, "datoStatusFom")
        assertUtbetalingslinje(1, 2, "delytelseId")
        assertUtbetalingslinje(1, 1, "refDelytelseId")
        assertUtbetalingslinje(1, "NY", "endringskode")
        assertUtbetalingslinje(1, "SPREFAG-IOP", "klassekode")
    }

    private fun assertUtbetalingslinje(index: Int, expected: Any?, key: String) {
        assertEquals(
            expected, ((map
                ["arbeidsgiverOppdrag"] as Map<String, String>)
                ["linjer"] as List<Map<String, String>>)
                [index]
                [key]
        )
    }

    private fun assertUtbetalingslinjer(expected: Any?, key: String) {
        assertEquals(
            expected, ((map
                ["arbeidsgiverOppdrag"] as Map<String, String>)
                [key]
                )
        )
    }

    private fun tidslinjeMedDagsats(tidslinje: Utbetalingstidslinje) =
        tidslinje.onEach { if (it is NavDag) listOf(it.økonomi).betal(it.dato) }
}
