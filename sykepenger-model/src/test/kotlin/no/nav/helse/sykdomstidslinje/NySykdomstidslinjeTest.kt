package no.nav.helse.sykdomstidslinje

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.testhelpers.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class NySykdomstidslinjeTest {

    @Test internal fun `tom tidslinje er gyldig`() {
        assertEquals(0, NySykdomstidslinje().count())
    }

    @Test internal fun `dager mellom to perioder blir UkjentDag`() {
        val tidslinje1 = NySykdomstidslinje.sykedager(
            1.mandag, 1.onsdag, 100.0, TestEvent.søknad)
        val tidslinje2 = NySykdomstidslinje.sykedager(
            2.onsdag, 2.fredag, 100.0, TestEvent.søknad)
        val tidslinje = tidslinje1.merge(tidslinje2, konfliktsky)
        assertEquals(" SSS???? ??SSS", tidslinje.toShortString())
    }

    @Test
    internal fun `to sykeperioder med mellomrom får riktig slutt og start dato`() {
        val tidslinje1 = NySykdomstidslinje.sykedager(1.mandag, 1.tirsdag, 100.0, TestEvent.søknad)
        val tidslinje2 = NySykdomstidslinje.sykedager(1.fredag, 2.mandag, 100.0, TestEvent.søknad)

        val tidslinje = tidslinje2 + tidslinje1

        assertEquals(1.mandag, tidslinje.periode()?.start)
        assertEquals(2.mandag, tidslinje.periode()?.endInclusive)
        assertEquals(8, tidslinje.count())
        assertEquals(" SS??SHH S", tidslinje.toShortString())
    }

    @Test
    internal fun `tidslinje med problemdag er utenfor omfang`() {
        val tidslinje = NySykdomstidslinje.problemdager(1.mandag, 1.mandag, TestEvent.testkilde, "Dette er en problemdag")
        val aktivitetslogg = Aktivitetslogg()
        assertFalse(tidslinje.valider(aktivitetslogg))
        assertTrue(aktivitetslogg.hasErrors())
    }


    private val konfliktsky = {venstre: NyDag, høyre: NyDag -> when {
                venstre is NyDag.NyUkjentDag -> høyre
                høyre is NyDag.NyUkjentDag -> venstre
                else -> venstre.problem(høyre)
            }
        }
}
