package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.somFødselsnummer
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class MaksimumSykepengedagerfilterTest {

    companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val PERSON_70_ÅR_FNR_2018 = "10014812345"
        private const val PERSON_67_ÅR_FNR_2018 = "11015112345"
    }

    private lateinit var aktivitetslogg: Aktivitetslogg

    @BeforeEach internal fun setup() {
        aktivitetslogg = Aktivitetslogg()
    }

    @Test fun `riktig antall dager`() {
        val tidslinje = tidslinjeOf(10.AP, 10.NAV)
        assertEquals(emptyList<LocalDate>(), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test fun `stopper betaling etter 248 dager`() {
        val tidslinje = tidslinjeOf(249.NAV)
        assertEquals(listOf(6.september), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
        assertTrue(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test fun `stopper betaling etter 248 dager inkl persontidslinje`() {
        val persontidslinje = tidslinjeOf(247.NAV)
        val tidslinje = tidslinjeOf(2.NAV, startDato = persontidslinje.periode().endInclusive.plusDays(1))
        assertEquals(listOf(6.september), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018, personTidslinje = persontidslinje))
        assertTrue(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test fun `stopper betaling etter 248 dager `() {
        val tidslinje = tidslinjeOf(249.NAV)
        assertEquals(listOf(6.september), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018, Periode(1.januar, 1.mars)))
        assertTrue(aktivitetslogg.hasActivities())
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test fun `26 uker arbeid resetter utbetalingsgrense`() {
        val tidslinje = tidslinjeOf(248.NAV, (26 * 7).ARB, 10.NAV)
        assertEquals(emptyList<LocalDate>(), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test fun `utbetalingsgrense resettes ikke når oppholdsdager nås ved sykedager`() {
        val tidslinje = tidslinjeOf(248.NAV, (25 * 7).ARB, 10.NAV)
        val periode = 28.februar(2019) til 9.mars(2019)
        assertEquals(periode.map { it }, tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test fun `utbetalingsgrense resettes ved første arbeidsdag`() {
        val tidslinje = tidslinjeOf(248.NAV, (25 * 7).ARB, 10.NAV, 1.ARB, 10.NAV)
        val periode = 28.februar(2019) til 9.mars(2019)
        assertEquals(periode.map { it }, tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test fun `utbetalingsgrense resettes ved første arbeidsgiverperiodedag`() {
        val tidslinje = tidslinjeOf(248.NAV, (25 * 7).ARB, 10.NAV, 1.AP, 10.NAV)
        val periode = 28.februar(2019) til 9.mars(2019)
        assertEquals(periode.map { it }, tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test fun `en ubetalt sykedag før opphold`() {
        val tidslinje = tidslinjeOf(249.NAV, (26 * 7).ARB, 10.NAV)
        assertEquals(listOf(6.september), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test fun `utbetaling stopper når du blir 70 år`() {
        val tidslinje = tidslinjeOf(11.NAV)
        assertEquals(listOf(10.januar, 11.januar), tidslinje.utbetalingsavgrenser(PERSON_70_ÅR_FNR_2018))
    }

    @Test fun `noe som helst sykdom i opphold resetter teller`() {
        val tidslinje =
            tidslinjeOf(248.NAV, (24 * 7).ARB, 7.NAV, (2 * 7).ARB, 10.NAV)
        assertEquals(7, tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018).size)
    }

    @Test fun `fridag etter sykdag er en del av opphold`() {
        val tidslinje = tidslinjeOf(248.NAV, (25 * 7).FRI, 7.ARB, 7.NAV)
        assertEquals(emptyList<LocalDate>(), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test fun `opphold på mindre enn 26 uker skal ikke nullstille telleren`() {
        val tidslinje = tidslinjeOf(248.NAV, (26 * 7 - 1).FRI, 1.NAV)
        assertEquals(listOf(6.mars(2019)), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test fun `sjekk 60 dagers grense for 67 åringer`() {
        val tidslinje = tidslinjeOf(11.NAV, 61.NAV)
        assertEquals(listOf(13.mars), tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_FNR_2018))
    }

    @Test fun `sjekk 60 dagers grense for 67 åringer med 26 ukers opphold`() {
        val tidslinje = tidslinjeOf(10.NAV, 60.NAV, (26 * 7).ARB, 60.NAV)
        assertEquals(emptyList<LocalDate>(), tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_FNR_2018))
    }

    @Test fun `sjekk at 26 uker med syk etter karantene ikke starter utbetaling`() {
        val tidslinje = tidslinjeOf(248.NAV, (26 * 7).NAV, 60.NAV)
        assertEquals(26*7 + 60, tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018).size)
    }

    @Test fun `sjekk at 26 uker med syk etter karantene starter utbetaling etter første arbeidsdag`() {
        val tidslinje = tidslinjeOf(248.NAV, (26 * 7).NAV, 1.ARB, 60.NAV)
        assertEquals(26*7, tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018).size)
    }

    @Test fun `sjekk at sykepenger er oppbrukt når personen fyller 67 år`() {
        val tidslinje = tidslinjeOf(248.NAV, 10.NAV, startDato = 11.januar.minusDays(248))
        assertEquals(10, tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_FNR_2018).size)
    }

    @Test fun `sjekk at sykepenger er oppbrukt etter at personen har fylt 67 år`() {
        val tidslinje = tidslinjeOf(11.NAV, 60.NAV, 1.NAV)
        assertEquals(1, tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_FNR_2018).size)
    }

    @Test fun `hvis gjenstående sykepengedager er under 60 ved fylte 67 begrunnes avslag med 8-12`() {
        val tidslinje = tidslinjeOf(248.NAV, 2.NAV, startDato = 11.januar.minusDays(222))
        val avvisteDager = tidslinje.utbetalingsavgrenserMedKommentarer(PERSON_67_ÅR_FNR_2018)
        assertEquals(2, avvisteDager.size)
        assertTrue(avvisteDager.all { it.begrunnelser.single() == Begrunnelse.SykepengedagerOppbrukt })
    }

    @Test fun `hvis gjenstående sykepengedager er over 60 ved fylte 67 begrunnes avslag med 8-51`() {
        val tidslinje = tidslinjeOf(11.NAV, 60.NAV, 2.NAV, startDato = 11.januar.minusDays(10))
        val avvisteDager = tidslinje.utbetalingsavgrenserMedKommentarer(PERSON_67_ÅR_FNR_2018)
        assertEquals(2, avvisteDager.size)
        assertTrue(avvisteDager.all { it.begrunnelser.single() == Begrunnelse.SykepengedagerOppbruktOver67 })
    }

    @Test fun `begrunnelsen følger tidspunkt for når sykepengedagene brukes opp, også om det er gap mellom`() {
        val tidslinje = tidslinjeOf(248.NAV, 25.ARB, 2.NAV, startDato = 11.januar.minusDays(258))
        val avvisteDager = tidslinje.utbetalingsavgrenserMedKommentarer(PERSON_67_ÅR_FNR_2018)
        assertEquals(2, avvisteDager.size)
        assertTrue(avvisteDager.all { it.begrunnelser.single() == Begrunnelse.SykepengedagerOppbrukt })
    }

    @Test fun `begrunnelse ved flere oppbrukte rettigheter - hel tidslinje`() {
        val tidslinje = tidslinjeOf(248.NAV, 1.NAV, (26 * 7).ARB, 60.NAV, 1.NAV, startDato = 11.januar.minusDays(248))
        val avvisteDager = tidslinje.utbetalingsavgrenserMedKommentarer(PERSON_67_ÅR_FNR_2018)
        assertEquals(2, avvisteDager.size)
        assertEquals(Begrunnelse.SykepengedagerOppbrukt, avvisteDager.first().begrunnelser.single())
        assertEquals(Begrunnelse.SykepengedagerOppbruktOver67, avvisteDager.last().begrunnelser.single())
    }

    @Test fun `sjekk at 26 uker med syk etter karantene ikke starter utbetaling gammel person`() {
        val tidslinje = tidslinjeOf(11.NAV, 60.NAV, (26 * 7).NAV, 60.NAV)
        assertEquals(26*7 + 60, tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_FNR_2018).size)
    }

    @Test fun `sjekk at 26 uker med syk etter karantene starter utbetaling for gammel person etter første arbeidsdag`() {
        val tidslinje = tidslinjeOf(11.NAV, 60.NAV, (26 * 7).NAV, 1.ARB, 60.NAV)
        assertEquals(26*7, tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_FNR_2018).size)
    }

    @Test fun `helgedager teller som opphold`() {
        val tidslinje = tidslinjeOf(248.NAV, (26 * 7).HELG, 60.NAV)
        assertEquals(emptyList<LocalDate>(), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test fun `helgedager sammen med utbetalingsdager teller som opphold`() {
        val tidslinje =
            tidslinjeOf(248.NAV, (20 * 7).HELG, 7.NAV, (5 * 7).HELG, 60.NAV)
        assertEquals(7, tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018).size)
    }

    @Test fun `sjekk at sykdom i arbgiver periode ikke ødelegger oppholdsperioden`() {
        val tidslinje = tidslinjeOf(50.NAV, (25 * 7).ARB, 7.AP, 248.NAV)
        assertEquals(emptyList<LocalDate>(), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018, 1.april(2019) til 30.april(2019)))
    }

    @Test fun `helgedager innimellom utbetalingsdager betales ikke`() {
        val tidslinje = tidslinjeOf(200.NAV, 40.HELG, 48.NAV, 1.NAV)
        assertEquals(listOf(16.oktober), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test fun `ukjente dager generert når du legger til to utbetalingstidslinjer teller som ikke-utbetalingsdager`() {
        val tidslinje1 = tidslinjeOf(50.NAV)
        val tidslinje2 = tidslinjeOf(50.UTELATE, (26 * 7).UTELATE, 248.NAV)
        assertEquals(emptyList<LocalDate>(), (tidslinje1 + tidslinje2).utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test fun `248 dager nådd på 3 år`() {
        val tidslinje = tilbakevendendeSykdom(3.NAV, 10.ARB, 1.NAV)
        assertEquals(listOf(3.januar(2022), 14.januar(2022)), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test fun `sykepengedager eldre enn tre år teller ikke lenger som forbrukte dager`() {
        val tidslinje = tilbakevendendeSykdom(1.NAV, 3.ARB, 5.NAV, 1.NAV)
        assertEquals(listOf(10.januar(2022)), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test fun `26 ukers friske tilbakestiller skyvevindu på 3 år`() {
        val tidslinje = enAnnenSykdom(1.NAV, 3.ARB, 5.NAV, (248-60).NAV, 1.NAV)
        assertEquals(listOf(17.juli(2022)), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test fun `teller sykedager med opphold i sykdom`() {
        val gjeldendePerioder = listOf(tidslinjeOf(12.NAVv2, startDato = 1.mars))
        val historikk = tidslinjeOf(45.NAVv2, startDato = 1.januar(2018))
        val sykepengerettighet = maksimumSykepengedagerfilter().filter(gjeldendePerioder, historikk)
        assertEquals(41, sykepengerettighet.forbrukteSykedager)
    }

    @Test fun `teller sykedager med overlapp`() {
        val gjeldendePerioder = listOf(tidslinjeOf(12.NAVv2, startDato = 1.februar))
        val historikk = tidslinjeOf(12.ARBv2, 45.NAVv2, startDato = 1.januar(2018))
        val sykepengerettighet = maksimumSykepengedagerfilter().filter(gjeldendePerioder, historikk)
        assertEquals(31, sykepengerettighet.forbrukteSykedager)
    }

    @Test fun `teller sykedager med konflikt`() {
        val gjeldendePerioder = listOf(tidslinjeOf(12.NAVv2, startDato = 1.januar))
        val historikk = tidslinjeOf(12.ARBv2, 45.NAVv2, startDato = 1.januar)
        val sykepengerettighet = maksimumSykepengedagerfilter().filter(gjeldendePerioder, historikk)
        assertEquals(41, sykepengerettighet.forbrukteSykedager)
    }

    @Test fun `teller sykedager med 26 uker`() {
        val sykepengerettighet = maksimumSykepengedagerfilter().filter(listOf(enAnnenSykdom()), Utbetalingstidslinje())
        assertEquals(54, sykepengerettighet.forbrukteSykedager)
    }

    private fun maksimumSykepengedagerfilter() = MaksimumSykepengedagerfilter(
        UNG_PERSON_FNR_2018.somFødselsnummer().alder(),
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

    private fun Utbetalingstidslinje.utbetalingsavgrenser(fnr: String, periode: Periode = Periode(1.januar, 31.desember), personTidslinje: Utbetalingstidslinje = Utbetalingstidslinje()): List<LocalDate> {
        MaksimumSykepengedagerfilter(
            fnr.somFødselsnummer().alder(),
            NormalArbeidstaker,
            periode,
            aktivitetslogg
        ).filter(listOf(this), personTidslinje)
        return inspektør.avvistedatoer
    }

    private fun Utbetalingstidslinje.utbetalingsavgrenserMedKommentarer(
        fnr: String,
        periode: Periode = Periode(1.januar, 31.desember),
        personTidslinje: Utbetalingstidslinje = Utbetalingstidslinje()
    ): MutableList<Utbetalingstidslinje.Utbetalingsdag.AvvistDag> {
        MaksimumSykepengedagerfilter(
            fnr.somFødselsnummer().alder(),
            NormalArbeidstaker,
            periode,
            aktivitetslogg
        ).filter(listOf(this), personTidslinje)
        return inspektør.avvistedager
    }
}
