package no.nav.helse.tournament


import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.Testhendelse
import no.nav.helse.sykdomstidslinje.dag.Arbeidsdag
import no.nav.helse.sykdomstidslinje.dag.Dag
import no.nav.helse.sykdomstidslinje.dag.Sykedag
import no.nav.helse.testhelpers.Uke
import no.nav.helse.testhelpers.get
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DagTurneringTest {

    @Test
    fun `Turneringen skal inneholde riktige strategier basert på en csv-fil`() {
        val turnering = DagTurnering("/microturnering.csv")
        assertEquals(LatestOrRow::class, turnering.strategies.getValue(Dag.Nøkkel.I).getValue(Dag.Nøkkel.I)::class)
        assertEquals(LatestOrColumn::class, turnering.strategies.getValue(Dag.Nøkkel.S_A).getValue(Dag.Nøkkel.I)::class)
        assertEquals(Row::class, turnering.strategies.getValue(Dag.Nøkkel.WD_A).getValue(Dag.Nøkkel.I)::class)
        assertEquals(Undecided::class, turnering.strategies.getValue(Dag.Nøkkel.WD_A).getValue(Dag.Nøkkel.WD_A)::class)
        assertEquals(Column::class, turnering.strategies.getValue(Dag.Nøkkel.S_A).getValue(Dag.Nøkkel.WD_A)::class)
        assertEquals(Impossible::class, turnering.strategies.getValue(Dag.Nøkkel.S_A).getValue(Dag.Nøkkel.S_A)::class)
    }

    @Test
    fun `Arbeidsdag fra søknad vinner over sykedag fra sykmelding`() {
        val turnering = DagTurnering()
        val sykedag = Sykedag(
            Uke(1).mandag, Testhendelse(
                rapportertdato = Uke(1).mandag.atTime(9, 0)
            )
        )
        val arbeidsdag = Arbeidsdag(
            Uke(1).mandag, Testhendelse(
                rapportertdato = Uke(1).mandag.atTime(12, 0)
            )
        )
        val vinner = turnering.slåss(arbeidsdag, sykedag)

        assertEquals(vinner, arbeidsdag)
    }

    @Test
    fun `kombinering av tidslinjer fører til at dagsturnering slår sammen dagene`() {
        val nySøknad = Sykdomstidslinje.sykedager(
            Uke(1).mandag, Uke(1).fredag, Testhendelse(
                rapportertdato = Uke(1).mandag.atTime(9, 0)
            )
        )
        val sendtSøknad = Testhendelse(
            rapportertdato = Uke(1).mandag.atTime(12, 0)
        )
        val sendtSøknadSykedager = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).fredag, sendtSøknad)
        val sendtSøknadArbeidsdager = Sykdomstidslinje.ikkeSykedager(Uke(1).torsdag, Uke(1).fredag, sendtSøknad)

        val tidslinje = nySøknad + (sendtSøknadSykedager + sendtSøknadArbeidsdager)
        assertTrue(
            tidslinje[Uke(1).onsdag] is Sykedag,
            "Onsdag er fortsatt en sykedag etter kombinering av ny og sendt søknad"
        )
        assertTrue(
            tidslinje[Uke(1).torsdag] is Arbeidsdag,
            "Torsdag er en arbeidsdag etter kombinering av ny og sendt søknad"
        )
    }
}
