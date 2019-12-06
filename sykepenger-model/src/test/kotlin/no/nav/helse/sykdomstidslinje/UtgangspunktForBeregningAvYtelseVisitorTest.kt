package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Testhendelse
import no.nav.helse.testhelpers.Uke
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

internal class UtgangspunktForBeregningAvYtelseVisitorTest {

    companion object {
        private val tidspunktRapportert = Testhendelse(
            rapportertdato = LocalDateTime.of(2019, 7, 31, 20, 0)
        )
    }

    @Test
    internal fun `utgangspunkt for beregning av ytelse er første egenmeldingsdag, sykedag eller sykhelgdag i en sammenhengende periode`() {
        Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).fredag, tidspunktRapportert)
            .also {
                Assertions.assertEquals(Uke(1).mandag, it.utgangspunktForBeregningAvYtelse())
            }

        Sykdomstidslinje.egenmeldingsdager(
            Uke(1).mandag, Uke(1).fredag,
            tidspunktRapportert
        ).also {
            Assertions.assertEquals(Uke(1).mandag, it.utgangspunktForBeregningAvYtelse())
        }

        (Sykdomstidslinje.sykedager(
            Uke(1).mandag,
            Uke(1).tirsdag,
            tidspunktRapportert
        ) + Sykdomstidslinje.sykedager(
            Uke(2).torsdag, Uke(2).fredag,
            tidspunktRapportert
        )).also {
            Assertions.assertEquals(Uke(2).torsdag, it.utgangspunktForBeregningAvYtelse())
        }
    }

    @Test
    internal fun `utgangspunkt for beregning av ytelse på tidslinjer med ubestemte dager`() {
        val sykedager1 = Sykdomstidslinje.sykedager(
            Uke(1).mandag, Uke(1).tirsdag,
            tidspunktRapportert
        )
        val sykedager2 = Sykdomstidslinje.sykedager(
            Uke(2).torsdag, Uke(2).fredag,
            tidspunktRapportert
        )

        val ubestemtDag = Sykdomstidslinje.utenlandsdag(
            Uke(1).fredag,
            tidspunktRapportert
        )
        val studiedag = Sykdomstidslinje.studiedag(Uke(1).fredag, tidspunktRapportert)

        (sykedager1 + ubestemtDag + sykedager2).also {
            assertThrows<IllegalStateException> {
                it.utgangspunktForBeregningAvYtelse()
            }
        }

        (sykedager1 + studiedag + sykedager2).also {
            assertThrows<IllegalStateException> {
                it.utgangspunktForBeregningAvYtelse()
            }
        }
    }

    @Test
    internal fun `utgangspunkt for beregning av ytelse på tidslinjer som slutter med arbeidsdager`() {
        val sykedager = Sykdomstidslinje.sykedager(
            Uke(1).mandag, Uke(1).tirsdag,
            tidspunktRapportert
        )
        val ikkeSykedag = Sykdomstidslinje.utenlandsdag(
            Uke(1).fredag,
            tidspunktRapportert
        )

        (sykedager + ikkeSykedag).also {
            assertThrows<IllegalStateException> {
                it.utgangspunktForBeregningAvYtelse()
            }
        }
    }
}
