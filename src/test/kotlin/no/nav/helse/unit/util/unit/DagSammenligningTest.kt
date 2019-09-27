package no.nav.helse.unit.util.unit

import no.nav.helse.util.interval.Sykdomstidslinje
import no.nav.helse.util.interval.Nulldag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class DagSammenligningTest {

    companion object {
        private val tidligsteTidspunktRapportert = LocalDateTime.of(2019,9,16, 10, 45)
        private val senereTidspunktRapportert = LocalDateTime.of(2019,9,17, 11, 58)

        private val førsteMandag = LocalDate.of(2019,9,23)
    }

    @Test
    internal fun arbeidsdagPrioriteresOverSykedag() {
        val sykedag = Sykdomstidslinje.sykedager(førsteMandag, tidligsteTidspunktRapportert)
        val arbeidsdag = Sykdomstidslinje.ikkeSykedag(førsteMandag, tidligsteTidspunktRapportert)

        assertEquals(1, arbeidsdag.compareTo(sykedag))
    }

    @Test
    internal fun velgerDenSenestRapporterteDagenAvToLikeDager() {
        val tidligereSykedag = Sykdomstidslinje.sykedager(førsteMandag, tidligsteTidspunktRapportert)
        val senereSykedag = Sykdomstidslinje.sykedager(førsteMandag, senereTidspunktRapportert)

        assertEquals(-1, tidligereSykedag.compareTo(senereSykedag))
    }

    @Test
    internal fun nulldagTaperMotEnSykedag() {
        val nulldag = Nulldag(førsteMandag, tidligsteTidspunktRapportert)
        val sykedag = Sykdomstidslinje.sykedager(førsteMandag, tidligsteTidspunktRapportert)

        assertEquals(1, sykedag.compareTo(nulldag))
    }

    @Test
    internal fun toLikeDagerErLike() {
        val feriedag = Sykdomstidslinje.ferie(førsteMandag, tidligsteTidspunktRapportert)
        val arbeidsdag = Sykdomstidslinje.ikkeSykedag(førsteMandag, tidligsteTidspunktRapportert)

        assertEquals(0, arbeidsdag.compareTo(feriedag))
    }
}
