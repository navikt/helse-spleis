package no.nav.helse.utbetalingstidslinje

import no.nav.helse.fixtures.A
import no.nav.helse.fixtures.F
import no.nav.helse.fixtures.S
import no.nav.helse.fixtures.resetSeed
import no.nav.helse.person.Inntekthistorikk
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.UtbetalingsdagVisitor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.reflect.KClass

internal class UtbetalingstidslinjeBuilderTest {

    private lateinit var tidslinje: Utbetalingstidslinje
    private val inspektør get() = TestTidslinjeInspektør(tidslinje)

    @BeforeEach
    internal fun reset() {
        resetSeed()
    }

    @Test
    fun `to dager blir betalt av arbeidsgiver`() {
        2.S.utbetalingslinjer()
        assertEquals(null, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
    }

    @Test
    fun `en utbetalingslinje med tre dager`() {
        (16.S + 3.S).utbetalingslinjer()
        assertEquals(3, inspektør.dagtelling[NavDag::class])
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
    }

    @Test
    fun `en utbetalingslinje med helg`() {
        (16.S + 6.S).utbetalingslinjer()
        assertEquals(4, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[NavHelgDag::class])
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
    }

    @Test
    fun `utbetalingslinjer starter aldri med helg`() { // TODO: ? Spør Fred 🧙🏻‍
        (3.A + 16.S + 6.S).utbetalingslinjer()
        assertEquals(4, inspektør.dagtelling[NavDag::class])
        assertEquals(null, inspektør.dagtelling[NavHelgDag::class])
        assertEquals(18, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
    }

    @Test
    fun `Sykedager med inneklemte arbeidsdager`() {
        (16.S + 7.S + 2.A + 1.S).utbetalingslinjer() //6 utbetalingsdager
        assertEquals(6, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[NavHelgDag::class])
        assertEquals(2, inspektør.dagtelling[Arbeidsdag::class])
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
    }

    @Test
    fun `Arbeidsdager i arbeidsgiverperioden`() {
        (15.S + 2.A + 1.S + 7.S).utbetalingslinjer()
        assertEquals(5, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[NavHelgDag::class])
        assertEquals(2, inspektør.dagtelling[Arbeidsdag::class])
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
    }


    @Test
    fun `Ferie i arbeidsgiverperiode`() {
        (1.S + 2.F + 13.S + 1.S).utbetalingslinjer()
        assertEquals(1, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[Fridag::class])
        assertEquals(14, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
    }

    @Test
    fun `Arbeidsdag etter feire i arbeidsgiverperioden`() {
        (1.S + 2.F + 1.A + 1.S + 14.S + 3.S).utbetalingslinjer()
        assertEquals(1, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[Fridag::class])
        assertEquals(1, inspektør.dagtelling[Arbeidsdag::class])
        assertEquals(18, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
    }

    @Test
    fun `Arbeidsdag før ferie i arbeidsgiverperioden`() {
        (1.S + 1.A + 2.F + 1.S + 14.S + 3.S).utbetalingslinjer()
        assertEquals(1, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[Fridag::class])
        assertEquals(1, inspektør.dagtelling[Arbeidsdag::class])
        assertEquals(18, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
//        assertEquals(1, betalingslinjer.size)
//        assert(betalingslinjer.first(), 22.januar, 22.januar, 1200)
    }
//
//    @Test
//    fun `Ferie etter arbeidsgiverperioden`() {
//        (16.S + 2.F + 1.S).utbetalingslinjer()
//
//        assertEquals(1, betalingslinjer.size)
//        assert(betalingslinjer.first(), 19.januar, 19.januar, 1200)
//    }
//
//    @Test
//    fun `Arbeidsdag etter ferie teller som gap`() {
//        (15.S + 2.F + 1.A + 1.S).utbetalingslinjer()
//
//        assertEquals(0, betalingslinjer.size)
//    }
//
//    @Test
//    fun `Ferie rett etter arbeidsgiverperioden teller ikke som opphold`() {
//        (16.S + 16.F + 1.A + 3.S).utbetalingslinjer()
//
//        assertEquals(1, betalingslinjer.size)
//        assert(betalingslinjer.first(), 5.februar, 5.februar, 1000)
//    }
//
//    @Test
//    fun `Ferie i slutten av arbeidsgiverperioden teller som opphold`() {
//        (15.S + 16.F + 1.A + 3.S).utbetalingslinjer()
//        assertEquals(0, betalingslinjer.size)
//    }
//
//    @Test
//    fun `Ferie og arbeid påvirker ikke initiell tilstand`() {
//        (2.F + 2.A + 16.S + 2.F).utbetalingslinjer()
//        assertEquals(0, betalingslinjer.size)
//    }
//
//    @Test
//    fun `Arbeidsgiverperioden resettes når det er opphold over 16 dager`() {
//        (10.S + 20.F + 1.A + 10.S + 20.F).utbetalingslinjer()
//        assertEquals(0, betalingslinjer.size)
//    }
//
//    @Test
//    fun `Ferie fullfører arbeidsgiverperioden`() {
//        (10.S + 20.F + 10.S).utbetalingslinjer()
//
//        assertEquals(1, betalingslinjer.size)
//        assert(betalingslinjer.first(), 31.januar, 9.februar, 1200)
//    }
//
//    @Test
//    fun `Ferie mer enn 16 dager gir ikke ny arbeidsgiverperiode`() {
//        (20.S + 20.F + 10.S).utbetalingslinjer()
//
//        assertEquals(2, betalingslinjer.size)
//        assert(betalingslinjer.first(), 17.januar, 19.januar, 1200)
//        assert(betalingslinjer.last(), 12.februar, 19.februar, 1200)
//    }
//
//    @Test
//    fun `egenmelding sammen med sykdom oppfører seg som sykdom`() {
//        (5.E + 15.S).utbetalingslinjer()
//
//        assertEquals(1, betalingslinjer.size)
//        assert(betalingslinjer.first(), 17.januar, 19.januar, 1200)
//    }
//
//    @Test
//    fun `16 dagers opphold etter en utbetaling gir ny arbeidsgiverperiode ved påfølgende sykdom`() {
//        (22.S + 16.A + 10.S).utbetalingslinjer()
//
//        assertEquals(1, betalingslinjer.size)
//        assert(betalingslinjer.first(), 17.januar, 22.januar, 1200)
//    }
//
//    @Test
//    fun `Ferie i arbeidsgiverperioden direkte etterfulgt av en arbeidsdag gjør at ferien teller som opphold`() {
//        (10.S + 15.F + 1.A + 10.S).utbetalingslinjer()
//
//        assertEquals(0, betalingslinjer.size)
//    }
//
//    @Test
//    fun `Ferie etter arbeidsdag i arbeidsgiverperioden gjør at ferien teller som opphold`() {
//        (10.S + 1.A + 15.F + 10.S).utbetalingslinjer()
//
//        assertEquals(0, betalingslinjer.size)
//    }
//
//    @Test
//    fun `Ferie direkte etter arbeidsgiverperioden teller ikke som opphold, selv om det er en direkte etterfølgende arbeidsdag`() {
//        (16.S + 15.F + 1.A + 10.S).utbetalingslinjer()
//
//        assertEquals(1, betalingslinjer.size)
//        assert(betalingslinjer.first(), 2.februar, 9.februar, 1000)
//    }
//
//    @Test
//    fun `Ferie direkte etter en sykedag utenfor arbeidsgiverperioden teller ikke som opphold, selv om det er en direkte etterfølgende arbeidsdag`() {
//        (20.S + 15.F + 1.A + 10.S).utbetalingslinjer()
//
//        assertEquals(2, betalingslinjer.size)
//        assert(betalingslinjer.first(), 17.januar, 19.januar, 1200)
//        assert(betalingslinjer.last(), 6.februar, 15.februar, 1000)
//    }
//
//    @Test
//    fun `Ferie direkte etter en arbeidsdag utenfor arbeidsgiverperioden teller som opphold`() {
//        (21.S + 1.A + 15.F + 10.S).utbetalingslinjer()
//
//        assertEquals(1, betalingslinjer.size)
//        assert(betalingslinjer.first(), 17.januar, 19.januar, 1200)
//    }
//
//    @Test
//    fun `Ferie direkte etter en sykedag utenfor arbeidsgiverperioden teller ikke som opphold, mens ferie direkte etter en arbeidsdag utenfor arbeidsgiverperioden teller som opphold, så A + 15F gir ett opphold på 16 dager og dette resulterer i to arbeidsgiverperioder`() {
//        (17.S + 4.F + 1.A + 15.F + 10.S).utbetalingslinjer()
//
//        assertEquals(1, betalingslinjer.size)
//        assert(betalingslinjer.first(), 17.januar, 17.januar, 1200)
//    }
//
//    @Test
//    fun `Ferie direkte etter en sykedag utenfor arbeidsgiverperioden teller ikke som opphold, mens ferie direkte etter en arbeidsdag utenfor arbeidsgiverperioden teller som opphold, så A + 14F gir ett opphold på 15 dager og dette resulterer i en arbeidsgiverperiode`() {
//        (17.S + 3.F + 1.A + 14.F + 10.S).utbetalingslinjer()
//
//        assertEquals(2, betalingslinjer.size)
//        assert(betalingslinjer.first(), 17.januar, 17.januar, 1200)
//        assert(betalingslinjer.last(), 5.februar, 14.februar, 1200)
//    }
//
//    @Test
//    fun `beregn maksdato i et sykdomsforløp som slutter på en fredag`() {
//        (20.S).utbetalingslinjer()
//        assertEquals(28.desember, maksdato) // 3 dager already paid, 245 left. So should be fredag!
//    }
//
//    @Test
//    fun `beregn maksdato i et sykdomsforløp med opphold i sykdom`() {
//        (2.A + 20.S + 7.A + 20.S).utbetalingslinjer() //18feb
//        assertEquals(8.januar(2019), maksdato)
//    }
//
//    @Test
//    fun `beregn maksdato (med rest) der den ville falt på en lørdag`() {
//        (351.S + 1.F + 1.S).utbetalingslinjer()
//        assertEquals(31.desember, maksdato)
//    }
//
//    @Test
//    fun `beregn maksdato (med rest) der den ville falt på en søndag`() {
//        (23.S + 2.F + 1.S).utbetalingslinjer()
//        assertEquals(1.januar(2019), maksdato )
//    }
//
//    @Test
//    fun `maksdato forskyves av ferie etterfulgt av sykedager`() {
//        (21.S + 3.F + 1.S).utbetalingslinjer()
//        assertEquals(2.januar(2019), maksdato)
//    }
//
//    @Test
//    fun `maksdato forskyves ikke av ferie på tampen av sykdomstidslinjen`() {
//        (21.S + 3.F).utbetalingslinjer()
//        assertEquals(28.desember, maksdato)
//    }
//
//    @Test
//    fun `maksdato forskyves ikke av ferie etterfulgt av arbeidsdag på tampen av sykdomstidslinjen`() {
//        (21.S + 3.F + 1.A).utbetalingslinjer()
//        assertEquals(28.desember, maksdato)
//    }
//
//    @Test
//    fun `maksdato er udefinert hvis det ikke er utbetalingsdager`() {
//        (16.S).utbetalingslinjer()
//        assertNull(maksdato)
//    }
//
//    @Test
//    fun `arbeidsgiverperiode med to påfølgende sykedager i helg blir ingen utbetalingslinjer`() {
//        (3.A + 18.S).utbetalingslinjer()
//        assertEquals(0, betalingslinjer.size)
//    }
//
//    @Test
//    fun `arbeidsgiverperiode med tre påfølgende sykedager i helg`() {
//        (3.A + 19.S).utbetalingslinjer()
//        assertEquals(1, betalingslinjer.size)
//        assert(betalingslinjer.first(), 22.januar, 22.januar, 1200)
//    }
//
//    @Test
//    fun `arbeidsgiverperiode med tre påfølgende sykedager i helg blir ingen utbetalingslinjer`() {
//        (2.A + 19.S).utbetalingslinjer()
//        assertEquals(1, betalingslinjer.size)
//        assert(betalingslinjer.first(), 19.januar, 19.januar, 1200)
//    }
//
//    @Test
//    fun `arbeidsgiverperiode til fredag med sykedager over helg gir ingen betalingslinjer`() {
//        (16.S + 3.A + 2.S).utbetalingslinjer()
//        assertEquals(0, betalingslinjer.size)
//    }
//
//    @Test
//    fun `arbeidsgiverperioden slutter på en søndag`() {
//        (3.A + 5.S + 2.F + 13.S).utbetalingslinjer()
//        assertEquals(1, betalingslinjer.size)
//        assert(betalingslinjer.first(), 22.januar, 23.januar, 1200)
//    }
//
//    @Test
//    fun `når sykepengeperioden går over maksdato, så skal utbetaling stoppe ved maksdato`() {
//        (368.S).utbetalingslinjer()
//        assertEquals(1, betalingslinjer.size)
//        assert(betalingslinjer.first(), 17.januar, 28.desember, 1200)
//    }
//
//    @Test
//    fun `når personen fyller 67 blir antall gjenværende dager 60`() {
//        (16.S + 90.S).utbetalingslinjer(fødselsnummer = AlderReglerTest.PERSON_67_ÅR_FNR_2018)
//        assert(betalingslinjer.first(), 17.januar, 10.april, 1200)
//    }
//
//    @Test
//    fun `når personen fyller 67 og 248 dager er brukt opp`() {
//        (400.S).utbetalingslinjer(fødselsnummer = "01125112345")
//        assert(betalingslinjer.first(), 17.januar, 28.desember, 1200)
//    }
//
//    @Test
//    fun `når personen fyller 70 skal det ikke utbetales sykepenger`() {
//        (400.S).utbetalingslinjer(fødselsnummer = "01024812345")
//        assert(betalingslinjer.first(), 17.januar, 31.januar, 1200)
//    }
//
//    @Test
//    fun `ta hensyn til en andre arbeidsgiverperiode, ferieopphold`() {
//        (16.S + 6.S + 16.F + 1.A + 16.S).utbetalingslinjer()
//        assertEquals(2, betalingslinjer.size)
//        assert(betalingslinjer.first(), 17.januar, 22.januar, 1200)
//        assert(betalingslinjer.last(), 9.februar, 23.februar, 1000)
//    }
//
//    @Test
//    fun `ta hensyn til en andre arbeidsgiverperiode, arbeidsdageropphold`() {
//        (16.S + 6.S + 16.A + 16.S).utbetalingslinjer()
//        assertEquals(1, betalingslinjer.size)
//        assert(betalingslinjer.first(), 17.januar, 22.januar, 1200)
//    }
//
//    @Test
//    fun `ta hensyn til en andre arbeidsgiverperiode, arbeidsdageropphold der sykedager går over helg`() {
//        (16.S + 6.S + 16.A + 16.S).utbetalingslinjer()
//        assertEquals(1, betalingslinjer.size)
//        assert(betalingslinjer.first(), 17.januar, 22.januar, 1200)
//    }
//
//    @Test
//    fun `permisjon i en arbeidsgiverperiode telles som ferie`() {
//        (4.S + 4.P + 9.S).utbetalingslinjer()
//        assertEquals(1, betalingslinjer.size)
//        assert(betalingslinjer.first(), 17.januar, 17.januar, 1200)
//    }
//
//    @Test
//    fun `permisjon med påfølgende arbeidsdag teller som opphold i sykeperioden`() {
//        (4.S + 4.P + 1.A + 16.S).utbetalingslinjer()
//        assertEquals(1, betalingslinjer.size)
//        assert(betalingslinjer.first(), 22.januar, 25.januar, 1200)
//    }
//
//    @Test
//    fun `permisjon direkte etter arbeidsgiverperioden teller ikke som opphold`() {
//        (16.S + 10.P + 15.A + 3.S).utbetalingslinjer()
//        assertEquals(1, betalingslinjer.size)
//        assert(betalingslinjer.first(), 12.februar, 13.februar, 1000)
//    }
//
//    @Test
//    fun `resetter arbeidsgiverperioden etter 16 arbeidsdager`() {
//        (15.S + 16.A + 14.S).utbetalingslinjer()
//        assertEquals(0, betalingslinjer.size)
//    }
//
//    @Test
//    fun `utlands-, ubestemt- og utdanningsdager teller som opphold`() {
//        (15.S + 10.U + 4.EDU + 3.UT + 14.S).utbetalingslinjer()
//        assertEquals(0, betalingslinjer.size)
//    }
//
//    @Test
//    fun `oppdeltArbeidsgiverutbetalingstidslinje har ingen sykedager betalt av nav`() {
//        (50.S).utbetalingslinjer(1.januar, 10.januar)
//        assertEquals(0, betalingslinjer.size)
//    }
//
//    @Test
//    fun `oppdelt tidslinje i arbeidsgiverperioden`() {
//        (50.S).utbetalingslinjer(10.januar, 20.januar)
//        assertEquals(1, betalingslinjer.size)
//        assert(betalingslinjer.first(), 17.januar, 19.januar, 1200)
//    }
//
//    @Test
//    fun `oppdelt tidslinje etter arbeidsgiverperioden`() {
//        (50.S).utbetalingslinjer(21.januar, 30.januar)
//        assertEquals(1, betalingslinjer.size)
//        assert(betalingslinjer.first(), 22.januar, 30.januar, 1200)
//    }
//
//    @Test
//    fun `oppdelt tidslinje blir bare helg`() {
//        (21.S).utbetalingslinjer(20.januar, 21.januar)
//        assertEquals(0, betalingslinjer.size)
//    }
//
//    @Test
//    fun `NAV betaler første dag når 15-dagers opphold`() {
//        (1.S).utbetalingslinjer(sisteNavDagForArbeidsgiverFørPerioden = 1.januar.minusDays(16))
//        assertEquals(1, betalingslinjer.size)
//        assert(betalingslinjer.first(), 1.januar, 1.januar, 1200)
//    }
//
//    @Test
//    fun `NAV betaler IKKE første dag når 16-dagers opphold`() {
//        (1.S).utbetalingslinjer(sisteNavDagForArbeidsgiverFørPerioden = 1.januar.minusDays(17))
//        assertEquals(0, betalingslinjer.size)
//    }
//
//    @Test
//    fun `siste dag i arbeidsgiverperioden faller på mandag`() {
//        (1.S + 3.A + 4.S + 3.A + 11.S + 4.S).utbetalingslinjer()
//        assertEquals(LocalDate.of(2018, 1, 23), betalingslinjer.first().fom)
//    }
//
//    @Test
//    fun `siste dag i arbeidsgiverperioden faller på søndag`() {
//        (1.S + 3.A + 4.S + 2.A + 12.S + 4.S).utbetalingslinjer()
//        assertEquals(LocalDate.of(2018, 1, 22), betalingslinjer.first().fom)
//    }
//
//    @Test
//    fun `siste dag i arbeidsgiverperioden faller på lørdag`() {
//        (1.S + 3.A + 4.S + 1.A + 13.S + 4.S).utbetalingslinjer()
//        assertEquals(LocalDate.of(2018, 1, 22), betalingslinjer.first().fom)
//    }

    private fun ConcreteSykdomstidslinje.utbetalingslinjer(
        sisteDag: LocalDate = this.sisteDag(),
        sisteNavDagForArbeidsgiverFørPerioden: LocalDate? = null
    ) {
        tidslinje = UtbetalingstidslinjeBuilder(
            sykdomstidslinje = this,
            sisteDag = sisteDag,
            inntekthistorikk = Inntekthistorikk(),
            sisteNavDagForArbeidsgiverFørPerioden = sisteNavDagForArbeidsgiverFørPerioden
        ).result()
    }

    private class TestTidslinjeInspektør(tidslinje: Utbetalingstidslinje) : UtbetalingsdagVisitor {

        internal val dagtelling: MutableMap<KClass<out Utbetalingsdag>, Int> = mutableMapOf()

        init {
            tidslinje.accept(this)
        }

        override fun visitNavDag(dag: NavDag) = inkrementer(NavDag::class)

        override fun visitArbeidsdag(dag: Arbeidsdag) = inkrementer(Arbeidsdag::class)

        override fun visitNavHelgDag(dag: NavHelgDag) = inkrementer(NavHelgDag::class)

        override fun visitArbeidsgiverperiodeDag(dag: ArbeidsgiverperiodeDag) =
            inkrementer(ArbeidsgiverperiodeDag::class)

        override fun visitFridag(dag: Fridag) = inkrementer(Fridag::class)

        private fun inkrementer(klasse: KClass<out Utbetalingsdag>) {
            dagtelling.compute(klasse) { _, value -> 1 + (value ?: 0) }
        }

    }

}

