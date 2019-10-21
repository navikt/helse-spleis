import no.nav.helse.Testhendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
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

        assertEquals(førsteTirsdag.startdato(), interval.startdato())
        assertEquals(førsteOnsdag.sluttdato(), interval.sluttdato())
        assertEquals(2, interval.antallSykedagerHvorViTellerMedHelg())
    }

    @Test
    internal fun sammenhengedeUkedagerBaklengs() {
        val interval = førsteOnsdag + førsteTirsdag

        assertEquals(førsteTirsdag.startdato(), interval.startdato())
        assertEquals(førsteOnsdag.sluttdato(), interval.sluttdato())
        assertEquals(2, interval.antallSykedagerHvorViTellerMedHelg())
    }

    @Test
    internal fun ukedagerMedOpphold() {
        val interval = førsteTorsdag + førsteTirsdag

        assertEquals(førsteTirsdag.startdato(), interval.startdato())
        assertEquals(førsteTorsdag.sluttdato(), interval.sluttdato())
        assertEquals(2, interval.antallSykedagerHvorViTellerMedHelg())
        assertEquals(3, interval.flatten().size)
    }

    @Test
    internal fun mandagTilMandag() {
        val interval = førsteMandag + andreMandag

        assertEquals(førsteMandag.startdato(), interval.startdato())
        assertEquals(andreMandag.sluttdato(), interval.sluttdato())
        assertEquals(2, interval.antallSykedagerHvorViTellerMedHelg())
        assertEquals(8, interval.flatten().size)
    }
}
