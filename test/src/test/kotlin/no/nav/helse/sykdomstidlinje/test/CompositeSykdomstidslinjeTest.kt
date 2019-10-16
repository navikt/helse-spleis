import no.nav.helse.Testhendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.fredag
import no.nav.helse.testhelpers.mandag
import no.nav.helse.testhelpers.tirsdag
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class CompositeSykdomstidslinjeTest {

    companion object {
        private val tidspunktRapportert = Testhendelse(rapportertdato = LocalDateTime.of(2019, 7, 31, 20, 0))
    }

    @Test
    internal fun `to sykeperioder med mellomrom får riktig slutt og start dato`() {
        val førsteInterval = Sykdomstidslinje.sykedager(1.mandag, 1.tirsdag, tidspunktRapportert)
        val andreInterval = Sykdomstidslinje.sykedager(1.fredag, 2.mandag, tidspunktRapportert)

        val interval = andreInterval + førsteInterval

        Assertions.assertEquals(1.mandag, interval.startdato())
        Assertions.assertEquals(2.mandag, interval.sluttdato())
        Assertions.assertEquals(6, interval.antallSykedagerHvorViTellerMedHelg())
        Assertions.assertEquals(8, interval.flatten().size)
    }
}
