package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Økonomi
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class MaksimumSykepengedagerfilterTest {

    companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val PERSON_70_ÅR_FNR_2018 = "10014812345"
        private const val PERSON_67_ÅR_FNR_2018 = "10015112345"
    }

    private lateinit var aktivitetslogg: Aktivitetslogg

    @BeforeEach internal fun setup() {
        aktivitetslogg = Aktivitetslogg()
    }

    @Test internal fun `riktig antall dager`() {
        val tidslinje = tidslinjeOf(10.AP, 10.NAV)
        assertEquals(emptyList<LocalDate>(), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test internal fun `stopper betaling etter 248 dager`() {
        val tidslinje = tidslinjeOf(249.NAV)
        assertEquals(listOf(6.september), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
        assertTrue(aktivitetslogg.hasWarnings())
    }

    @Test fun `stopper betaling etter 248 dager `() {
        val tidslinje = tidslinjeOf(249.NAV)
        assertEquals(listOf(6.september), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018, Periode(1.januar, 1.mars)))
        assertTrue(aktivitetslogg.hasMessages())
        assertFalse(aktivitetslogg.hasWarnings())
    }

    @Test internal fun `26 uker arbeid resetter utbetalingsgrense`() {
        val tidslinje = tidslinjeOf(248.NAV, (26 * 7).ARB, 10.NAV)
        assertEquals(emptyList<LocalDate>(), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test internal fun `en ubetalt sykedag før opphold`() {
        val tidslinje = tidslinjeOf(249.NAV, (26 * 7).ARB, 10.NAV)
        assertEquals(listOf(6.september), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test internal fun `utbetaling stopper når du blir 70 år`() {
        val tidslinje = tidslinjeOf(11.NAV)
        assertEquals(listOf(10.januar, 11.januar), tidslinje.utbetalingsavgrenser(PERSON_70_ÅR_FNR_2018))
    }

    @Test internal fun `noe som helst sykdom i opphold resetter teller`() {
        val tidslinje =
            tidslinjeOf(248.NAV, (24 * 7).ARB, 7.NAV, (2 * 7).ARB, 10.NAV)
        assertEquals(7, tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018).size)
    }

    @Test internal fun `fridag etter sykdag er en del av opphold`() {
        val tidslinje = tidslinjeOf(248.NAV, (25 * 7).FRI, 7.ARB, 7.NAV)
        assertEquals(emptyList<LocalDate>(), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test internal fun `opphold på mindre enn 26 uker skal ikke nullstille telleren`() {
        val tidslinje = tidslinjeOf(248.NAV, (26 * 7 - 1).FRI, 1.NAV)
        assertEquals(listOf(6.mars(2019)), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test internal fun `sjekk 60 dagers grense for 67 åringer`() {
        val tidslinje = tidslinjeOf(10.NAV, 61.NAV)
        assertEquals(listOf(12.mars), tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_FNR_2018))
    }

    @Test internal fun `sjekk 60 dagers grense for 67 åringer med 26 ukers opphold`() {
        val tidslinje = tidslinjeOf(10.NAV, 60.NAV, (26 * 7).ARB, 60.NAV)
        assertEquals(emptyList<LocalDate>(), tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_FNR_2018))
    }

    @Test internal fun `sjekk at 26 uker med syk etter karantene starter utbetaling`() {
        val tidslinje = tidslinjeOf(248.NAV, (26 * 7).NAV, 60.NAV)
        assertEquals(26*7, tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018).size)
    }

    @Test internal fun `sjekk at 26 uker med syk etter karantene starter utbetaling gammel person`() {
        val tidslinje = tidslinjeOf(60.NAV, (26 * 7).NAV, 60.NAV)
        assertEquals(26*7, tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_FNR_2018).size)
    }

    @Test internal fun `helgedager teller som opphold`() {
        val tidslinje = tidslinjeOf(248.NAV, (26 * 7).HELG, 60.NAV)
        assertEquals(emptyList<LocalDate>(), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test internal fun `helgedager sammen med utbetalingsdager teller som opphold`() {
        val tidslinje =
            tidslinjeOf(248.NAV, (20 * 7).HELG, 7.NAV, (5 * 7).HELG, 60.NAV)
        assertEquals(7, tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018).size)
    }

    @Test internal fun `sjekk at sykdom i arbgiver periode ikke ødelegger oppholdsperioden`() {
        val tidslinje = tidslinjeOf(50.NAV, (25 * 7).ARB, 7.AP, 248.NAV)
        assertEquals(emptyList<LocalDate>(), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test internal fun `helgedager innimellom utbetalingsdager betales ikke`() {
        val tidslinje = tidslinjeOf(200.NAV, 40.HELG, 48.NAV, 1.NAV)
        assertEquals(listOf(16.oktober), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test internal fun `26 uker tilbakestilles ikke for 70 år gammel`() {
        val tidslinje = tidslinjeOf(9.NAV, (26 * 7).ARB, 2.NAV)
        assertEquals(listOf(11.juli, 12.juli), tidslinje.utbetalingsavgrenser(PERSON_70_ÅR_FNR_2018))
    }

    @Test internal fun `ingen utbetaling når 70 år gammel`() {
        val tidslinje = tidslinjeOf(15.UTELATE, 1.NAV, (26 * 7).ARB, 1.NAV)
        assertEquals(listOf(16.januar, 18.juli), tidslinje.utbetalingsavgrenser(PERSON_70_ÅR_FNR_2018))
    }

    @Test internal fun `ukjente dager generert når du legger til to utbetalingstidslinjer teller som ikke-utbetalingsdager`() {
        val tidslinje1 = tidslinjeOf(50.NAV)
        val tidslinje2 = tidslinjeOf(50.UTELATE, (26 * 7).UTELATE, 248.NAV)
        assertEquals(emptyList<LocalDate>(), (tidslinje1 + tidslinje2).utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test internal fun `248 dager nådd på 3 år`() {
        val tidslinje = tilbakevendendeSykdom(3.NAV, 10.ARB, 1.NAV)
        assertEquals(listOf(3.januar(2022), 14.januar(2022)), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test internal fun `skyvevindu på 3 år legger til dager`() {
        val tidslinje = tilbakevendendeSykdom(1.NAV, 3.ARB, 5.NAV, 1.NAV)
        assertEquals(listOf(10.januar(2022)), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test internal fun `26 ukers friske tilbakestiller skyvevindu på 3 år`() {
        val tidslinje = enAnnenSykdom(1.NAV, 3.ARB, 5.NAV, (248-60).NAV, 1.NAV)
        assertEquals(listOf(17.juli(2022)), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test internal fun `teller sykedager med opphold i sykdom`() {
        val gjeldendePerioder = listOf(tidslinjeOf(10.NAV, startDato = 1.mars))
        val historikk = tidslinjeOf(31.NAV, startDato = 1.januar(2018))
        val filter = maksimumSykepengedagerfilter()
            .also { it.filter(gjeldendePerioder, historikk) }
        assertEquals(41, filter.forbrukteSykedager())
    }

    @Test internal fun `teller sykedager med overlapp`() {
        val gjeldendePerioder = listOf(tidslinjeOf(10.NAV, startDato = 1.februar))
        val historikk = tidslinjeOf(16.ARB, 31.NAV, startDato = 1.januar(2018))
        val filter = maksimumSykepengedagerfilter()
            .also { it.filter(gjeldendePerioder, historikk) }
        assertEquals(31, filter.forbrukteSykedager())
    }

    @Test internal fun `teller sykedager med konflikt`() {
        val gjeldendePerioder = listOf(tidslinjeOf(10.NAV, startDato = 1.januar))
        val historikk = tidslinjeOf(16.ARB, 31.NAV, startDato = 1.januar)
        val filter = maksimumSykepengedagerfilter()
            .also { it.filter(gjeldendePerioder, historikk) }
        assertEquals(41, filter.forbrukteSykedager())
    }

    @Test internal fun `teller sykedager med 26 uker`() {
        val filter = maksimumSykepengedagerfilter()
            .also { it.filter(listOf(enAnnenSykdom()), tidslinjeOf()) }
        assertEquals(54, filter.forbrukteSykedager())
    }

    private fun maksimumSykepengedagerfilter() = MaksimumSykepengedagerfilter(
        Alder(UNG_PERSON_FNR_2018),
        NormalArbeidstaker,
        Periode(1.januar, 10.januar),
        Aktivitetslogg()
    )

    // No 26 week gap with base of 246 NAV days
    private fun tilbakevendendeSykdom(vararg utbetalingsdager: Utbetalingsdager): Utbetalingstidslinje {
        return tidslinjeOf(
            365.ARB,
            48.NAV,
            152.ARB,
            48.NAV,
            152.ARB,
            48.NAV,
            152.ARB,
            48.NAV,
            152.ARB,
            48.NAV,
            152.ARB,
            6.NAV,
            90.ARB,
            *utbetalingsdager
        )
    }

    // 26 week gap inside 3 year window of 246 days with 54 NAV days after the gap
    private fun enAnnenSykdom(vararg utbetalingsdager: Utbetalingsdager): Utbetalingstidslinje {
        return tidslinjeOf(
            365.ARB,
            48.NAV,
            152.ARB,
            48.NAV,
            152.ARB,
            48.NAV,
            152.ARB,
            18.NAV,
            182.ARB,
            48.NAV,
            152.ARB,
            6.NAV,
            90.ARB,
            *utbetalingsdager
        )
    }

    private fun Utbetalingstidslinje.utbetalingsavgrenser(fnr: String, periode: Periode = Periode(1.januar, 31.desember)): List<LocalDate> {
        MaksimumSykepengedagerfilter(
            Alder(fnr),
            NormalArbeidstaker,
            periode,
            aktivitetslogg
        ).also {
            it.filter(listOf(this), tidslinjeOf())
            it.beregnGrenser(periode.endInclusive)
        }
        return AvvisteDager(this).datoer
    }

    private class AvvisteDager(tidslinje: Utbetalingstidslinje): UtbetalingsdagVisitor {
        internal val datoer = mutableListOf<LocalDate>()

        init {
            tidslinje.accept(this)
        }

        override fun visit(
            dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag,
            dato: LocalDate,
            økonomi: Økonomi,
            grad: Prosentdel,
            aktuellDagsinntekt: Double,
            dekningsgrunnlag: Double,
            arbeidsgiverbeløp: Int,
            personbeløp: Int
        ) {
            datoer.add(dag.dato)
        }

    }
}
