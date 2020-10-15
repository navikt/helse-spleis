package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.testhelpers.*
import no.nav.helse.tournament.Dagturnering
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class SykdomstidslinjeTest {

    @Test
    fun `tom tidslinje er gyldig`() {
        assertEquals(0, Sykdomstidslinje().count())
    }

    @Test
    fun `dager mellom to perioder blir UkjentDag`() {
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
    fun `to sykeperioder med mellomrom får riktig slutt og start dato`() {
        val tidslinje1 = Sykdomstidslinje.sykedager(1.mandag, 1.tirsdag, 100.0, TestEvent.søknad)
        val tidslinje2 = Sykdomstidslinje.sykedager(1.fredag, 2.mandag, 100.0, TestEvent.søknad)

        val tidslinje = tidslinje2 + tidslinje1

        assertEquals(1.mandag, tidslinje.periode()?.start)
        assertEquals(2.mandag, tidslinje.periode()?.endInclusive)
        assertEquals(8, tidslinje.count())
        assertEquals(" SS??SHH S", tidslinje.toShortString())
    }

    @Test
    fun `tidslinje med problemdag er utenfor omfang`() {
        val tidslinje = Sykdomstidslinje.problemdager(1.mandag, 1.mandag, TestEvent.testkilde, "Dette er en problemdag")
        val aktivitetslogg = Aktivitetslogg()
        assertFalse(tidslinje.valider(aktivitetslogg))
        assertTrue(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `kutter frem til og med`() {
        val tidslinje = Sykdomstidslinje.sykedager(1.februar, 1.mars, 100, TestEvent.testkilde)
        assertEquals(14.februar, tidslinje.fremTilOgMed(14.februar).sisteDag())
        assertEquals(1.mars, tidslinje.fremTilOgMed(1.mars).sisteDag())
        assertEquals(1.mars, tidslinje.fremTilOgMed(1.april).sisteDag())
        assertEquals(1.februar, tidslinje.fremTilOgMed(1.februar).sisteDag())
        assertEquals(1.februar, Sykdomstidslinje.sykedager(1.februar, 1.februar, 100, TestEvent.testkilde)
            .fremTilOgMed(1.februar)
            .sisteDag())
        assertEquals(0, tidslinje.fremTilOgMed(tidslinje.førsteDag().minusDays(1)).length())
        assertEquals(0, Sykdomstidslinje().fremTilOgMed(1.januar).length())
    }

    @Test
    fun `overskriving av tidslinje`() {
        val tidslinje1 = (Sykdomstidslinje.problemdager(1.mandag, 1.onsdag, TestEvent.sykmelding, "Yes")
            + Sykdomstidslinje.sykedager(1.torsdag, 1.fredag, 100.0, TestEvent.sykmelding))
        val tidslinje2 = (Sykdomstidslinje.arbeidsdager(1.mandag, 1.onsdag, TestEvent.testkilde))

        val merged = tidslinje1.merge(tidslinje2, Dag.replace)
        assertEquals(" AAASS", merged.toShortString())
    }

    @Test
    fun annullering() {
        val dagturnering = Dagturnering("/dagturnering.csv")
        val tidslinje = Sykdomstidslinje.sykedager(1.mandag, 1.tirsdag, 100.0, TestEvent.søknad) +
            Sykdomstidslinje.arbeidsdager(1.onsdag, 1.onsdag, TestEvent.søknad) +
            Sykdomstidslinje.sykedager(1.torsdag, 1.fredag, 100.0, TestEvent.søknad) +
            Sykdomstidslinje.arbeidsdager(Periode(1.lørdag, 1.søndag), TestEvent.søknad) +
            Sykdomstidslinje.sykedager(2.mandag, 2.fredag, 100.0, TestEvent.søknad)
        val actual = tidslinje.merge(Sykdomstidslinje.annullerteDager(1.onsdag til 2.fredag, TestEvent.søknad), dagturnering::beste)
        val aktivitetslogg = Aktivitetslogg()
        assertTrue(actual.valider(aktivitetslogg))
        assertTrue(
            actual.subset(1.mandag til 1.tirsdag).all { it is Dag.Sykedag }
        )
        assertTrue(
            actual.subset(1.onsdag til 2.fredag).all { it is Dag.AnnullertDag }
        )
    }

    private val konfliktsky = { venstre: Dag, høyre: Dag ->
        when {
            venstre is Dag.UkjentDag -> høyre
            høyre is Dag.UkjentDag -> venstre
            else -> venstre.problem(høyre)
        }
    }
}
