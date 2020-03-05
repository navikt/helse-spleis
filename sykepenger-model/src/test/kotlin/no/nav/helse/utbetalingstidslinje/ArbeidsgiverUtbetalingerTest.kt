package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Person
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class ArbeidsgiverUtbetalingerTest {

    private var maksdato: LocalDate? = null
    private lateinit var inspektør: UtbetalingstidslinjeInspektør
    private lateinit var aktivitetslogg: Aktivitetslogg

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
        assertTrue(aktivitetslogg.hasMessages())
        assertFalse(aktivitetslogg.hasWarnings())
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
        assertTrue(aktivitetslogg.hasWarnings())
        assertFalse(aktivitetslogg.hasErrors())
    }

    @Test
    internal fun `avgrenset betaling pga maksimum inntekt`() {
        undersøke(UNG_PERSON_FNR_2018, 5.NAV(3500.0), 2.HELG, 5.NAV)

        assertEquals(12, inspektør.size)
        assertEquals(10, inspektør.navDagTeller)
        assertEquals(2, inspektør.navHelgDagTeller)
        assertEquals(10805 + 6000, inspektør.totalUtbetaling())
        assertEquals(12.desember, maksdato)
        assertTrue(aktivitetslogg.hasWarnings())
        assertFalse(aktivitetslogg.hasErrors())
    }

    @Test
    internal fun `avgrenset betaling pga minimun sykdomsgrad`() {
        undersøke(UNG_PERSON_FNR_2018, 5.NAV(1200.0, 19.0), 2.HELG, 5.NAV)

        assertEquals(12, inspektør.size)
        assertEquals(10, inspektør.avvistDagTeller)
        assertEquals(2, inspektør.navHelgDagTeller)
        assertEquals(0, inspektør.totalUtbetaling())
        assertTrue(aktivitetslogg.hasWarnings())
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
        assertTrue(aktivitetslogg.hasWarnings())
        assertFalse(aktivitetslogg.hasErrors())
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
        assertTrue(aktivitetslogg.hasWarnings())
        assertFalse(aktivitetslogg.hasErrors())
    }

    @Test
    internal fun `historiske utbetalingstidslinjer vurdert i 248 grense`() {
        undersøke(
            PERSON_67_ÅR_FNR_2018,
            tidslinjeOf(35.UTELATE, 50.NAV),
            tidslinjeOf(7.UTELATE, 20.NAV)
            )

        assertEquals(50, inspektør.size)
        assertEquals(40, inspektør.navDagTeller)
        assertEquals(10, inspektør.avvistDagTeller)
        assertEquals(40 * 1200, inspektør.totalUtbetaling())
        assertEquals(16.mars, maksdato)
        assertTrue(aktivitetslogg.hasWarnings())
        assertFalse(aktivitetslogg.hasErrors())
    }

    @Test
    fun `beregn maksdato i et sykdomsforløp som slutter på en fredag`() {
        undersøke(UNG_PERSON_FNR_2018, 16.AP, 3.NAV, 1.HELG)
        assertEquals(28.desember, maksdato) // 3 dager already paid, 245 left. So should be fredag!
        assertTrue(aktivitetslogg.hasMessages())
        assertFalse(aktivitetslogg.hasWarnings())
    }

    @Test
    fun `beregn maksdato i et sykdomsforløp med opphold i sykdom`() {
        undersøke(UNG_PERSON_FNR_2018, 2.ARB, 16.AP, 7.ARB, 1.NAV, 2.HELG, 5.NAV)
        assertEquals(8.januar(2019), maksdato)
    }

    @Test
    fun `beregn maksdato (med rest) der den ville falt på en lørdag`() { //(351.S + 1.F + 1.S)
        undersøke(
            UNG_PERSON_FNR_2018,
            16.AP,
            3.NAV,
            2.HELG,
            (47 * 5).NAV,
            (47 * 2).HELG,
            1.NAV,
            1.FRI,
            1.NAV
        )
        assertEquals(31.desember, maksdato)
    }

    @Test
    fun `beregn maksdato (med rest) der den ville falt på en søndag`() { //(23.S + 2.F + 1.S)
        undersøke(
            UNG_PERSON_FNR_2018,
            16.AP,
            3.NAV,
            2.HELG,
            2.NAV,
            2.FRI,
            1.NAV
        )
        assertEquals(1.januar(2019), maksdato)
    }

    @Test
    fun `maksdato forskyves av ferie etterfulgt av sykedager`() { //21.S + 3.F + 1.S)
        undersøke(
            UNG_PERSON_FNR_2018,
            16.AP,
            3.NAV,
            2.HELG,
            3.FRI,
            1.NAV
        )
        assertEquals(2.januar(2019), maksdato)
    }

    @Test
    fun `maksdato forskyves ikke av ferie på tampen av sykdomstidslinjen`() { //(21.S + 3.F)
        undersøke(
            UNG_PERSON_FNR_2018,
            16.AP,
            3.NAV,
            2.HELG,
            3.FRI
        )
        assertEquals(28.desember, maksdato)
    }

    @Test
    fun `maksdato forskyves ikke av ferie etterfulgt av arbeidsdag på tampen av sykdomstidslinjen`() { //(21.S + 3.F + 1.A)
        undersøke(
            UNG_PERSON_FNR_2018,
            16.AP,
            3.NAV,
            2.HELG,
            3.FRI,
            1.ARB
        )
        assertEquals(28.desember, maksdato)
    }

    @Test
    fun `maksdato er udefinert hvis det ikke er utbetalingsdager`() { //(16.S)
        undersøke(
            UNG_PERSON_FNR_2018,
            16.AP
        )
        assertNull(maksdato)
    }


    @Test
    fun `når sykepengeperioden går over maksdato, så skal utbetaling stoppe ved maksdato`() { //(249.NAV)
        undersøke(
            UNG_PERSON_FNR_2018,
            16.AP,
            3.NAV,
            (49 * 2).HELG,
            (49 * 5).NAV,
            1.NAV
        )
        assertEquals(28.desember, maksdato)
        assertTrue(aktivitetslogg.hasWarnings())
        assertFalse(aktivitetslogg.hasErrors())
    }

    @Test
    fun `når personen fyller 67 blir antall gjenværende dager 60`() {
        undersøke(
            PERSON_67_ÅR_FNR_2018,
            16.AP,
            (12 * 2).HELG,
            (12 * 5).NAV,
            10.NAV
        )
        assertEquals(10.april, maksdato)
        assertEquals(60, inspektør.navDagTeller)
        assertTrue(aktivitetslogg.hasWarnings())
        assertFalse(aktivitetslogg.hasErrors())
    }

    @Test
    fun `når personen fyller 67 og 248 dager er brukt opp`() {
        undersøke(
            "01125112345",
            16.AP,
            3.NAV,
            (49 * 2).HELG,
            (49 * 5).NAV,
            20.NAV
        )
        assertEquals(28.desember, maksdato)
        assertTrue(aktivitetslogg.hasWarnings())
        assertFalse(aktivitetslogg.hasErrors())
    }

    @Test
    fun `når personen fyller 70 skal det ikke utbetales sykepenger`() {
        undersøke(
            "01024812345",
            16.AP,
            3.NAV,
            2.HELG,
            400.NAV
        )
        assertEquals(31.januar, maksdato)
        assertTrue(aktivitetslogg.hasWarnings())
        assertFalse(aktivitetslogg.hasErrors())
    }

    private fun undersøke(fnr: String, vararg utbetalingsdager: Utbetalingsdager) {
        val tidslinje = tidslinjeOf(*utbetalingsdager)
        undersøke(fnr, tidslinje, tidslinjeOf())
    }

    private fun undersøke(fnr: String,
                          arbeidsgiverTidslinje: Utbetalingstidslinje,
                          historiskTidslinje: Utbetalingstidslinje) {
        val arbeidsgiver = Arbeidsgiver(Person("aktørid", fnr), "88888888")
        aktivitetslogg = Aktivitetslogg()
        ArbeidsgiverUtbetalinger(
            mapOf(arbeidsgiver to arbeidsgiverTidslinje),
            historiskTidslinje,
            Periode(1.januar, 31.desember(2019)),
            Alder(fnr),
            NormalArbeidstaker,
            aktivitetslogg
        ).also {
            it.beregn()
            maksdato = it.maksdato()
        }
        inspektør = UtbetalingstidslinjeInspektør(arbeidsgiver.peekTidslinje())
            .result()
    }

}
