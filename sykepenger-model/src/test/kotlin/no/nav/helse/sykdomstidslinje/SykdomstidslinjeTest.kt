package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.testhelpers.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class SykdomstidslinjeTest {

    @Test
    internal fun `tom tidslinje er gyldig`() {
        assertEquals(0, Sykdomstidslinje().count())
    }

    @Test
    internal fun `dager mellom to perioder blir UkjentDag`() {
        val tidslinje1 = Sykdomstidslinje.sykedager(
            1.mandag, 1.onsdag, 100.0, TestEvent.søknad
        )
        val tidslinje2 = Sykdomstidslinje.sykedager(
            2.onsdag, 2.fredag, 100.0, TestEvent.søknad
        )
        val tidslinje = tidslinje1.merge(tidslinje2, konfliktsky)
        assertEquals(" SSS???? ??SSS", tidslinje.toShortString())
    }

    @Test
    internal fun `to sykeperioder med mellomrom får riktig slutt og start dato`() {
        val tidslinje1 = Sykdomstidslinje.sykedager(1.mandag, 1.tirsdag, 100.0, TestEvent.søknad)
        val tidslinje2 = Sykdomstidslinje.sykedager(1.fredag, 2.mandag, 100.0, TestEvent.søknad)

        val tidslinje = tidslinje2 + tidslinje1

        assertEquals(1.mandag, tidslinje.periode()?.start)
        assertEquals(2.mandag, tidslinje.periode()?.endInclusive)
        assertEquals(8, tidslinje.count())
        assertEquals(" SS??SHH S", tidslinje.toShortString())
    }

    @Test
    internal fun `tidslinje med problemdag er utenfor omfang`() {
        val tidslinje = Sykdomstidslinje.problemdager(1.mandag, 1.mandag, TestEvent.testkilde, "Dette er en problemdag")
        val aktivitetslogg = Aktivitetslogg()
        assertFalse(tidslinje.valider(aktivitetslogg))
        assertTrue(aktivitetslogg.hasErrors())
    }

    @Test
    internal fun `overskriving av tidslinje`() {
        val tidslinje1 = (Sykdomstidslinje.problemdager(1.mandag, 1.onsdag, TestEvent.sykmelding, "Yes")
            + Sykdomstidslinje.sykedager(1.torsdag, 1.fredag, 100.0, TestEvent.sykmelding))
        val tidslinje2 = (Sykdomstidslinje.arbeidsdager(1.mandag, 1.onsdag, TestEvent.testkilde))

        val merged = tidslinje1.merge(tidslinje2, Dag.replace)
        assertEquals(" AAASS", merged.toShortString())
    }

    @Test
    fun `sykeperioder`() {
        val tidslinje = Sykdomstidslinje.sykedager(1.mandag, 1.tirsdag, 100.0, TestEvent.testkilde) +
            Sykdomstidslinje.arbeidsdager(1.onsdag, 1.onsdag, TestEvent.testkilde) +
            Sykdomstidslinje.sykedager(1.torsdag, 1.fredag, 100.0, TestEvent.testkilde) +
            Sykdomstidslinje.arbeidsdager(Periode(1.lørdag, 1.søndag), TestEvent.testkilde) +
            Sykdomstidslinje.sykedager(2.mandag, 2.fredag, 100.0, TestEvent.testkilde)
        val aktivitetslogg = Aktivitetslogg()
        assertTrue(tidslinje.valider(aktivitetslogg))
        assertEquals(listOf(Periode(1.mandag, 1.tirsdag), Periode(1.torsdag, 1.fredag), Periode(2.mandag, 2.fredag)), tidslinje.sykeperioder())
    }


    private val konfliktsky = { venstre: Dag, høyre: Dag ->
        when {
            venstre is Dag.UkjentDag -> høyre
            høyre is Dag.UkjentDag -> venstre
            else -> venstre.problem(høyre)
        }
    }
}
