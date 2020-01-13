package no.nav.helse.utbetalingstidslinje

import no.nav.helse.fixtures.*
import no.nav.helse.fixtures.HELG
import no.nav.helse.fixtures.NAV
import no.nav.helse.fixtures.UtbetalingstidslinjeInspektør
import no.nav.helse.fixtures.tidslinjeOf
import no.nav.helse.person.Arbeidsgiver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class ArbeidsgiverUtbetalingerTest {

    private var maksdato: LocalDate? = null
    private lateinit var inspektør: UtbetalingstidslinjeInspektør

    companion object {
        internal const val UNG_PERSON_FNR_2018 = "12020052345"
        internal const val PERSON_67_ÅR_FNR_2018 = "05015112345"
    }

    @Test
    internal fun `uavgrenset utbetaling`() {
        undersøke(UNG_PERSON_FNR_2018, 5.NAV, 2.HELG, 5.NAV)
        assertEquals(12, inspektør.size)
        assertEquals(10, inspektør.navDagTeller)
        assertEquals(2, inspektør.navHelgDagTeller)
        assertEquals(12000, inspektør.totalUtbetaling())
        assertEquals(12.desember, maksdato)
    }

    @Test
    internal fun `avgrenset betaling pga minimum inntekt`() {
        undersøke(UNG_PERSON_FNR_2018, 5.NAV(12.0), 2.HELG, 5.NAV)

        assertEquals(12, inspektør.size)
        assertEquals(5, inspektør.navDagTeller)
        assertEquals(2, inspektør.navHelgDagTeller)
        assertEquals(5, inspektør.avvistDagTeller)
        assertEquals(6000, inspektør.totalUtbetaling())
        assertEquals(19.desember, maksdato)
    }

    @Test
    internal fun `avgrenset betaling pga maksimum inntekt`() {
        undersøke(UNG_PERSON_FNR_2018, 5.NAV(3500.0), 2.HELG, 5.NAV)

        assertEquals(12, inspektør.size)
        assertEquals(10, inspektør.navDagTeller)
        assertEquals(2, inspektør.navHelgDagTeller)
        assertEquals(10805 + 6000, inspektør.totalUtbetaling())
        assertEquals(12.desember, maksdato)
    }

    @Test
    internal fun `avgrenset betaling pga oppbrukte sykepengedager`() {
        undersøke(PERSON_67_ÅR_FNR_2018,
            7.UTELATE,
            5.NAV, 2.HELG,
            5.NAV, 2.HELG,
            5.NAV, 2.HELG,
            5.NAV, 2.HELG,
            5.NAV, 2.HELG,
            5.NAV, 2.HELG,
            5.NAV, 2.HELG,
            5.NAV, 2.HELG,
            5.NAV, 2.HELG,
            5.NAV, 2.HELG,
            5.NAV, 2.HELG,
            5.NAV, 2.HELG,
            5.NAV, 2.HELG
            )

        assertEquals(91, inspektør.size)
        assertEquals(60, inspektør.navDagTeller)
        assertEquals(26, inspektør.navHelgDagTeller)
        assertEquals(5, inspektør.avvistDagTeller)
        assertEquals(60 * 1200, inspektør.totalUtbetaling())
        assertEquals(30.mars, maksdato)
    }

    @Test
    internal fun `avgrenset betaling pga oppbrukte sykepengedager i tillegg til beløpsgrenser`() {
        undersøke(PERSON_67_ÅR_FNR_2018,
            7.UTELATE,
            5.NAV, 2.HELG,
            5.NAV, 2.HELG,
            5.NAV, 2.HELG,
            5.NAV, 2.HELG,
            5.NAV(12.0), 2.HELG,
            5.NAV, 2.HELG,
            5.NAV, 2.HELG,
            5.NAV, 2.HELG,
            5.NAV, 2.HELG,
            5.NAV, 2.HELG,
            5.NAV, 2.HELG,
            5.NAV(3500.0), 2.HELG,
            5.NAV(3500.0), 2.HELG,
            5.NAV(1200.0), 2.HELG
            )

        assertEquals(98, inspektør.size)
        assertEquals(60, inspektør.navDagTeller)
        assertEquals(28, inspektør.navHelgDagTeller)
        assertEquals(10, inspektør.avvistDagTeller)
        assertEquals((50 * 1200) + (10 * 2161), inspektør.totalUtbetaling())
        assertEquals(6.april, maksdato)
    }

    private fun undersøke(fnr: String, vararg dager: Triple<Int, Utbetalingstidslinje.(Double, LocalDate) -> Unit, Double>) {
        val tidslinje = tidslinjeOf(*dager)
        val arbeidsgiver = Arbeidsgiver("88888888")
        val historiskTidslinje = tidslinjeOf()
        ArbeidsgiverUtbetalinger(mapOf(arbeidsgiver to tidslinje), historiskTidslinje, Alder(fnr)).also {
            it.beregn()
            maksdato = it.maksdato()
        }

        inspektør = UtbetalingstidslinjeInspektør(arbeidsgiver.peekTidslinje()).result()

    }

}
