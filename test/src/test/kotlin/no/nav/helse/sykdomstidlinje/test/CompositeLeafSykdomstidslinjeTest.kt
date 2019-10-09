import no.nav.helse.sykdomstidlinje.test.Testhendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class CompositeLeafSykdomstidslinjeTest {

    companion object {
        private val tidspunktRapportert = Testhendelse()

        private val førsteMandag = Sykdomstidslinje.sykedag(
            gjelder = LocalDate.of(2019, 9, 23),
            hendelse = tidspunktRapportert
        )
        private val førsteTirsdag = Sykdomstidslinje.sykedag(
            gjelder = LocalDate.of(2019, 9, 24),
            hendelse = tidspunktRapportert
        )
        private val førsteOnsdag = Sykdomstidslinje.sykedag(
            gjelder = LocalDate.of(2019, 9, 25),
            hendelse = tidspunktRapportert
        )
        private val førsteTorsdag = Sykdomstidslinje.sykedag(
            gjelder = LocalDate.of(2019, 9, 26),
            hendelse = tidspunktRapportert
        )
        private val andreMandag = Sykdomstidslinje.sykedag(
            gjelder = LocalDate.of(2019, 9, 30),
            hendelse = tidspunktRapportert
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
