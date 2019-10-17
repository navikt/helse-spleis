package no.nav.helse.tournament

import no.nav.helse.hendelse.TestHendelser.nySøknad
import no.nav.helse.hendelse.TestHendelser.sendtSøknad
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.dag.Arbeidsdag
import no.nav.helse.sykdomstidslinje.dag.Dag
import no.nav.helse.sykdomstidslinje.dag.Sykedag
import no.nav.helse.testhelpers.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

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
        val sykedag = Sykedag(1.mandag, nySøknad(
            opprettetTidspunkt = 1.mandag.atTime(9, 0)
        ))
        val arbeidsdag = Arbeidsdag(1.mandag, sendtSøknad(
            opprettetTidspunkt = 1.mandag.atTime(12, 0)
        ))
        val vinner = turnering.slåss(arbeidsdag, sykedag)

        assertEquals(vinner, arbeidsdag)
    }

    @Test
    fun `kombinering av tidslinjer fører til at dagsturnering slår sammen dagene`() {
        val nySøknad = Sykdomstidslinje.sykedager(1.mandag, 1.fredag, nySøknad(
            opprettetTidspunkt = 1.mandag.atTime(9, 0)
        ))
        val sendtSøknad = sendtSøknad(
            opprettetTidspunkt = 1.mandag.atTime(12, 0)
        )
        val sendtSøknadSykedager = Sykdomstidslinje.sykedager(1.mandag, 1.fredag, sendtSøknad)
        val sendtSøknadArbeidsdager = Sykdomstidslinje.ikkeSykedager(1.torsdag, 1.fredag, sendtSøknad)

        val tidslinje = nySøknad + (sendtSøknadSykedager + sendtSøknadArbeidsdager)
        assertTrue(tidslinje[1.torsdag] is Arbeidsdag,
            "Torsdag er en arbeidsdag etter kombinering av ny og sendt søknad")
        assertTrue(tidslinje[1.onsdag] is Sykedag,
            "Onsdag er fortsatt en sykedag etter kombinering av ny og sendt søknad")
        assertEquals(1.onsdag, tidslinje.syketilfeller().first().sluttdato(),
            "Siste arbeidsdag er onsdag siden personen var tilbake på jobb torsdag")
    }
}
