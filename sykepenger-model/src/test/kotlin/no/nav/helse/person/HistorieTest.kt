package no.nav.helse.person

import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.Utbetalingshistorikk.Periode
import no.nav.helse.hendelser.Utbetalingshistorikk.Periode.*
import no.nav.helse.person.Periodetype.*
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse.Hendelseskilde
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse.Hendelseskilde.Companion.INGEN
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

internal class HistorieTest {

    private companion object {
        private const val FNR = "12345678910"
        private const val AKTØRID = "1234567891011"
        private const val AG1 = "1234"
        private const val AG2 = "2345"
    }

    private lateinit var historie: Historie

    @BeforeEach
    fun beforeEach() {
        historie = Historie()
    }

    @Test
    fun `sykedag på fredag og feriedag på fredag`() {
        historie(
            RefusjonTilArbeidsgiver(2.januar, 12.januar, 1000, 100, AG1),
            Ferie(15.januar, 19.januar),
            RefusjonTilArbeidsgiver(22.januar, 31.januar, 1000, 100, AG1),
        )

        assertEquals(null, historie.skjæringstidspunkt(1.januar))
        assertEquals(2.januar, historie.skjæringstidspunkt(21.januar))
        assertEquals(2.januar, historie.skjæringstidspunkt(31.januar))
        assertTrue(historie.forlengerInfotrygd(AG1, no.nav.helse.hendelser.Periode(1.februar, 28.februar)))
        assertFalse(historie.forlengerInfotrygd(AG2, no.nav.helse.hendelser.Periode(22.januar, 31.januar)))
    }

    @Test
    fun `skjæringstidspunkt med flere arbeidsgivere`() {
        historie(
            RefusjonTilArbeidsgiver(2.januar, 12.januar, 1000, 100, AG1),
            Ferie(15.januar, 19.januar),
            RefusjonTilArbeidsgiver(22.januar, 31.januar, 1000, 100, AG2),
        )

        assertEquals(null, historie.skjæringstidspunkt(1.januar))
        assertEquals(2.januar, historie.skjæringstidspunkt(21.januar))
        assertEquals(2.januar, historie.skjæringstidspunkt(31.januar))
        assertFalse(historie.forlengerInfotrygd(AG2, no.nav.helse.hendelser.Periode(16.januar, 31.januar)))
        assertTrue(historie.forlengerInfotrygd(AG2, no.nav.helse.hendelser.Periode(1.februar, 28.februar)))
    }

    @Test
    fun `innledende ferie som avsluttes på fredag`() {
        historie(
            Ferie(1.januar, 5.januar),
            RefusjonTilArbeidsgiver(8.januar, 12.januar, 1000, 100, AG1)
        )

        assertEquals(8.januar, historie.skjæringstidspunkt(12.januar))
    }

    @Test
    fun `fri i helg mappes til ukjentdag`() {
        val tidslinje = tidslinjeOf(4.NAV, 3.FRI, 5.NAV, 2.HELG)
        Historie.Historikkbøtte.konverter(tidslinje).also {
            assertTrue(it[1.januar] is Dag.Sykedag)
            assertTrue(it[5.januar] is Dag.UkjentDag)
            assertTrue(it[6.januar] is Dag.UkjentDag)
            assertTrue(it[8.januar] is Dag.Sykedag)
            assertTrue(it[13.januar] is Dag.SykHelgedag)
        }
    }

    @Test
    fun `mapper utbetalingstidslinje til sykdomstidslinje`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV)
        Historie.Historikkbøtte.konverter(tidslinje).also {
            assertTrue(it[1.januar] is Dag.Sykedag)
            assertTrue(it[6.januar] is Dag.SykHelgedag)
            assertTrue(it[31.januar] is Dag.Sykedag)
        }
    }

    @Test
    fun `utbetalingstidslinje for orgnr`() {
        val bøtte = Historie.Historikkbøtte()
        bøtte.add(tidslinje = tidslinjeOf(7.FRI))
        bøtte.add(AG1, tidslinjeOf(5.NAV, 2.HELG, startDato = 8.januar))
        bøtte.add(AG2, tidslinjeOf(5.NAV, 2.HELG, startDato = 15.januar))
        bøtte.utbetalingstidslinje(AG1).also {
            assertEquals(1.januar, it.førsteDato())
            assertEquals(14.januar, it.sisteDato())
            assertTrue(it[1.januar] is Fridag)
            assertTrue(it[8.januar] is NavDag)
        }
        bøtte.utbetalingstidslinje(AG2).also {
            assertEquals(1.januar, it.førsteDato())
            assertEquals(21.januar, it.sisteDato())
            assertTrue(it[1.januar] is Fridag)
            assertTrue(it[8.januar] is UkjentDag)
            assertTrue(it[15.januar] is NavDag)
            assertTrue(it[21.januar] is NavHelgDag)
        }
    }

    @Test
    fun `infotrygd - spleis`() {
        historie(refusjon(1.januar, 31.januar))
        historie.add(AG1, sykedager(1.februar, 28.februar))
        assertEquals(1.januar, historie.skjæringstidspunkt(31.januar))
        assertEquals(1.januar, historie.skjæringstidspunkt(28.februar))
        assertEquals(1.januar, historie.skjæringstidspunkt(28.februar))
        assertSkjæringstidspunkter(kuttdato = 28.februar, 1.januar)
        assertTrue(historie.forlengerInfotrygd(AG1, no.nav.helse.hendelser.Periode(1.februar, 28.februar)))
        assertEquals(OVERGANG_FRA_IT, historie.periodetype(AG1, no.nav.helse.hendelser.Periode(1.februar, 28.februar)))
        assertFalse(historie.erPingPong(AG1, no.nav.helse.hendelser.Periode(1.februar, 28.februar)))
    }

    @Test
    fun `infotrygd - gap - spleis - gap - infotrygd - spleis - spleis`() {
        historie(refusjon(1.januar, 31.januar), refusjon(9.april, 30.april))
        historie.add(AG1, sykedager(1.mars, 30.mars))
        historie.add(AG1, sykedager(1.mai, 31.mai))
        historie.add(AG1, sykedager(1.juni, 30.juni))

        assertFalse(historie.forlengerInfotrygd(AG1, no.nav.helse.hendelser.Periode(1.mars, 31.mars)))
        assertFalse(historie.erPingPong(AG1, no.nav.helse.hendelser.Periode(1.mars, 31.mars)))
        assertEquals(FØRSTEGANGSBEHANDLING, historie.periodetype(AG1, no.nav.helse.hendelser.Periode(1.mars, 31.mars)))

        assertTrue(historie.forlengerInfotrygd(AG1, no.nav.helse.hendelser.Periode(1.mai, 31.mai)))
        assertEquals(OVERGANG_FRA_IT, historie.periodetype(AG1, no.nav.helse.hendelser.Periode(1.mai, 31.mai)))
        assertFalse(historie.erPingPong(AG1, no.nav.helse.hendelser.Periode(1.mai, 31.mai)))

        assertTrue(historie.forlengerInfotrygd(AG1, no.nav.helse.hendelser.Periode(1.juni, 30.juni)))
        assertEquals(INFOTRYGDFORLENGELSE, historie.periodetype(AG1, no.nav.helse.hendelser.Periode(1.juni, 30.juni)))
        assertFalse(historie.erPingPong(AG1, no.nav.helse.hendelser.Periode(1.juni, 30.juni)))
    }

    @Test
    fun `infotrygd - spleis - spleis`() {
        historie(refusjon(1.januar, 31.januar))
        historie.add(AG1, sykedager(1.februar, 28.februar))
        historie.add(AG1, sykedager(1.mars, 31.mars))

        assertTrue(historie.forlengerInfotrygd(AG1, no.nav.helse.hendelser.Periode(1.februar, 28.februar)))
        assertEquals(OVERGANG_FRA_IT, historie.periodetype(AG1, no.nav.helse.hendelser.Periode(1.februar, 28.februar)))
        assertFalse(historie.erPingPong(AG1, no.nav.helse.hendelser.Periode(1.februar, 28.februar)))

        assertEquals(INFOTRYGDFORLENGELSE, historie.periodetype(AG1, no.nav.helse.hendelser.Periode(1.mars, 31.mars)))
        assertFalse(historie.erPingPong(AG1, no.nav.helse.hendelser.Periode(1.februar, 28.februar)))
    }

    @Test
    fun `infotrygd - spleis - infotrygd - spleis`() {
        historie(refusjon(1.januar, 31.januar), refusjon(1.mars, 31.mars))
        historie.add(AG1, navdager(1.februar, 28.februar))
        historie.add(AG1, sykedager(1.april, 30.april))
        assertEquals(1.januar, historie.skjæringstidspunkt(31.januar))
        assertEquals(1.januar, historie.skjæringstidspunkt(28.februar))
        assertEquals(1.januar, historie.skjæringstidspunkt(31.mars))

        assertTrue(historie.forlengerInfotrygd(AG1, no.nav.helse.hendelser.Periode(1.februar, 28.februar)))
        assertEquals(OVERGANG_FRA_IT, historie.periodetype(AG1, no.nav.helse.hendelser.Periode(1.februar, 28.februar)))
        assertFalse(historie.erPingPong(AG1, no.nav.helse.hendelser.Periode(1.februar, 28.februar)))

        assertTrue(historie.forlengerInfotrygd(AG1, no.nav.helse.hendelser.Periode(1.april, 30.april)))
        assertEquals(INFOTRYGDFORLENGELSE, historie.periodetype(AG1, no.nav.helse.hendelser.Periode(1.april, 30.april)))
        assertTrue(historie.erPingPong(AG1, no.nav.helse.hendelser.Periode(1.april, 30.april)))
    }

    @Test
    fun `er ikke ping-pong eldre enn 6 mnd tilbake i infotrygdforlengelser`() {
        historie(refusjon(1.januar, 31.januar), refusjon(1.mars, 31.mars))
        historie.add(AG1, navdager(1.februar, 28.februar))
        historie.add(AG1, navdager(1.april, 30.april))
        historie.add(AG1, navdager(1.mai, 31.mai))
        historie.add(AG1, navdager(1.juni, 30.juni))
        historie.add(AG1, navdager(1.juli, 31.juli))
        historie.add(AG1, navdager(1.august, 31.august))
        historie.add(AG1, navdager(1.september, 30.september))
        assertFalse(historie.erPingPong(AG1, no.nav.helse.hendelser.Periode(1.februar, 28.februar)))
        assertTrue(historie.erPingPong(AG1, no.nav.helse.hendelser.Periode(1.april, 30.april)))
        assertFalse(historie.erPingPong(AG1, no.nav.helse.hendelser.Periode(1.september, 30.september)))
    }

    @Test
    fun `er ikke ping-pong eldre enn 6 mnd tilbake i spleisforlengelser`() {
        historie(refusjon(1.februar, 28.februar), refusjon(1.april, 30.april))
        historie.add(AG1, navdager(1.januar, 31.januar))
        historie.add(AG1, navdager(1.mars, 31.mars))
        historie.add(AG1, navdager(1.mai, 31.mai))
        historie.add(AG1, navdager(1.juni, 30.juni))
        historie.add(AG1, navdager(1.juli, 31.juli))
        historie.add(AG1, navdager(1.august, 31.august))
        historie.add(AG1, navdager(1.september, 30.september))
        historie.add(AG1, navdager(1.oktober, 31.oktober))
        assertTrue(historie.erPingPong(AG1, no.nav.helse.hendelser.Periode(1.mars, 31.mars)))
        assertTrue(historie.erPingPong(AG1, no.nav.helse.hendelser.Periode(1.mai, 31.mai)))
        assertFalse(historie.erPingPong(AG1, no.nav.helse.hendelser.Periode(1.oktober, 31.oktober)))
    }

    @Test
    fun `spleis - infotrygd - spleis`() {
        historie(refusjon(1.februar, 28.februar))
        historie.add(AG1, navdager(1.januar, 31.januar))
        historie.add(AG1, sykedager(1.mars, 31.mars))
        assertEquals(1.januar, historie.skjæringstidspunkt(31.januar))
        assertEquals(1.januar, historie.skjæringstidspunkt(28.februar))
        assertEquals(1.januar, historie.skjæringstidspunkt(31.mars))

        assertEquals(FØRSTEGANGSBEHANDLING, historie.periodetype(AG1, no.nav.helse.hendelser.Periode(1.januar, 31.januar)))

        assertFalse(historie.forlengerInfotrygd(AG1, no.nav.helse.hendelser.Periode(1.mars, 31.mars)))
        assertEquals(FORLENGELSE, historie.periodetype(AG1, no.nav.helse.hendelser.Periode(1.mars, 31.mars)))
        assertTrue(historie.erPingPong(AG1, no.nav.helse.hendelser.Periode(1.mars, 31.mars)))
    }

    @Test
    fun `infotrygd - gap - spleis`() {
        historie(refusjon(1.februar, 27.februar))
        historie.add(AG1, sykedager(1.mars, 31.mars))
        assertEquals(1.februar, historie.skjæringstidspunkt(28.februar))
        assertEquals(1.mars, historie.skjæringstidspunkt(31.mars))
        assertSkjæringstidspunkter(kuttdato = 31.mars, 1.mars, 1.februar)
        assertSkjæringstidspunkter(kuttdato = 28.februar, 1.februar)
        assertEquals(FØRSTEGANGSBEHANDLING, historie.periodetype(AG1, no.nav.helse.hendelser.Periode(1.mars, 31.mars)))
        assertFalse(historie.forlengerInfotrygd(AG1, no.nav.helse.hendelser.Periode(1.mars, 31.mars)))
        assertFalse(historie.erPingPong(AG1, no.nav.helse.hendelser.Periode(1.mars, 31.mars)))
    }

    @Test
    fun `spleis - gap - infotrygd - spleis`() {
        historie(refusjon(1.februar, 28.februar))
        historie.add(AG1, navdager(1.januar, 30.januar))
        historie.add(AG1, sykedager(1.mars, 31.mars))
        assertEquals(1.januar, historie.skjæringstidspunkt(31.januar))
        assertEquals(1.februar, historie.skjæringstidspunkt(28.februar))
        assertEquals(1.februar, historie.skjæringstidspunkt(31.mars))
        assertSkjæringstidspunkter(kuttdato = 31.mars, 1.februar, 1.januar)
        assertTrue(historie.forlengerInfotrygd(AG1, no.nav.helse.hendelser.Periode(1.mars, 31.mars)))
        assertEquals(OVERGANG_FRA_IT, historie.periodetype(AG1, no.nav.helse.hendelser.Periode(1.mars, 31.mars)))
    }

    @Test
    fun `spleis - infotrygd - gap - spleis`() {
        historie(refusjon(1.februar, 28.februar))
        historie.add(AG1, navdager(1.januar, 31.januar))
        historie.add(AG1, sykedager(2.mars, 31.mars))
        assertEquals(1.januar, historie.skjæringstidspunkt(31.januar))
        assertEquals(1.januar, historie.skjæringstidspunkt(1.mars))
        assertEquals(2.mars, historie.skjæringstidspunkt(31.mars))
        assertFalse(historie.forlengerInfotrygd(AG1, no.nav.helse.hendelser.Periode(2.mars, 31.mars)))
    }

    @Test
    fun `ubetalt spleis - ubetalt spleis`() {
        historie.add(AG1, sykedager(1.februar, 28.februar))
        historie.add(AG1, sykedager(1.mars, 31.mars))
        assertEquals(FØRSTEGANGSBEHANDLING, historie.periodetype(AG1, no.nav.helse.hendelser.Periode(1.januar, 28.februar)))
        assertFalse(historie.forlengerInfotrygd(AG1, no.nav.helse.hendelser.Periode(1.mars, 31.mars)))
        assertEquals(FORLENGELSE, historie.periodetype(AG1, no.nav.helse.hendelser.Periode(1.mars, 31.mars)))
    }

    @Test
    fun `spleis - ubetalt spleis - ubetalt spleis`() {
        historie.add(AG1, navdager(1.januar, 31.januar))
        historie.add(AG1, sykedager(1.februar, 28.februar))
        historie.add(AG1, sykedager(1.mars, 31.mars))
        assertEquals(FØRSTEGANGSBEHANDLING, historie.periodetype(AG1, no.nav.helse.hendelser.Periode(1.januar, 31.januar)))
        assertFalse(historie.forlengerInfotrygd(AG1, no.nav.helse.hendelser.Periode(1.februar, 28.februar)))
        assertEquals(FORLENGELSE, historie.periodetype(AG1, no.nav.helse.hendelser.Periode(1.februar, 28.februar)))
        assertFalse(historie.forlengerInfotrygd(AG1, no.nav.helse.hendelser.Periode(1.mars, 31.mars)))
        assertEquals(FORLENGELSE, historie.periodetype(AG1, no.nav.helse.hendelser.Periode(1.mars, 31.mars)))
    }

    @Test
    fun `infotrygd - gap - infotrygdTilBbruker - spleis`() {
        historie(refusjon(1.januar, 31.januar), bruker(2.februar, 28.februar))
        historie.add(AG1, sykedager(1.mars, 31.mars))
        assertEquals(1.januar, historie.skjæringstidspunkt(1.februar))
        assertEquals(2.februar, historie.skjæringstidspunkt(28.februar))
        assertEquals(2.februar, historie.skjæringstidspunkt(31.mars))
        assertTrue(historie.forlengerInfotrygd(AG1, no.nav.helse.hendelser.Periode(1.mars, 31.mars)))
    }

    @Test
    fun `infotrygdferie - infotrygd - spleis`() {
        historie(ferie(1.januar, 31.januar), bruker(1.februar, 28.februar))
        historie.add(AG1, sykedager(1.mars, 31.mars))
        assertEquals(1.februar, historie.skjæringstidspunkt(1.februar))
        assertEquals(1.februar, historie.skjæringstidspunkt(28.februar))
        assertEquals(1.februar, historie.skjæringstidspunkt(31.mars))
        assertTrue(historie.forlengerInfotrygd(AG1, no.nav.helse.hendelser.Periode(1.mars, 31.mars)))
    }

    @Test
    fun `infotrygd - infotrygdferie - spleis`() {
        historie(refusjon(1.januar, 31.januar), ferie(1.februar, 10.februar))
        historie.add(AG1, sykedager(11.februar, 28.februar))
        assertEquals(1.januar, historie.skjæringstidspunkt(1.februar))
        assertEquals(1.januar, historie.skjæringstidspunkt(10.februar))
        assertEquals(1.januar, historie.skjæringstidspunkt(28.februar))
        assertTrue(historie.forlengerInfotrygd(AG1, no.nav.helse.hendelser.Periode(11.februar, 28.februar)))
    }

    @Test
    fun `infotrygd AG1 - infotrygdferie AG1 - spleis AG2`() {
        historie(refusjon(1.januar, 31.januar), ferie(1.februar, 10.februar))
        historie.add(AG2, sykedager(11.februar, 28.februar))
        assertFalse(historie.forlengerInfotrygd(AG2, no.nav.helse.hendelser.Periode(11.februar, 28.februar)))
    }

    private fun refusjon(fom: LocalDate, tom: LocalDate, dagsats: Int = 1000, grad: Int = 100, orgnr: String = AG1) =
        RefusjonTilArbeidsgiver(fom, tom, dagsats, grad, orgnr)

    private fun bruker(fom: LocalDate, tom: LocalDate, dagsats: Int = 1000, grad: Int = 100, orgnr: String = AG1) =
        Utbetaling(fom, tom, dagsats, grad, orgnr)

    private fun ferie(fom: LocalDate, tom: LocalDate) =
        Ferie(fom, tom)

    private fun navdager(fom: LocalDate, tom: LocalDate) =
        tidslinjeOf((ChronoUnit.DAYS.between(fom, tom).toInt() + 1).NAV, startDato = fom)

    private fun sykedager(fom: LocalDate, tom: LocalDate, grad: Int = 100, kilde: Hendelseskilde = INGEN) = Sykdomstidslinje.sykedager(fom, tom, grad, kilde)

    private fun historie(vararg perioder: Periode) {
        historie = Historie(
            Utbetalingshistorikk(
                UUID.randomUUID(),
                AKTØRID,
                FNR,
                "ET ORGNR",
                UUID.randomUUID().toString(),
                perioder.toList(),
                emptyList()
            )
        )
    }

    private fun assertSkjæringstidspunkter(kuttdato: LocalDate, vararg datoer: LocalDate) {
        assertEquals(datoer.toList(), historie.skjæringstidspunkter(kuttdato))
    }
}
