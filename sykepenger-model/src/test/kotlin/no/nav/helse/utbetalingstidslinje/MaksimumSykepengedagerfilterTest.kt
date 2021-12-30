package no.nav.helse.utbetalingstidslinje

import no.nav.helse.ForventetFeil
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
    private companion object {
        private const val UNG_PERSON_FNR_2018 = "12029240045"
        private const val PERSON_70_ÅR_10_JANUAR_2018 = "10014812345"
        private const val PERSON_67_ÅR_11_JANUAR_2018 = "11015112345"
    }

    private lateinit var rettighet: Sykepengerettighet
    private lateinit var aktivitetslogg: Aktivitetslogg

    @BeforeEach
    internal fun setup() {
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `riktig antall dager`() {
        val tidslinje = tidslinjeOf(16.AP, 3.NAVDAGER, 2.HELG, 5.NAVDAGER)
        assertEquals(emptyList<LocalDate>(), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
        assertEquals(28.desember, rettighet.maksdato)
    }

    @Test
    fun `stopper betaling etter 248 dager`() {
        val tidslinje = tidslinjeOf(249.NAVDAGER)
        assertEquals(listOf(6.september), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
        assertEquals(5.september, rettighet.maksdato)
        assertTrue(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `stopper betaling etter 248 dager inkl persontidslinje`() {
        val persontidslinje = tidslinjeOf(247.NAVDAGER)
        val tidslinje = tidslinjeOf(2.NAVDAGER, startDato = persontidslinje.periode().endInclusive.plusDays(1))
        assertEquals(listOf(6.september), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018, personTidslinje = persontidslinje))
        assertTrue(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `stopper betaling etter 248 dager `() {
        val tidslinje = tidslinjeOf(249.NAVDAGER)
        assertEquals(listOf(6.september), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018, Periode(1.januar, 1.mars)))
        assertTrue(aktivitetslogg.hasActivities())
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `26 uker arbeid resetter utbetalingsgrense`() {
        val tidslinje = tidslinjeOf(248.NAVDAGER, (26 * 7).ARB, 10.NAVDAGER)
        assertEquals(emptyList<LocalDate>(), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test
    fun `utbetalingsgrense resettes ikke når oppholdsdager nås ved sykedager`() {
        val tidslinje = tidslinjeOf(248.NAVDAGER, (25 * 7).ARB, 10.NAVDAGER)
        val periode = 28.februar(2019) til 9.mars(2019)
        assertEquals(periode.map { it }, tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test
    fun `utbetalingsgrense resettes ved første arbeidsdag`() {
        val tidslinje = tidslinjeOf(248.NAVDAGER, (25 * 7).ARB, 10.NAVDAGER, 1.ARB, 10.NAVDAGER)
        val periode = 28.februar(2019) til 9.mars(2019)
        assertEquals(periode.map { it }, tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test
    fun `utbetalingsgrense resettes ved første arbeidsgiverperiodedag`() {
        val tidslinje = tidslinjeOf(248.NAVDAGER, (25 * 7).ARB, 10.NAVDAGER, 1.AP, 10.NAVDAGER)
        val periode = 28.februar(2019) til 9.mars(2019)
        assertEquals(periode.map { it }, tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test
    fun `en ubetalt sykedag før opphold`() {
        val tidslinje = tidslinjeOf(249.NAVDAGER, (26 * 7).ARB, 10.NAVDAGER)
        assertEquals(listOf(6.september), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test
    fun `utbetaling stopper når du blir 70 år`() {
        val tidslinje = tidslinjeOf(11.NAVDAGER)
        assertEquals(listOf(10.januar, 11.januar), tidslinje.utbetalingsavgrenser(PERSON_70_ÅR_10_JANUAR_2018))
    }

    @Test
    fun `kan ikke bli syk på 70årsdagen`() {
        val tidslinje = tidslinjeOf(9.UTELATE, 2.NAVDAGER)
        assertEquals(listOf(10.januar, 11.januar), tidslinje.utbetalingsavgrenser(PERSON_70_ÅR_10_JANUAR_2018))
    }

    @Test
    fun `kan ikke bli syk etter 70årsdagen`() {
        val tidslinje = tidslinjeOf(10.UTELATE, 2.NAVDAGER)
        assertEquals(listOf(11.januar, 12.januar), tidslinje.utbetalingsavgrenser(PERSON_70_ÅR_10_JANUAR_2018))
    }

    @Test
    fun `noe som helst sykdom i opphold resetter teller`() {
        val tidslinje =
            tidslinjeOf(248.NAVDAGER, (24 * 7).ARB, 7.NAVDAGER, (2 * 7).ARB, 10.NAVDAGER)
        assertEquals(7, tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018).size)
    }

    @Test
    fun `fridag etter sykdag er en del av opphold`() {
        val tidslinje = tidslinjeOf(248.NAVDAGER, (25 * 7).FRI, 7.ARB, 7.NAVDAGER)
        assertEquals(emptyList<LocalDate>(), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test
    fun `opphold på mindre enn 26 uker skal ikke nullstille telleren`() {
        val tidslinje = tidslinjeOf(248.NAVDAGER, (26 * 7 - 1).FRI, 1.NAVDAGER)
        assertEquals(listOf(6.mars(2019)), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test
    fun `sjekk 60 dagers grense for 67 åringer`() {
        val tidslinje = tidslinjeOf(11.NAVDAGER, 61.NAVDAGER)
        assertEquals(listOf(13.mars), tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_11_JANUAR_2018))
    }

    @Test
    fun `sjekk 60 dagers grense for 67 åringer med 26 ukers opphold`() {
        val tidslinje = tidslinjeOf(10.NAVDAGER, 60.NAVDAGER, (26 * 7).ARB, 60.NAVDAGER)
        assertEquals(emptyList<LocalDate>(), tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_11_JANUAR_2018))
    }

    @Test
    fun `sjekk at 26 uker med syk etter karantene ikke starter utbetaling`() {
        val tidslinje = tidslinjeOf(248.NAVDAGER, (26 * 7).NAVDAGER, 60.NAVDAGER)
        assertEquals(26*7 + 60, tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018).size)
    }

    @Test
    fun `sjekk at 26 uker med syk etter karantene starter utbetaling etter første arbeidsdag`() {
        val tidslinje = tidslinjeOf(248.NAVDAGER, (26 * 7).NAVDAGER, 1.ARB, 60.NAVDAGER)
        assertEquals(26*7, tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018).size)
    }

    @Test
    fun `sjekk at sykepenger er oppbrukt når personen fyller 67 år`() {
        val tidslinje = tidslinjeOf(248.NAVDAGER, 10.NAVDAGER, startDato = 11.januar.minusDays(248))
        assertEquals(10, tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_11_JANUAR_2018).size)
    }

    @Test
    fun `sjekk at sykepenger er oppbrukt etter at personen har fylt 67 år`() {
        val tidslinje = tidslinjeOf(11.NAVDAGER, 60.NAVDAGER, 1.NAVDAGER)
        assertEquals(1, tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_11_JANUAR_2018).size)
    }

    @Test
    fun `hvis gjenstående sykepengedager er under 60 ved fylte 67 begrunnes avslag med 8-12`() {
        val tidslinje = tidslinjeOf(248.NAVDAGER, 2.NAVDAGER, startDato = 11.januar.minusDays(222))
        assertEquals(2, tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_11_JANUAR_2018).size)
        assertTrue(tidslinje.inspektør.avvistedager.all { it.begrunnelser.single() == Begrunnelse.SykepengedagerOppbrukt })
    }

    @ForventetFeil("skal ikke kunne bli syk på 70årsdagen")
    @Test
    fun `bruke opp alt fom 67 år`() {
        val tidslinje = tidslinjeOf(188.NAVDAGER, 61.NAVDAGER, (26 * 7).ARB, 61.NAVDAGER, 61.ARB, 365.ARB, 365.ARB, 1.NAVDAGER, startDato = 8.juli(2017))
        assertEquals(listOf(13.mars, 11.november), tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_11_JANUAR_2018))
        assertEquals(listOf(Begrunnelse.SykepengedagerOppbrukt), tidslinje.inspektør.begrunnelse(13.mars))
        assertEquals(listOf(Begrunnelse.SykepengedagerOppbruktOver67), tidslinje.inspektør.begrunnelse(11.november))
        assertEquals(listOf(Begrunnelse.Over70), tidslinje.inspektør.begrunnelse(11.januar(2022)))
    }

    @Test
    fun `hvis gjenstående sykepengedager er over 60 ved fylte 67 begrunnes avslag med 8-51`() {
        val tidslinje = tidslinjeOf(11.NAVDAGER, 60.NAVDAGER, 2.NAVDAGER, startDato = 11.januar.minusDays(10))
        assertEquals(2, tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_11_JANUAR_2018).size)
        assertTrue(tidslinje.inspektør.avvistedager.all { it.begrunnelser.single() == Begrunnelse.SykepengedagerOppbruktOver67 })
    }

    @Test
    fun `begrunnelsen følger tidspunkt for når sykepengedagene brukes opp, også om det er gap mellom`() {
        val tidslinje = tidslinjeOf(248.NAVDAGER, 25.ARB, 2.NAVDAGER, startDato = 11.januar.minusDays(258))
        assertEquals(2, tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_11_JANUAR_2018).size)
        assertTrue(tidslinje.inspektør.avvistedager.all { it.begrunnelser.single() == Begrunnelse.SykepengedagerOppbrukt })
    }

    @Test
    fun `begrunnelse ved flere oppbrukte rettigheter - hel tidslinje`() {
        val tidslinje = tidslinjeOf(248.NAVDAGER, 1.NAVDAGER, (26 * 7).ARB, 60.NAVDAGER, 1.NAVDAGER, startDato = 11.januar.minusDays(248))
        assertEquals(2, tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_11_JANUAR_2018).size)
        assertEquals(Begrunnelse.SykepengedagerOppbrukt, tidslinje.inspektør.avvistedager.first().begrunnelser.single())
        assertEquals(Begrunnelse.SykepengedagerOppbruktOver67, tidslinje.inspektør.avvistedager.last().begrunnelser.single())
    }

    @Test
    fun `begrunnelse ved fylte 70`() {
        val tidslinje = tidslinjeOf(11.NAVDAGER)
        assertEquals(2, tidslinje.utbetalingsavgrenser(PERSON_70_ÅR_10_JANUAR_2018).size)
        assertTrue(tidslinje.inspektør.avvistedager.all { it.begrunnelser.single() == Begrunnelse.Over70 })
    }

    @Test
    fun `sjekk at 26 uker med syk etter karantene ikke starter utbetaling gammel person`() {
        val tidslinje = tidslinjeOf(11.NAVDAGER, 60.NAVDAGER, (26 * 7).NAVDAGER, 60.NAVDAGER)
        assertEquals(26*7 + 60, tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_11_JANUAR_2018).size)
    }

    @Test
    fun `sjekk at 26 uker med syk etter karantene starter utbetaling for gammel person etter første arbeidsdag`() {
        val tidslinje = tidslinjeOf(11.NAVDAGER, 60.NAVDAGER, (26 * 7).NAVDAGER, 1.ARB, 60.NAVDAGER)
        assertEquals(26*7, tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_11_JANUAR_2018).size)
    }

    @Test
    fun `helgedager teller som opphold`() {
        val tidslinje = tidslinjeOf(248.NAVDAGER, (26 * 7).HELG, 60.NAVDAGER)
        assertEquals(emptyList<LocalDate>(), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test
    fun `helgedager sammen med utbetalingsdager teller som opphold`() {
        val tidslinje =
            tidslinjeOf(248.NAVDAGER, (20 * 7).HELG, 7.NAVDAGER, (5 * 7).HELG, 60.NAVDAGER)
        assertEquals(7, tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018).size)
    }

    @Test
    fun `sjekk at sykdom i arbgiver periode ikke ødelegger oppholdsperioden`() {
        val tidslinje = tidslinjeOf(50.NAVDAGER, (25 * 7).ARB, 7.AP, 248.NAVDAGER)
        assertEquals(emptyList<LocalDate>(), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018, 1.april(2019) til 30.april(2019)))
    }

    @Test
    fun `helgedager innimellom utbetalingsdager betales ikke`() {
        val tidslinje = tidslinjeOf(200.NAVDAGER, 40.HELG, 48.NAVDAGER, 1.NAVDAGER)
        assertEquals(listOf(16.oktober), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test
    fun `ukjente dager generert når du legger til to utbetalingstidslinjer teller som ikke-utbetalingsdager`() {
        val tidslinje1 = tidslinjeOf(50.NAVDAGER)
        val tidslinje2 = tidslinjeOf(50.UTELATE, (26 * 7).UTELATE, 248.NAVDAGER)
        assertEquals(emptyList<LocalDate>(), (tidslinje1 + tidslinje2).utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test
    fun `248 dager nådd på 3 år`() {
        val tidslinje = tilbakevendendeSykdom(3.NAVDAGER, 10.ARB, 1.NAVDAGER)
        assertEquals(listOf(3.januar(2022), 14.januar(2022)), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test
    fun `sykepengedager eldre enn tre år teller ikke lenger som forbrukte dager`() {
        val tidslinje = tilbakevendendeSykdom(1.NAVDAGER, 3.ARB, 5.NAVDAGER, 1.NAVDAGER)
        assertEquals(listOf(10.januar(2022)), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test
    fun `26 ukers friske tilbakestiller skyvevindu på 3 år`() {
        val tidslinje = enAnnenSykdom(1.NAVDAGER, 3.ARB, 5.NAVDAGER, (248-60).NAVDAGER, 1.NAVDAGER)
        assertEquals(listOf(17.juli(2022)), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test
    fun `teller sykedager med opphold i sykdom`() {
        val tidslinje = tidslinjeOf(12.NAV, startDato = 1.mars)
        val historikk = tidslinjeOf(45.NAV, startDato = 1.januar(2018))
        tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018, personTidslinje = historikk)
        assertEquals(41, rettighet.forbrukteSykedager)
    }

    @Test
    fun `teller sykedager med overlapp`() {
        val tidslinje = tidslinjeOf(12.NAV, startDato = 1.februar)
        val historikk = tidslinjeOf(12.ARB, 45.NAV, startDato = 1.januar(2018))
        tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018, personTidslinje = historikk)
        assertEquals(31, rettighet.forbrukteSykedager)
    }

    @Test
    fun `teller sykedager med konflikt`() {
        val tidslinje = tidslinjeOf(12.NAV, startDato = 1.januar)
        val historikk = tidslinjeOf(12.ARB, 45.NAV, startDato = 1.januar)
        tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018, personTidslinje = historikk)
        assertEquals(41, rettighet.forbrukteSykedager)
    }

    @Test
    fun `teller sykedager med 26 uker`() {
        val tidslinje = enAnnenSykdom()
        tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018)
        assertEquals(54, rettighet.forbrukteSykedager)
    }

    // No 26 week gap with base of 246 NAV days
    private fun tilbakevendendeSykdom(vararg utbetalingsdager: Utbetalingsdager): Utbetalingstidslinje {
        return tidslinjeOf(
            365.ARB,
            48.NAVDAGER,
            152.ARB,
            48.NAVDAGER,
            152.ARB,
            48.NAVDAGER,
            152.ARB,
            48.NAVDAGER,
            152.ARB,
            48.NAVDAGER,
            152.ARB,
            6.NAVDAGER,
            90.ARB,
            *utbetalingsdager
        )
    }

    // 26 week gap inside 3 year window of 246 days with 54 NAV days after the gap
    private fun enAnnenSykdom(vararg utbetalingsdager: Utbetalingsdager): Utbetalingstidslinje {
        return tidslinjeOf(
            365.ARB,
            48.NAVDAGER,
            152.ARB,
            48.NAVDAGER,
            152.ARB,
            48.NAVDAGER,
            152.ARB,
            18.NAVDAGER,
            182.ARB,
            48.NAVDAGER,
            152.ARB,
            6.NAVDAGER,
            90.ARB,
            *utbetalingsdager
        )
    }

    private fun Utbetalingstidslinje.utbetalingsavgrenser(fnr: String, periode: Periode = Periode(1.januar, 31.desember), personTidslinje: Utbetalingstidslinje = Utbetalingstidslinje()): List<LocalDate> {
        rettighet = MaksimumSykepengedagerfilter(
            fnr.somFødselsnummer().alder(),
            NormalArbeidstaker,
            periode,
            aktivitetslogg
        ).filter(listOf(this), personTidslinje)
        return inspektør.avvistedatoer
    }
}
