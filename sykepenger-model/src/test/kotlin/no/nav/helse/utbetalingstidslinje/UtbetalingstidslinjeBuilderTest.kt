package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate
import java.util.*

internal class UtbetalingstidslinjeBuilderTest {
    private val hendelseId = UUID.randomUUID()
    private lateinit var tidslinje: Utbetalingstidslinje
    private val infotrygdUtbetaling = fun(dager: List<LocalDate>) =
        Forlengelsestrategi { dagen -> dagen in dager }

    @BeforeEach
    internal fun reset() {
        resetSeed()
    }

    @Test
    fun `to dager blir betalt av arbeidsgiver`() {
        2.S.utbetalingslinjer()
        assertEquals(1, tidslinje.inspektør.unikedager.size)
        assertEquals(2, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
    }

    @Test
    fun `overgang fra infotrygd`() {
        2.S.utbetalingslinjer(strategi = infotrygdUtbetaling(listOf(1.januar)))
        assertEquals(1, tidslinje.inspektør.unikedager.size)
        assertEquals(2, tidslinje.inspektør.navDagTeller)
    }

    @Test
    fun `sammenblandet infotrygd og spleis`() {
        (2.S + 15.A + 2.S).utbetalingslinjer(strategi = infotrygdUtbetaling(listOf(1.januar)))
        assertEquals(2, tidslinje.inspektør.unikedager.size)
        assertEquals(4, tidslinje.inspektør.navDagTeller)
    }

    @Test
    fun `ny arbeidsgiverperiode i spleis`() {
        (2.S + 16.A + 20.S).utbetalingslinjer(strategi = infotrygdUtbetaling(listOf(1.januar)))
        assertEquals(4, tidslinje.inspektør.unikedager.size)
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(5, tidslinje.inspektør.navDagTeller)
        assertEquals(1, tidslinje.inspektør.navHelgDagTeller)
    }

    @Test
    fun `infotrygd midt i`() {
        (20.S + 32.A + 20.S).utbetalingslinjer(strategi = infotrygdUtbetaling(listOf(22.februar)))
        assertEquals(4, tidslinje.inspektør.unikedager.size)
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(32, tidslinje.inspektør.arbeidsdagTeller)
        assertEquals(17, tidslinje.inspektør.navDagTeller)
        assertEquals(7, tidslinje.inspektør.navHelgDagTeller)
    }

    @Test
    fun `alt infotrygd`() {
        (20.S + 32.A + 20.S).utbetalingslinjer(strategi = infotrygdUtbetaling(listOf(2.januar, 22.februar)))
        assertEquals(4, tidslinje.inspektør.unikedager.size)
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(32, tidslinje.inspektør.arbeidsdagTeller)
        assertEquals(17, tidslinje.inspektør.navDagTeller)
        assertEquals(7, tidslinje.inspektør.navHelgDagTeller)
    }

    @Test
    fun `kort infotrygdperiode etter utbetalingopphold`() {
        (16.U + 1.S + 32.opphold + 5.S + 20.S).utbetalingslinjer(strategi = infotrygdUtbetaling(listOf(19.februar)))
        assertEquals(5, tidslinje.inspektør.unikedager.size)
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(22, tidslinje.inspektør.arbeidsdagTeller)
        assertEquals(10, tidslinje.inspektør.fridagTeller)
        assertEquals(20, tidslinje.inspektør.navDagTeller)
        assertEquals(6, tidslinje.inspektør.navHelgDagTeller)
    }

    @Test
    fun `sykedager i periode som starter i helg får riktig inntekt`() {
        resetSeed(6.januar)
        (16.S + 4.S).utbetalingslinjer()
        assertEquals(4, tidslinje.inspektør.navDagTeller)
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertInntekter(1431)
    }

    @Test
    fun `bare ferie`() {
        (20.F).utbetalingslinjer()
        assertEquals(20, tidslinje.inspektør.fridagTeller)
    }

    @Test
    fun `litt sykdom ellers bare ferie`() {
        (7.S + 20.F).utbetalingslinjer()
        assertEquals(7, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(20, tidslinje.inspektør.fridagTeller)
    }

    @Test
    fun `litt sykdom ellers bare ferie etterfulgt av arbeidsdag`() {
        (7.S + 20.F + 1.A).utbetalingslinjer()
        assertEquals(7, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(20, tidslinje.inspektør.fridagTeller)
        assertEquals(1, tidslinje.inspektør.arbeidsdagTeller)
    }

    @Test
    fun `bare ferie etter arbeidsgiverperioden`() {
        (16.S + 20.F).utbetalingslinjer()
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(20, tidslinje.inspektør.fridagTeller)
    }

    @Test
    fun `sykdom fra Infotrygd ellers bare ferie`() {
        (7.S + 20.F).utbetalingslinjer(strategi = infotrygdUtbetaling(listOf(1.januar)))
        assertEquals(5, tidslinje.inspektør.navDagTeller)
        assertEquals(2, tidslinje.inspektør.navHelgDagTeller)
        assertEquals(20, tidslinje.inspektør.fridagTeller)
    }

    @Test
    fun `en utbetalingslinje med tre dager`() {
        (16.S + 3.S).utbetalingslinjer()
        assertEquals(3, tidslinje.inspektør.navDagTeller)
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
    }

    @Test
    fun `en utbetalingslinje med helg`() {
        (16.S + 6.S).utbetalingslinjer()
        assertEquals(4, tidslinje.inspektør.navDagTeller)
        assertEquals(2, tidslinje.inspektør.navHelgDagTeller)
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
    }

    @Test
    fun `utbetalingstidslinjer kan starte i helg`() {
        (3.A + 16.S + 6.S).utbetalingslinjer()
        assertEquals(4, tidslinje.inspektør.navDagTeller)
        assertEquals(2, tidslinje.inspektør.navHelgDagTeller)
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
    }

    @Test
    fun `Sykedager med inneklemte arbeidsdager`() {
        (16.S + 7.S + 2.A + 1.S).utbetalingslinjer() //6 utbetalingsdager
        assertEquals(6, tidslinje.inspektør.navDagTeller)
        assertEquals(2, tidslinje.inspektør.navHelgDagTeller)
        assertEquals(2, tidslinje.inspektør.arbeidsdagTeller)
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
    }

    @Test
    fun `Arbeidsdager i arbeidsgiverperioden`() {
        (15.S + 2.A + 1.S + 7.S).utbetalingslinjer()
        assertEquals(5, tidslinje.inspektør.navDagTeller)
        assertEquals(2, tidslinje.inspektør.navHelgDagTeller)
        assertEquals(2, tidslinje.inspektør.arbeidsdagTeller)
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
    }

    @Test
    fun `Ferie i arbeidsgiverperiode`() {
        (1.S + 2.F + 13.S + 1.S).utbetalingslinjer()
        assertEquals(1, tidslinje.inspektør.navDagTeller)
        assertEquals(0, tidslinje.inspektør.fridagTeller)
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
    }

    @Test
    fun `Arbeidsdag etter ferie i arbeidsgiverperioden`() {
        (1.S + 2.F + 1.A + 1.S + 14.S + 3.S).utbetalingslinjer()
        assertEquals(1, tidslinje.inspektør.navDagTeller)
        assertEquals(2, tidslinje.inspektør.navHelgDagTeller)
        assertEquals(2, tidslinje.inspektør.fridagTeller)
        assertEquals(1, tidslinje.inspektør.arbeidsdagTeller)
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
    }

    @Test
    fun `Arbeidsdag før ferie i arbeidsgiverperioden`() {
        (1.S + 1.A + 2.F + 1.S + 14.S + 3.S).utbetalingslinjer()
        assertEquals(1, tidslinje.inspektør.navDagTeller)
        assertEquals(2, tidslinje.inspektør.navHelgDagTeller)
        assertEquals(2, tidslinje.inspektør.fridagTeller)
        assertEquals(1, tidslinje.inspektør.arbeidsdagTeller)
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
    }

    @Test
    fun `Ferie etter arbeidsgiverperioden`() {
        (16.S + 2.F + 1.S).utbetalingslinjer()
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(2, tidslinje.inspektør.fridagTeller)
        assertEquals(1, tidslinje.inspektør.navDagTeller)
    }

    @Test
    fun `helg teller som opphold ved ufullstendig arbeidsgiverperiode dersom mandag er frisk`() {
        (4.opphold + 8.S + 16.opphold + 19.S).utbetalingslinjer()
        assertEquals(3, tidslinje.inspektør.navDagTeller)
        assertEquals(0, tidslinje.inspektør.navHelgDagTeller)
        assertEquals(24, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(6, tidslinje.inspektør.fridagTeller)
        assertEquals(10, tidslinje.inspektør.arbeidsdagTeller)
    }

    @Test
    fun `ferie teller som opphold ved ufullstendig arbeidsgiverperiode`() {
        (5.S + 14.F + 3.opphold + 19.S).utbetalingslinjer()
        assertEquals(2, tidslinje.inspektør.navDagTeller)
        assertEquals(1, tidslinje.inspektør.navHelgDagTeller)
        assertEquals(21, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(16, tidslinje.inspektør.fridagTeller)
        assertEquals(1, tidslinje.inspektør.arbeidsdagTeller)
    }

    @Test
    fun `ferie fullfører nesten arbeidsgiverperioden`() {
        (8.S + 7.F + 10.S).utbetalingslinjer()
        assertEquals(7, tidslinje.inspektør.navDagTeller)
        assertEquals(2, tidslinje.inspektør.navHelgDagTeller)
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(0, tidslinje.inspektør.fridagTeller)
    }

    @Test
    fun `ferie fullfører arbeidsgiverperioden`() {
        (8.S + 8.F + 9.S).utbetalingslinjer()
        assertEquals(7, tidslinje.inspektør.navDagTeller)
        assertEquals(2, tidslinje.inspektør.navHelgDagTeller)
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(0, tidslinje.inspektør.fridagTeller)
    }

    @Test
    fun `ferie fullfører arbeidsgiverperioden - slutter på en fredag`() {
        (3.opphold + 8.S + 8.F + 2.opphold + 9.S).utbetalingslinjer()
        assertEquals(7, tidslinje.inspektør.navDagTeller)
        assertEquals(2, tidslinje.inspektør.navHelgDagTeller)
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(2, tidslinje.inspektør.fridagTeller)
    }

    @Test
    fun `ferie teller ikke som opphold ved ufullstendig arbeidsgiverperiode dersom syk etterpå`() {
        (8.S + 7.F + 1.S + 9.opphold + 1.S).utbetalingslinjer()
        assertEquals(1, tidslinje.inspektør.navDagTeller)
        assertEquals(0, tidslinje.inspektør.navHelgDagTeller)
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(2, tidslinje.inspektør.fridagTeller)
        assertEquals(7, tidslinje.inspektør.arbeidsdagTeller)
    }

    @Test
    fun `Arbeidsdag etter ferie i arbeidsgiverperiode teller som gap, men ikke ferie`() {
        (15.S + 2.F + 1.A + 1.S).utbetalingslinjer()
        assertEquals(0, tidslinje.inspektør.navDagTeller)
        assertEquals(2, tidslinje.inspektør.fridagTeller)
        assertEquals(1, tidslinje.inspektør.arbeidsdagTeller)
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
    }

    @Test
    fun `Ferie rett etter arbeidsgiverperioden teller ikke som opphold`() {
        (16.S + 16.F + 1.A + 3.S).utbetalingslinjer()
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(16, tidslinje.inspektør.fridagTeller)
        assertEquals(1, tidslinje.inspektør.arbeidsdagTeller)
        assertEquals(2, tidslinje.inspektør.navHelgDagTeller)
        assertEquals(1, tidslinje.inspektør.navDagTeller)
    }

    @Test
    fun `Ferie i slutten av arbeidsgiverperioden teller som opphold`() {
        (15.S + 16.F + 1.A + 3.S).utbetalingslinjer()
        assertEquals(18, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(16, tidslinje.inspektør.fridagTeller)
        assertEquals(1, tidslinje.inspektør.arbeidsdagTeller)
    }

    @Test
    fun `Ferie og arbeid påvirker ikke initiell tilstand`() {
        (2.F + 2.A + 16.S + 2.F).utbetalingslinjer()
        assertEquals(4, tidslinje.inspektør.fridagTeller)
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(2, tidslinje.inspektør.arbeidsdagTeller)
    }

    @Test
    fun `Arbeidsgiverperioden resettes når det er opphold over 16 dager`() {
        (10.S + 20.F + 1.A + 10.S + 20.F).utbetalingslinjer()
        assertEquals(20, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(40, tidslinje.inspektør.fridagTeller)
        assertEquals(1, tidslinje.inspektør.arbeidsdagTeller)
    }

    @Test
    fun `Ferie fullfører arbeidsgiverperioden`() {
        (10.S + 20.F + 10.S).utbetalingslinjer()
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(14, tidslinje.inspektør.fridagTeller)
        assertEquals(8, tidslinje.inspektør.navDagTeller)
        assertEquals(2, tidslinje.inspektør.navHelgDagTeller)
    }

    @Test
    fun `Ferie mer enn 16 dager gir ikke ny arbeidsgiverperiode`() {
        (20.S + 20.F + 10.S).utbetalingslinjer()
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(9, tidslinje.inspektør.navDagTeller)
        assertEquals(5, tidslinje.inspektør.navHelgDagTeller)
        assertEquals(20, tidslinje.inspektør.fridagTeller)
    }

    @Test
    fun `egenmelding sammen med sykdom oppfører seg som sykdom`() {
        (5.U + 15.S).utbetalingslinjer()
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(3, tidslinje.inspektør.navDagTeller)
        assertEquals(1, tidslinje.inspektør.navHelgDagTeller)
    }

    @Test
    fun `16 dagers opphold etter en utbetaling gir ny arbeidsgiverperiode ved påfølgende sykdom`() {
        (22.S + 16.A + 10.S).utbetalingslinjer()
        assertEquals(26, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(4, tidslinje.inspektør.navDagTeller)
        assertEquals(2, tidslinje.inspektør.navHelgDagTeller)
        assertEquals(16, tidslinje.inspektør.arbeidsdagTeller)
    }

    @Test
    fun `Ferie i arbeidsgiverperioden direkte etterfulgt av en arbeidsdag gjør at ferien teller som opphold`() {
        (10.S + 15.F + 1.A + 10.S).utbetalingslinjer()
        assertEquals(20, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(1, tidslinje.inspektør.arbeidsdagTeller)
        assertEquals(15, tidslinje.inspektør.fridagTeller)
        assertEquals(0, tidslinje.inspektør.navDagTeller)
    }

    @Test
    fun `Ferie etter arbeidsdag i arbeidsgiverperioden gjør at ferien teller som opphold`() {
        (10.S + 1.A + 15.F + 10.S).utbetalingslinjer()
        assertEquals(20, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(1, tidslinje.inspektør.arbeidsdagTeller)
        assertEquals(15, tidslinje.inspektør.fridagTeller)
        assertEquals(0, tidslinje.inspektør.navDagTeller)
    }

    @Test
    fun `sykedag i helg etter ferie`() {
        (3.opphold + 14.S + 2.F + 2.S).utbetalingslinjer()
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(0, tidslinje.inspektør.fridagTeller)
        assertEquals(2, tidslinje.inspektør.navHelgDagTeller)
        assertEquals(0, tidslinje.inspektør.navDagTeller)
    }

    @Test
    fun `siste sykedag faller på fredag - lørdag og søndag teller ikke som opphold`() {
        (19.S + 2.UK + 14.A + 19.S).utbetalingslinjer()
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(2, tidslinje.inspektør.fridagTeller)
        assertEquals(14, tidslinje.inspektør.arbeidsdagTeller)
        assertEquals(4, tidslinje.inspektør.navHelgDagTeller)
        assertEquals(18, tidslinje.inspektør.navDagTeller)
    }

    @Test
    fun `starter ny arbeidsgiverperiode etter ferie og arbeidsdag dersom oppholdet er høyt nok`() {
        (2.S + 15.F + 1.A + 17.S).utbetalingslinjer()
        assertEquals(18, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(0, tidslinje.inspektør.navDagTeller)
        assertEquals(1, tidslinje.inspektør.navHelgDagTeller)
        assertEquals(1, tidslinje.inspektør.arbeidsdagTeller)
        assertEquals(15, tidslinje.inspektør.fridagTeller)
    }

    @Test
    fun `starter ikke ny arbeidsgiverperiode etter ferie og arbeidsdag dersom oppholdet er akkurat lavt nok`() {
        (2.S + 14.F + 1.A + 17.S).utbetalingslinjer()
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(2, tidslinje.inspektør.navDagTeller)
        assertEquals(1, tidslinje.inspektør.navHelgDagTeller)
        assertEquals(1, tidslinje.inspektør.arbeidsdagTeller)
        assertEquals(14, tidslinje.inspektør.fridagTeller)
    }

    @Test
    fun `Ferie direkte etter arbeidsgiverperioden teller ikke som opphold, selv om det er en direkte etterfølgende arbeidsdag`() {
        (16.S + 15.F + 1.A + 10.S).utbetalingslinjer()
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(15, tidslinje.inspektør.fridagTeller)
        assertEquals(1, tidslinje.inspektør.arbeidsdagTeller)
        assertEquals(6, tidslinje.inspektør.navDagTeller)
        assertEquals(4, tidslinje.inspektør.navHelgDagTeller)
    }

    @Test
    fun `Ferie direkte etter en sykedag utenfor arbeidsgiverperioden teller ikke som opphold, selv om det er en direkte etterfølgende arbeidsdag`() {
        (20.S + 15.F + 1.A + 10.S).utbetalingslinjer()
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(15, tidslinje.inspektør.fridagTeller)
        assertEquals(1, tidslinje.inspektør.arbeidsdagTeller)
        assertEquals(11, tidslinje.inspektør.navDagTeller)
        assertEquals(3, tidslinje.inspektør.navHelgDagTeller)
    }

    @Test
    fun `Ferie direkte etter en arbeidsdag utenfor arbeidsgiverperioden teller som opphold`() {
        (21.S + 1.A + 15.F + 10.S).utbetalingslinjer()
        assertEquals(26, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(15, tidslinje.inspektør.fridagTeller)
        assertEquals(1, tidslinje.inspektør.arbeidsdagTeller)
        assertEquals(3, tidslinje.inspektør.navDagTeller)
        assertEquals(2, tidslinje.inspektør.navHelgDagTeller)
    }

    @Test
    fun `Ferie direkte etter en sykedag utenfor arbeidsgiverperioden teller ikke som opphold, mens ferie direkte etter en arbeidsdag utenfor arbeidsgiverperioden teller som opphold, så A + 15F gir ett opphold på 16 dager og dette resulterer i to arbeidsgiverperioder`() {
        (17.S + 4.F + 1.A + 15.F + 10.S).utbetalingslinjer()
        assertEquals(26, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(19, tidslinje.inspektør.fridagTeller)
        assertEquals(1, tidslinje.inspektør.arbeidsdagTeller)
        assertEquals(1, tidslinje.inspektør.navDagTeller)
    }

    @Test
    fun `Ferie direkte etter en sykedag utenfor arbeidsgiverperioden teller ikke som opphold, mens ferie direkte etter en arbeidsdag utenfor arbeidsgiverperioden teller som opphold, så A + 13F gir ett opphold på 14 dager og dette resulterer i én arbeidsgiverperiode`() {
        (17.S + 4.F + 1.A + 13.F + 10.S).utbetalingslinjer()
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(17, tidslinje.inspektør.fridagTeller)
        assertEquals(9, tidslinje.inspektør.navDagTeller)
        assertEquals(2, tidslinje.inspektør.navHelgDagTeller)
        assertEquals(1, tidslinje.inspektør.arbeidsdagTeller)
    }

    @Test
    fun `arbeidsgiverperiode med tre påfølgende sykedager i helg`() {
        (3.A + 19.S).utbetalingslinjer()
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(3, tidslinje.inspektør.arbeidsdagTeller)
        assertEquals(1, tidslinje.inspektør.navDagTeller)
        assertEquals(2, tidslinje.inspektør.navHelgDagTeller)
    }

    @Test
    fun `arbeidsgiverperioden slutter på en fredag`() {
        (3.A + 5.S + 2.F + 13.S).utbetalingslinjer()
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(0, tidslinje.inspektør.fridagTeller)
        assertEquals(2, tidslinje.inspektør.navDagTeller)
        assertEquals(2, tidslinje.inspektør.navHelgDagTeller)
        assertEquals(3, tidslinje.inspektør.arbeidsdagTeller)
    }

    @Test
    fun `ferie før arbeidsdag etter arbeidsgiverperioden teller ikke som opphold`() {
        (16.S + 6.S + 16.F + 1.A + 16.S).utbetalingslinjer()
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(16, tidslinje.inspektør.fridagTeller)
        assertEquals(15, tidslinje.inspektør.navDagTeller)
        assertEquals(7, tidslinje.inspektør.navHelgDagTeller)
        assertEquals(1, tidslinje.inspektør.arbeidsdagTeller)
    }

    @Test
    fun `ta hensyn til en andre arbeidsgiverperiode, arbeidsdageropphold`() {
        (16.S + 6.S + 16.A + 16.S).utbetalingslinjer()
        assertEquals(32, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(4, tidslinje.inspektør.navDagTeller)
        assertEquals(2, tidslinje.inspektør.navHelgDagTeller)
        assertEquals(16, tidslinje.inspektør.arbeidsdagTeller)
    }

    @Test
    fun `resetter arbeidsgiverperioden etter 16 arbeidsdager`() {
        (15.S + 16.A + 14.S).utbetalingslinjer()
        assertEquals(29, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(16, tidslinje.inspektør.arbeidsdagTeller)
    }

    @Test
    fun `siste dag i arbeidsgiverperioden faller på mandag`() {
        (1.S + 3.A + 4.S + 3.A + 11.S + 4.S).utbetalingslinjer()
        assertEquals(ArbeidsgiverperiodeDag::class, tidslinje[22.januar]::class)
        assertEquals(NavDag::class, tidslinje[23.januar]::class)
    }

    @Test
    fun `siste dag i arbeidsgiverperioden faller på søndag`() {
        (1.S + 3.A + 4.S + 2.A + 12.S + 4.S).utbetalingslinjer()
        assertEquals(ArbeidsgiverperiodeDag::class, tidslinje[21.januar]::class)
        assertEquals(NavDag::class, tidslinje[22.januar]::class)
    }

    @Test
    fun `siste dag i arbeidsgiverperioden faller på lørdag`() {
        (1.S + 3.A + 4.S + 1.A + 13.S + 4.S).utbetalingslinjer()
        assertEquals(ArbeidsgiverperiodeDag::class, tidslinje[20.januar]::class)
        assertEquals(NavHelgDag::class, tidslinje[21.januar]::class)
        assertEquals(NavDag::class, tidslinje[22.januar]::class)
    }

    @Test
    fun `ForeldetSykedag godkjennes som ArbeidsgverperiodeDag`() {
        (10.K + 6.S).utbetalingslinjer()
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
    }

    @Test
    fun `ForeldetSykedag blir ForeldetDag utenfor arbeidsgiverperioden`() {
        (20.K).utbetalingslinjer()
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(3, tidslinje.inspektør.foreldetDagTeller)
        assertEquals(1, tidslinje.inspektør.navHelgDagTeller)
    }

    @Test
    fun `feriedag før siste arbeidsgiverperiodedag`() {
        (15.U + 1.F + 1.U + 10.S).utbetalingslinjer()
        assertNotEquals(
            0.0,
            tidslinje.inspektør
                .navdager
                .first()
                .økonomi
                .medData { _, _, dekningsgrunnlag, _, _, _, _, _, _ -> dekningsgrunnlag }
        )
        assertEquals(18.januar, tidslinje.inspektør.navdager.first().dato)
    }

    @Test
    fun `feriedag før siste arbeidsgiverperiodedag med påfølgende helg`() {
        resetSeed(1.januar(2020))
        (10.U + 7.F + 14.S).utbetalingslinjer()
        assertEquals(31, tidslinje.inspektør.size)
        assertEquals(Fridag::class, tidslinje[17.januar(2020)]::class)
        assertEquals(NavHelgDag::class, tidslinje[18.januar(2020)]::class)
        assertEquals(NavHelgDag::class, tidslinje[19.januar(2020)]::class)
        assertEquals(NavDag::class, tidslinje[20.januar(2020)]::class)
    }

    @Test
    fun `Setter inntekt basert på inntektsdatoer`() {
        resetSeed(1.januar(2020))
        (14.S).utbetalingslinjer(
            inntektsopplysningPerSkjæringstidspunkt = mapOf(
                1.januar(2020) to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 1.januar(2020), hendelseId, 31000.månedlig)
            ),
            skjæringstidspunkter = listOf(1.januar(2020))
        )
        tidslinje.inspektør.navdager.assertDekningsgrunnlag(1.januar(2020) til 31.januar(2020), 31000.månedlig)
    }

    @Test
    fun `Setter inntekt basert på inntektsdato for siste del av arbeidsgiverperioden`() {
        resetSeed(1.januar(2020))
        (10.S + 10.A + 10.S).utbetalingslinjer(
            inntektsopplysningPerSkjæringstidspunkt = mapOf(
                21.januar(2020) to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 21.januar(2020), hendelseId, 30000.månedlig)
            ),
            skjæringstidspunkter = listOf(21.januar(2020))
        )

        tidslinje.inspektør.arbeidsgiverdager.assertDekningsgrunnlag(1.januar(2020) til 10.januar(2020), INGEN)
        tidslinje.inspektør.arbeidsdager.assertDekningsgrunnlag(11.januar(2020) til 20.januar(2020), INGEN)
        tidslinje.inspektør.arbeidsgiverdager.assertDekningsgrunnlag(21.januar(2020) til 26.januar(2020), 30000.månedlig)
        tidslinje.inspektør.navdager.assertDekningsgrunnlag(27.januar(2020) til 30.januar(2020), 30000.månedlig)
    }

    @Test
    fun `Setter inntekt basert på inntektsdatoer med gap`() {
        resetSeed(1.januar(2020))
        (20.S + 10.A + 10.S).utbetalingslinjer(
            inntektsopplysningPerSkjæringstidspunkt = mapOf(
                1.januar(2020) to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 1.januar(2020), hendelseId, 31000.månedlig),
                31.januar(2020) to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 31.januar(2020), hendelseId, 30000.månedlig)
            ),
            skjæringstidspunkter = listOf(1.januar(2020), 31.januar(2020))
        )

        tidslinje.inspektør.arbeidsgiverdager.assertDekningsgrunnlag(1.januar(2020) til 16.januar(2020), 31000.månedlig)
        tidslinje.inspektør.navdager.assertDekningsgrunnlag(17.januar(2020) til 20.januar(2020), 31000.månedlig)
        tidslinje.inspektør.arbeidsdager.assertDekningsgrunnlag(21.januar(2020) til 30.januar(2020), 31000.månedlig)
        tidslinje.inspektør.navdager.assertDekningsgrunnlag(31.januar(2020) til 9.februar(2020), 30000.månedlig)
    }

    @Test
    fun `Arbeidsgiverdager før frisk helg har ikke inntekt`() {
        resetSeed(1.januar(2020))
        (3.S + 2.A + 5.S + 2.A + 20.S).utbetalingslinjer(
            inntektsopplysningPerSkjæringstidspunkt = mapOf(
                13.januar(2020) to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 13.januar(2020), hendelseId, 30000.månedlig)
            ),
            skjæringstidspunkter = listOf(13.januar(2020))
        )

        tidslinje.inspektør.arbeidsgiverdager.assertDekningsgrunnlag(1.januar(2020) til 3.januar(2020), INGEN)
        tidslinje.inspektør.arbeidsdager.assertDekningsgrunnlag(4.januar(2020) til 5.januar(2020), INGEN)
        tidslinje.inspektør.arbeidsgiverdager.assertDekningsgrunnlag(6.januar(2020) til 10.januar(2020), INGEN)
        tidslinje.inspektør.arbeidsdager.assertDekningsgrunnlag(11.januar(2020) til 12.januar(2020), INGEN)
        tidslinje.inspektør.navdager.assertDekningsgrunnlag(13.januar(2020) til 1.februar(2020), 30000.månedlig)
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(4, tidslinje.inspektør.arbeidsdagTeller)
        assertEquals(3, tidslinje.inspektør.navHelgDagTeller)
        assertEquals(9, tidslinje.inspektør.navDagTeller)
    }

    @Test
    fun `Endrer ikke inntekt ved ferie`() {
        resetSeed(1.januar(2020))
        (5.S + 5.F + 15.S).utbetalingslinjer(
            inntektsopplysningPerSkjæringstidspunkt = mapOf(
                1.januar(2020) to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 1.januar(2020), hendelseId, 30000.månedlig)
            ),
            skjæringstidspunkter = listOf(1.januar(2020))
        )

        tidslinje.inspektør.arbeidsgiverdager.assertDekningsgrunnlag(1.januar(2020) til 5.januar(2020), 30000.månedlig)
        tidslinje.inspektør.fridager.assertDekningsgrunnlag(6.januar(2020) til 10.januar(2020), 30000.månedlig)
        tidslinje.inspektør.arbeidsgiverdager.assertDekningsgrunnlag(11.januar(2020) til 16.januar(2020), 30000.månedlig)
        tidslinje.inspektør.navdager.assertDekningsgrunnlag(17.januar(2020) til 25.januar(2020), 30000.månedlig)
    }

    @Test
    fun `Setter inntekt ved sykedag i helg etter opphold i arbeidsgiverperioden`() {
        resetSeed(1.januar(2020))
        (2.S + 1.A + 7.F + 17.S).utbetalingslinjer(
            inntektsopplysningPerSkjæringstidspunkt = mapOf(
                11.januar(2020) to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 11.januar(2020), hendelseId, 30000.månedlig)
            ),
            skjæringstidspunkter = listOf(11.januar(2020))
        )

        tidslinje.inspektør.arbeidsgiverdager.assertDekningsgrunnlag(1.januar(2020) til 2.januar(2020), INGEN)
        tidslinje.inspektør.arbeidsdager.assertDekningsgrunnlag(3.januar(2020) til 3.januar(2020), INGEN)
        tidslinje.inspektør.fridager.assertDekningsgrunnlag(4.januar(2020) til 10.januar(2020), INGEN)
        tidslinje.inspektør.arbeidsgiverdager.assertDekningsgrunnlag(11.januar(2020) til 24.januar(2020), 30000.månedlig)
        tidslinje.inspektør.navHelgdager.assertDekningsgrunnlag(25.januar(2020) til 26.januar(2020), 0.månedlig)
        tidslinje.inspektør.navdager.assertDekningsgrunnlag(27.januar(2020) til 27.januar(2020), 30000.månedlig)
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(1, tidslinje.inspektør.arbeidsdagTeller)
        assertEquals(7, tidslinje.inspektør.fridagTeller)
        assertEquals(2, tidslinje.inspektør.navHelgDagTeller)
        assertEquals(1, tidslinje.inspektør.navDagTeller)
    }

    @Test
    fun `Setter inntekt ved sykedag i helg etter opphold utenfor arbeidsgiverperioden`() {
        resetSeed(1.januar(2020))
        (20.S + 1.A + 3.F + 3.S).utbetalingslinjer(
            inntektsopplysningPerSkjæringstidspunkt = mapOf(
                1.januar(2020) to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 1.januar(2020), hendelseId, 30000.månedlig),
                25.januar(2020) to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 25.januar(2020), hendelseId, 31000.månedlig)
            ),
            skjæringstidspunkter = listOf(1.januar(2020), 25.januar(2020))
        )

        tidslinje.inspektør.arbeidsgiverdager.assertDekningsgrunnlag(1.januar(2020) til 16.januar(2020), 30000.månedlig)
        tidslinje.inspektør.navdager.assertDekningsgrunnlag(17.januar(2020) til 20.januar(2020), 30000.månedlig)
        tidslinje.inspektør.arbeidsdager.assertDekningsgrunnlag(21.januar(2020) til 21.januar(2020), 30000.månedlig)
        tidslinje.inspektør.fridager.assertDekningsgrunnlag(22.januar(2020) til 24.januar(2020), 30000.månedlig)
        tidslinje.inspektør.fridager.assertDekningsgrunnlag(25.januar(2020) til 26.januar(2020), 31000.månedlig)
        tidslinje.inspektør.navdager.assertDekningsgrunnlag(27.januar(2020) til 27.januar(2020), 31000.månedlig)
    }

    @Test
    fun `Setter inntekt ved sykedag i helg etter opphold rett etter arbeidsgiverperioden`() {
        resetSeed(1.januar(2020))
        (16.S + 2.A + 3.S).utbetalingslinjer(
            inntektsopplysningPerSkjæringstidspunkt = mapOf(
                19.januar(2020) to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 19.januar(2020), hendelseId, 30000.månedlig)
            ),
            skjæringstidspunkter = listOf(19.januar(2020))
        )
        tidslinje.inspektør.arbeidsgiverdager.assertDekningsgrunnlag(1.januar(2020) til 16.januar(2020), INGEN)
        tidslinje.inspektør.arbeidsdager.assertDekningsgrunnlag(17.januar(2020) til 18.januar(2020), INGEN)
        tidslinje.inspektør.navHelgdager.assertDekningsgrunnlag(19.januar(2020) til 19.januar(2020), INGEN)
        tidslinje.inspektør.navdager.assertDekningsgrunnlag(20.januar(2020) til 21.januar(2020), 30000.månedlig)
    }

    @Test
    fun `opphold i arbeidsgiverperioden`() {
        resetSeed(1.januar(2020))
        assertDoesNotThrow {
            (1.S + 11.A + 21.S).utbetalingslinjer(
                inntektsopplysningPerSkjæringstidspunkt = mapOf(
                    13.januar(2020) to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 13.januar(2020), hendelseId, 30000.månedlig)
                ),
                skjæringstidspunkter = listOf(13.januar(2020), 1.januar(2020))
            )
        }

        tidslinje.inspektør.arbeidsgiverdager.assertDekningsgrunnlag(1.januar(2020) til 1.januar(2020), INGEN)
        tidslinje.inspektør.arbeidsdager.assertDekningsgrunnlag(2.januar(2020) til 12.januar(2020), INGEN)
        tidslinje.inspektør.arbeidsgiverdager.assertDekningsgrunnlag(13.januar(2020) til 27.januar(2020), 30000.månedlig)
        tidslinje.inspektør.navdager.assertDekningsgrunnlag(28.januar(2020) til 31.januar(2020), 30000.månedlig)
        tidslinje.inspektør.navHelgdager.assertDekningsgrunnlag(1.februar(2020) til 2.februar(2020), INGEN)
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(11, tidslinje.inspektør.arbeidsdagTeller)
        assertEquals(4, tidslinje.inspektør.navDagTeller)
        assertEquals(2, tidslinje.inspektør.navHelgDagTeller)
    }

    @Test
    fun `opphold etter arbeidsgiverperiode i helg`() {
        resetSeed(3.januar(2020))
        assertDoesNotThrow {
            (16.U + 1.R + 2.S).utbetalingslinjer(
                inntektsopplysningPerSkjæringstidspunkt = mapOf(
                    20.januar(2020) to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 20.januar(2020), hendelseId, 30000.månedlig)
                ),
                skjæringstidspunkter = listOf(20.januar(2020), 3.januar(2020))
            )
        }

        tidslinje.inspektør.arbeidsgiverdager.assertDekningsgrunnlag(3.januar(2020) til 18.januar(2020), INGEN)
        tidslinje.inspektør.arbeidsdager.assertDekningsgrunnlag(19.januar(2020) til 19.januar(2020), INGEN)
        tidslinje.inspektør.navdager.assertDekningsgrunnlag(20.januar(2020) til 21.januar(2020), 30000.månedlig)
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(1, tidslinje.inspektør.arbeidsdagTeller)
        assertEquals(2, tidslinje.inspektør.navDagTeller)
    }

    @Test
    fun `opphold i arbeidsgiverperiode`() {
        resetSeed(4.januar(2020))
        assertDoesNotThrow {
            (16.U + 2.A + 2.S).utbetalingslinjer(
                inntektsopplysningPerSkjæringstidspunkt = mapOf(
                    22.januar(2020) to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 22.januar(2020), hendelseId, 30000.månedlig)
                ),
                skjæringstidspunkter = listOf(22.januar(2020), 4.januar(2020))
            )
        }

        tidslinje.inspektør.arbeidsgiverdager.assertDekningsgrunnlag(4.januar(2020) til 19.januar(2020), INGEN)
        tidslinje.inspektør.arbeidsdager.assertDekningsgrunnlag(20.januar(2020) til 21.januar(2020), INGEN)
        tidslinje.inspektør.navdager.assertDekningsgrunnlag(22.januar(2020) til 23.januar(2020), 30000.månedlig)
    }

    @Test
    fun `egenmeldingsdager med frisk helg gir opphold i arbeidsgiverperiode`() {
        (12.U + 2.R + 2.F + 2.U).utbetalingslinjer()
        assertEquals(ArbeidsgiverperiodeDag::class, tidslinje[17.januar]::class)
        assertEquals(ArbeidsgiverperiodeDag::class, tidslinje[18.januar]::class)
    }

    @Test
    fun `frisk helg gir opphold i arbeidsgiverperiode`() {
        (4.U + 8.S + 2.R + 2.F + 2.S).utbetalingslinjer()
        assertEquals(ArbeidsgiverperiodeDag::class, tidslinje[17.januar]::class)
        assertEquals(ArbeidsgiverperiodeDag::class, tidslinje[18.januar]::class)
    }

    @Test
    fun `oppdaterer inntekt etter frisk helg`() {
        (4.U + 1.A + 2.R + 12.U + 4.S).utbetalingslinjer(
            inntektsopplysningPerSkjæringstidspunkt = mapOf(
                8.januar to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 8.januar, hendelseId, 31000.månedlig)
            ),
            skjæringstidspunkter = listOf(8.januar)
        )
        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(3, tidslinje.inspektør.arbeidsdagTeller)
        assertEquals(2, tidslinje.inspektør.navDagTeller)
        assertEquals(2, tidslinje.inspektør.navHelgDagTeller)
        tidslinje.inspektør.navdager.assertDekningsgrunnlag(8.januar til 23.januar, 1431.daglig)
    }

    @Test
    fun `Sykedag etter langt opphold nullstiller tellere`() {
        (4.S + 1.A + 2.R + 5.A + 2.R + 5.A + 2.R + 5.A + 2.R + 5.A + 2.R + 2.S + 3.A + 2.R + 18.S).utbetalingslinjer(
            inntektsopplysningPerSkjæringstidspunkt = mapOf(
                12.februar to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 12.februar, hendelseId, 30000.månedlig),
                1.januar to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 1.januar, UUID.randomUUID(), 30000.månedlig)
            ),
            skjæringstidspunkter = listOf(1.januar, 5.februar, 12.februar)
        )

        assertEquals(20, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(36, tidslinje.inspektør.arbeidsdagTeller)
        assertEquals(4, tidslinje.inspektør.navDagTeller)
    }

    @Test
    fun `Syk helgedag etter langt opphold nullstiller tellere`() {
        (3.S + 2.A + 2.R + 5.A + 2.R + 5.A + 2.R + 5.A + 2.R + 5.A + 1.R + 1.H + 1.S + 4.A + 2.R + 18.S).utbetalingslinjer(
            inntektsopplysningPerSkjæringstidspunkt = mapOf(
                12.februar to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 12.februar, hendelseId, 30000.månedlig),
                1.januar to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 1.januar, UUID.randomUUID(), 30000.månedlig)
            ),
            skjæringstidspunkter = listOf(1.januar, 5.februar, 12.februar)
        )

        assertEquals(19, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(37, tidslinje.inspektør.arbeidsdagTeller)
        assertEquals(4, tidslinje.inspektør.navDagTeller)
    }

    @Test
    fun `Sykmelding som starter i helg etter oppholdsdager gir NavHelgDag i helgen`() {
        (16.U + 2.S + 1.A + 2.R + 5.A + 2.R + 5.A + 2.H + 1.S).utbetalingslinjer(
            inntektsopplysningPerSkjæringstidspunkt = mapOf(
                    1.januar to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 1.januar, hendelseId, 30000.månedlig),
                    3.februar to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 3.februar, hendelseId, 30000.månedlig)
            ),
            skjæringstidspunkter = listOf(1.januar, 3.februar)
        )

        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(3, tidslinje.inspektør.navDagTeller)
        assertEquals(15, tidslinje.inspektør.arbeidsdagTeller)
        assertEquals(2, tidslinje.inspektør.navHelgDagTeller)
    }

    private val inntektsopplysningPerSkjæringstidspunkt = mapOf(
        1.januar to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 1.januar, UUID.randomUUID(), 31000.månedlig),
        1.februar to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 1.februar, UUID.randomUUID(), 25000.månedlig),
        1.mars to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 1.mars, UUID.randomUUID(), 50000.månedlig),
    )


    @Test
    fun `Etter sykdom som slutter på fredag starter gap-telling i helgen - helg som friskHelgdag`() { // Fordi vi vet når hen gjenopptok arbeidet, og det var i helgen
        (16.U + 3.S + 2.R + 5.A + 2.R + 5.A + 2.R + 18.S).utbetalingslinjer(
            inntektsopplysningPerSkjæringstidspunkt = mapOf(
                    1.januar to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 1.januar, hendelseId, 30000.månedlig),
                    5.februar to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 5.februar, hendelseId, 30000.månedlig)
            ),
            skjæringstidspunkter = listOf(1.januar, 5.februar)
        )

        assertEquals(32, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(16, tidslinje.inspektør.arbeidsdagTeller)
        assertEquals(5, tidslinje.inspektør.navDagTeller)
    }

    @Test
    fun `Etter sykdom som slutter på fredag starter gap-telling mandagen etter (ikke i helgen) - helg som ukjent-dag`() { // Fordi vi ikke vet når hen gjenopptok arbeidet, men antar mandag
        (16.U + 3.S + 2.UK + 5.A + 2.R + 5.A + 2.R + 18.S).utbetalingslinjer(
            inntektsopplysningPerSkjæringstidspunkt = mapOf(
                    1.januar to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 1.januar, hendelseId, 30000.månedlig),
                    5.februar to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 5.februar, hendelseId, 30000.månedlig)
            ),
            skjæringstidspunkter = listOf(1.januar, 5.februar)
        )

        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(14, tidslinje.inspektør.arbeidsdagTeller)
        assertEquals(2, tidslinje.inspektør.fridagTeller)
        assertEquals(17, tidslinje.inspektør.navDagTeller)
    }

    @Test
    fun `Etter sykdom som slutter på fredag starter gap-telling mandagen etter (ikke i helgen) - helg som sykhelgdag`() {
        (16.U + 3.S + 2.H + 5.A + 2.R + 5.A + 2.R + 18.S).utbetalingslinjer(
            inntektsopplysningPerSkjæringstidspunkt = mapOf(
                1.januar to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 1.januar, hendelseId, 30000.månedlig),
                5.februar to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 5.februar, hendelseId, 30000.månedlig)
            ),
            skjæringstidspunkter = listOf(1.januar, 5.februar)
        )

        assertEquals(16, tidslinje.inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(14, tidslinje.inspektør.arbeidsdagTeller)
        assertEquals(17, tidslinje.inspektør.navDagTeller)
        assertEquals(6, tidslinje.inspektør.navHelgDagTeller)
    }

    private fun assertInntekter(dekningsgrunnlaget: Int? = null, aktuelleDagsinntekten: Int? = null) {
        tidslinje.inspektør.navdager.forEach { navDag ->
            navDag.økonomi.medAvrundetData { _, _, dekningsgrunnlag, aktuellDagsinntekt, _, _, _ ->
                dekningsgrunnlaget?.let { assertEquals(it, dekningsgrunnlag) }
                aktuelleDagsinntekten?.let { assertEquals(it, aktuellDagsinntekt) }
            }
        }
    }

    private fun List<Utbetalingsdag>.assertDekningsgrunnlag(periode: Periode, dekningsgrunnlaget: Inntekt?) =
        filter { it.dato in periode }
            .forEach { utbetalingsdag ->
                val daglig = dekningsgrunnlaget?.reflection { _, _, _, daglig -> daglig }
                utbetalingsdag.økonomi.medAvrundetData { _, _, dekningsgrunnlag, _, _, _, _ ->
                    assertEquals(daglig, dekningsgrunnlag)
                }
            }

    private fun Sykdomstidslinje.utbetalingslinjer(
        inntektsopplysningPerSkjæringstidspunkt: Map<LocalDate, Inntektshistorikk.Inntektsopplysning?> = this@UtbetalingstidslinjeBuilderTest.inntektsopplysningPerSkjæringstidspunkt,
        skjæringstidspunkter: List<LocalDate> = listOf(1.januar, 1.februar, 1.mars),
        strategi: Forlengelsestrategi = Forlengelsestrategi.Ingen
    ) {
        tidslinje = UtbetalingstidslinjeBuilder(
            skjæringstidspunkter = skjæringstidspunkter,
            inntektPerSkjæringstidspunkt = inntektsopplysningPerSkjæringstidspunkt
        ).apply { forlengelsestrategi(strategi) }.result(this, periode()!!)
        verifiserRekkefølge(tidslinje)
    }

    private fun verifiserRekkefølge(tidslinje: Utbetalingstidslinje) {
        tidslinje.zipWithNext { forrige, neste ->
            assertTrue(neste.dato > forrige.dato) { "Rekkefølgen er ikke riktig: ${neste.dato} skal være nyere enn ${forrige.dato}" }
        }
    }
}
