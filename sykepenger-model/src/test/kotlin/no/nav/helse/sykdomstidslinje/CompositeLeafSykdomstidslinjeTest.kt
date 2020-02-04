package no.nav.helse.sykdomstidslinje

import no.nav.helse.sykdomstidslinje.dag.Dag
import no.nav.helse.testhelpers.Uke
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class CompositeLeafSykdomstidslinjeTest {
    companion object {
        private val førsteMandag = ConcreteSykdomstidslinje.sykedag(
            gjelder = Uke(1).mandag,
            hendelseType = Dag.NøkkelHendelseType.Søknad
        )
        private val førsteTirsdag = ConcreteSykdomstidslinje.sykedag(
            gjelder = Uke(1).tirsdag,
            hendelseType = Dag.NøkkelHendelseType.Søknad
        )
        private val førsteOnsdag = ConcreteSykdomstidslinje.sykedag(
            gjelder = Uke(1).onsdag,
            hendelseType = Dag.NøkkelHendelseType.Søknad
        )
        private val førsteTorsdag = ConcreteSykdomstidslinje.sykedag(
            gjelder = Uke(1).torsdag,
            hendelseType = Dag.NøkkelHendelseType.Søknad
        )
        private val andreMandag = ConcreteSykdomstidslinje.sykedag(
            gjelder = Uke(2).mandag,
            hendelseType = Dag.NøkkelHendelseType.Søknad
        )

    }

    @Test
    internal fun sammenhengedeUkedager() {
        val interval = førsteTirsdag + førsteOnsdag

        assertEquals(førsteTirsdag.førsteDag(), interval.førsteDag())
        assertEquals(førsteOnsdag.sisteDag(), interval.sisteDag())
    }

    @Test
    internal fun sammenhengedeUkedagerBaklengs() {
        val interval = førsteOnsdag + førsteTirsdag

        assertEquals(førsteTirsdag.førsteDag(), interval.førsteDag())
        assertEquals(førsteOnsdag.sisteDag(), interval.sisteDag())
    }

    @Test
    internal fun ukedagerMedOpphold() {
        val interval = førsteTorsdag + førsteTirsdag

        assertEquals(førsteTirsdag.førsteDag(), interval.førsteDag())
        assertEquals(førsteTorsdag.sisteDag(), interval.sisteDag())
        assertEquals(3, interval.flatten().size)
    }

    @Test
    internal fun mandagTilMandag() {
        val interval = førsteMandag + andreMandag

        assertEquals(førsteMandag.førsteDag(), interval.førsteDag())
        assertEquals(andreMandag.sisteDag(), interval.sisteDag())
        assertEquals(8, interval.flatten().size)
    }

    private operator fun ConcreteSykdomstidslinje.plus(other: ConcreteSykdomstidslinje) =
        this.plus(other, ConcreteSykdomstidslinje.Companion::implisittDag)
}
