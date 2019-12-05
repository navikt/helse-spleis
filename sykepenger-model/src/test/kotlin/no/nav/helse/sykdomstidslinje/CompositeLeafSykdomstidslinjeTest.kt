package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Testhendelse
import no.nav.helse.testhelpers.Uke
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class CompositeLeafSykdomstidslinjeTest {

    companion object {
        private val sendtSøknad = Testhendelse()

        private val førsteMandag = Sykdomstidslinje.sykedag(
            gjelder = Uke(1).mandag,
            hendelse = sendtSøknad
        )
        private val førsteTirsdag = Sykdomstidslinje.sykedag(
            gjelder = Uke(1).tirsdag,
            hendelse = sendtSøknad
        )
        private val førsteOnsdag = Sykdomstidslinje.sykedag(
            gjelder = Uke(1).onsdag,
            hendelse = sendtSøknad
        )
        private val førsteTorsdag = Sykdomstidslinje.sykedag(
            gjelder = Uke(1).torsdag,
            hendelse = sendtSøknad
        )
        private val andreMandag = Sykdomstidslinje.sykedag(
            gjelder = Uke(2).mandag,
            hendelse = sendtSøknad
        )

    }

    @Test
    internal fun sammenhengedeUkedager() {
        val interval = førsteTirsdag + førsteOnsdag

        assertEquals(førsteTirsdag.førsteDag(), interval.førsteDag())
        assertEquals(førsteOnsdag.sisteDag(), interval.sisteDag())
        assertEquals(2, interval.antallSykedagerHvorViTellerMedHelg())
    }

    @Test
    internal fun sammenhengedeUkedagerBaklengs() {
        val interval = førsteOnsdag + førsteTirsdag

        assertEquals(førsteTirsdag.førsteDag(), interval.førsteDag())
        assertEquals(førsteOnsdag.sisteDag(), interval.sisteDag())
        assertEquals(2, interval.antallSykedagerHvorViTellerMedHelg())
    }

    @Test
    internal fun ukedagerMedOpphold() {
        val interval = førsteTorsdag + førsteTirsdag

        assertEquals(førsteTirsdag.førsteDag(), interval.førsteDag())
        assertEquals(førsteTorsdag.sisteDag(), interval.sisteDag())
        assertEquals(2, interval.antallSykedagerHvorViTellerMedHelg())
        assertEquals(3, interval.flatten().size)
    }

    @Test
    internal fun mandagTilMandag() {
        val interval = førsteMandag + andreMandag

        assertEquals(førsteMandag.førsteDag(), interval.førsteDag())
        assertEquals(andreMandag.sisteDag(), interval.sisteDag())
        assertEquals(2, interval.antallSykedagerHvorViTellerMedHelg())
        assertEquals(8, interval.flatten().size)
    }
}
