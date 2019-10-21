import no.nav.helse.Testhendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.Uke
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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

        assertEquals(Uke(1).mandag, interval.startdato())
        assertEquals(Uke(2).mandag, interval.sluttdato())
        assertEquals(6, interval.antallSykedagerHvorViTellerMedHelg())
        assertEquals(8, interval.flatten().size)
    }

    @Test
    internal fun `tidslinje med ubestemt dag er utenfor omfang`() {
        val studiedag = Sykdomstidslinje.studiedag(Uke(1).mandag, tidspunktRapportert)
        val sykedag = Sykdomstidslinje.sykedag(Uke(1).mandag, tidspunktRapportert)
        val tidslinje = studiedag + sykedag

        assertTrue(tidslinje.erUtenforOmfang())
    }

    @Test
    internal fun `tidslinje med permisjonsdag er utenfor omfang`() {
        val permisjonsdag = Sykdomstidslinje.permisjonsdag(Uke(1).mandag, tidspunktRapportert)
        assertTrue(permisjonsdag.erUtenforOmfang())
    }

    @Test
    internal fun `tidslinje flere enn ett syketilfelle er utenfor omfang`() {
        val sykedag1 = Sykdomstidslinje.sykedag(Uke(1).mandag, tidspunktRapportert)
        val sykedag2 = Sykdomstidslinje.sykedag(Uke(4).mandag, tidspunktRapportert)

        assertTrue(sykedag1.plus(sykedag2).erUtenforOmfang())
    }
}
