package no.nav.helse.tournament

import no.nav.helse.hendelse.TestHendelser.nySøknad
import no.nav.helse.hendelse.TestHendelser.sendtSøknad
import no.nav.helse.sykdomstidslinje.dag.Arbeidsdag
import no.nav.helse.sykdomstidslinje.dag.Dag
import no.nav.helse.sykdomstidslinje.dag.Sykedag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DagTurneringTest {

    @Test
    fun `Turneringen skal inneholde riktige strategier basert på en csv-fil`() {
        val turnering = DagTurnering("/microturnering.csv")
        assertEquals(LatestOrRow::class, turnering.strategies.getValue(Dag.Nøkkel.I).getValue(Dag.Nøkkel.I)::class)
        assertEquals(LatestOrColumn::class, turnering.strategies.getValue(Dag.Nøkkel.S).getValue(Dag.Nøkkel.I)::class)
        assertEquals(Row::class, turnering.strategies.getValue(Dag.Nøkkel.WD_A).getValue(Dag.Nøkkel.I)::class)
        assertEquals(Undecided::class, turnering.strategies.getValue(Dag.Nøkkel.WD_A).getValue(Dag.Nøkkel.WD_A)::class)
        assertEquals(Column::class, turnering.strategies.getValue(Dag.Nøkkel.S).getValue(Dag.Nøkkel.WD_A)::class)
        assertEquals(Impossible::class, turnering.strategies.getValue(Dag.Nøkkel.S).getValue(Dag.Nøkkel.S)::class)
    }

    @Disabled("YOLO: Knut skal på ferie")
    @Test
    fun `Arbeidsdag fra søknad vinner over sykedag fra sykmelding`() {
        val turnering = DagTurnering()
        val arbeidsdag = Arbeidsdag(LocalDate.of(2019,10,10), sendtSøknad())
        val sykedag = Sykedag(LocalDate.of(2019,10,10), nySøknad())
        val vinner = turnering.slåss(arbeidsdag, sykedag)

        assertEquals(vinner, arbeidsdag)
    }
}
