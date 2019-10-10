package no.nav.helse.tournament

import no.nav.helse.sykdomstidslinje.dag.Dag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DagTurneringTest {
    @Test
    fun `Turneringen skal inneholde riktige strategier basert på en csv-fil`() {
        val turnering = DagTurnering("/microturnering.csv")

        assertEquals(Latest::class, turnering.strategies.getValue(Dag.Nøkkel.WD_I).getValue(Dag.Nøkkel.WD_I)::class)
        assertEquals(Row::class, turnering.strategies.getValue(Dag.Nøkkel.WD_A).getValue(Dag.Nøkkel.WD_I)::class)
        assertEquals(Undecided::class, turnering.strategies.getValue(Dag.Nøkkel.WD_A).getValue(Dag.Nøkkel.WD_A)::class)
        assertEquals(Column::class, turnering.strategies.getValue(Dag.Nøkkel.S).getValue(Dag.Nøkkel.WD_A)::class)
        assertEquals(Impossible::class, turnering.strategies.getValue(Dag.Nøkkel.S).getValue(Dag.Nøkkel.S)::class)
    }
}