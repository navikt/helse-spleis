import no.nav.helse.Testhendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.Uke
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class CompositeSykdomstidslinjeTest {

    companion object {
        private val tidspunktRapportert = Testhendelse(rapportertdato = LocalDateTime.of(2019, 7, 31, 20, 0))
    }

    @Test
    internal fun `to sykeperioder med mellomrom får riktig slutt og start dato`() {
        val førsteInterval = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).tirsdag, tidspunktRapportert)
        val andreInterval = Sykdomstidslinje.sykedager(Uke(1).fredag, Uke(2).mandag, tidspunktRapportert)

        val interval = andreInterval + førsteInterval

        Assertions.assertEquals(Uke(1).mandag, interval.startdato())
        Assertions.assertEquals(Uke(2).mandag, interval.sluttdato())
        Assertions.assertEquals(6, interval.antallSykedagerHvorViTellerMedHelg())
        Assertions.assertEquals(8, interval.flatten().size)
    }
}
