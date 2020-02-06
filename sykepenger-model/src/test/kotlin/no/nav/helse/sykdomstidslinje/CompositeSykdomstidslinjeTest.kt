package no.nav.helse.sykdomstidslinje

import no.nav.helse.sykdomstidslinje.dag.Arbeidsdag
import no.nav.helse.sykdomstidslinje.dag.Dag
import no.nav.helse.sykdomstidslinje.dag.ImplisittDag
import no.nav.helse.sykdomstidslinje.dag.Sykedag
import no.nav.helse.testhelpers.Uke
import no.nav.helse.testhelpers.get
import no.nav.helse.tournament.historiskDagturnering
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class CompositeSykdomstidslinjeTest {
    @Test
    internal fun `kan bestemme hvilken type dager mellom to perioder skal ha`() {
        val arbeidsgiverperiode1 = ConcreteSykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).onsdag, Dag.NøkkelHendelseType.Søknad)
        val arbeidsgiverperiode2 = ConcreteSykdomstidslinje.sykedager(Uke(2).onsdag, Uke(2).fredag, Dag.NøkkelHendelseType.Søknad)

        val arbeidsgiverperiode =
            arbeidsgiverperiode1.plus(arbeidsgiverperiode2, ConcreteSykdomstidslinje.Companion::ikkeSykedag, historiskDagturnering)

        assertEquals(Sykedag::class, arbeidsgiverperiode[Uke(1).mandag]!!::class)
        assertEquals(Sykedag::class, arbeidsgiverperiode[Uke(1).tirsdag]!!::class)
        assertEquals(Sykedag::class, arbeidsgiverperiode[Uke(1).onsdag]!!::class)
        assertEquals(Arbeidsdag::class, arbeidsgiverperiode[Uke(1).torsdag]!!::class)
        assertEquals(Arbeidsdag::class, arbeidsgiverperiode[Uke(1).fredag]!!::class)
        assertEquals(ImplisittDag::class, arbeidsgiverperiode[Uke(1).lørdag]!!::class)
        assertEquals(ImplisittDag::class, arbeidsgiverperiode[Uke(1).søndag]!!::class)
        assertEquals(Arbeidsdag::class, arbeidsgiverperiode[Uke(2).mandag]!!::class)
        assertEquals(Arbeidsdag::class, arbeidsgiverperiode[Uke(2).tirsdag]!!::class)
        assertEquals(Sykedag::class, arbeidsgiverperiode[Uke(2).onsdag]!!::class)
        assertEquals(Sykedag::class, arbeidsgiverperiode[Uke(2).torsdag]!!::class)
        assertEquals(Sykedag::class, arbeidsgiverperiode[Uke(2).fredag]!!::class)
    }

    @Test
    internal fun `to sykeperioder med mellomrom får riktig slutt og start dato`() {
        val førsteInterval = ConcreteSykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).tirsdag, Dag.NøkkelHendelseType.Søknad)
        val andreInterval = ConcreteSykdomstidslinje.sykedager(Uke(1).fredag, Uke(2).mandag, Dag.NøkkelHendelseType.Søknad)

        val interval = andreInterval + førsteInterval

        assertEquals(Uke(1).mandag, interval.førsteDag())
        assertEquals(Uke(2).mandag, interval.sisteDag())
        assertEquals(8, interval.flatten().size)
    }

    @Test
    internal fun `tidslinje med ubestemt dag er utenfor omfang`() {
        val studiedag = ConcreteSykdomstidslinje.studiedag(Uke(1).mandag, Dag.NøkkelHendelseType.Søknad)
        val sykedag = ConcreteSykdomstidslinje.sykedag(Uke(1).mandag, Dag.NøkkelHendelseType.Søknad)
        val tidslinje = studiedag + sykedag

        assertTrue(tidslinje.erUtenforOmfang())
    }

    @Test
    internal fun `tidslinje med permisjonsdag er utenfor omfang`() {
        val permisjonsdag = ConcreteSykdomstidslinje.permisjonsdag(Uke(1).mandag, Dag.NøkkelHendelseType.Søknad)
        assertTrue(permisjonsdag.erUtenforOmfang())
    }

    private operator fun ConcreteSykdomstidslinje.plus(other: ConcreteSykdomstidslinje) =
        this.plus(other, ConcreteSykdomstidslinje.Companion::implisittDag, historiskDagturnering)
}
