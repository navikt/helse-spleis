package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Testhendelse
import no.nav.helse.sykdomstidslinje.dag.Dag
import no.nav.helse.sykdomstidslinje.dag.Egenmeldingsdag
import no.nav.helse.sykdomstidslinje.dag.SykHelgedag
import no.nav.helse.sykdomstidslinje.dag.Sykedag
import no.nav.helse.testhelpers.Uke
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime

internal class UtgangspunktForBeregningAvYtelseVisitorTest {

    @Test
    fun `feiler når dagen er null`() {
        assertThrows<IllegalStateException> {
            UtgangspunktForBeregningAvYtelseVisitor()
                .utgangspunktForBeregningAvYtelse()
        }
    }

    private fun <DAG: Dag> assertUtgangspunktForBeregningAvYtelse(dagen: DAG, visit: UtgangspunktForBeregningAvYtelseVisitor.(DAG) -> Unit) {
        UtgangspunktForBeregningAvYtelseVisitor().apply {
            visit(dagen)
        }.also {
            assertEquals(dagen.dagen, it.utgangspunktForBeregningAvYtelse())
        }
    }

    @Test
    fun `utgangspunkt for beregning er sykedag, egenmeldingsdag eller syk helgedag`() {
        assertUtgangspunktForBeregningAvYtelse(sykedag, UtgangspunktForBeregningAvYtelseVisitor::visitSykedag)
        assertUtgangspunktForBeregningAvYtelse(egenmeldingsdag, UtgangspunktForBeregningAvYtelseVisitor::visitEgenmeldingsdag)
        assertUtgangspunktForBeregningAvYtelse(sykHelgedag, UtgangspunktForBeregningAvYtelseVisitor::visitSykHelgedag)
    }

    @Test
    internal fun `utgangspunkt for beregning av ytelse er første egenmeldingsdag, sykedag eller sykhelgdag i en sammenhengende periode`() {
        5.sykedager.also {
            assertEquals(it.førsteDag(), it.utgangspunktForBeregningAvYtelse())
        }

        5.egenmeldingsdager.also {
            assertEquals(it.førsteDag(), it.utgangspunktForBeregningAvYtelse())
        }

        tidslinjeMedPerioder(2.sykedager, 2.implisittDager, 2.sykedager) { _, _, periode3 ->
            assertEquals(periode3.førsteDag(), this.utgangspunktForBeregningAvYtelse())
        }
    }

    @Test
    internal fun `ferie påvirker ikke utgangspunktet for beregning av ytelse`() {
        tidslinjeMedPerioder(5.sykedager, 5.feriedager) { periode1, _ ->
            assertEquals(periode1.førsteDag(), this.utgangspunktForBeregningAvYtelse())
        }
    }

    @Test
    internal fun `utenlands- eller studiedager kan ikke håndteres`() {
        assertThrows<IllegalStateException> {
            (5.sykedager + 5.utenlandsdager).utgangspunktForBeregningAvYtelse()
        }

        assertThrows<IllegalStateException> {
            (5.sykedager + 5.studieDager).utgangspunktForBeregningAvYtelse()
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

    private companion object {
        private val tidspunktRapportert = Testhendelse(
            rapportertdato = LocalDateTime.of(2019, 7, 31, 20, 0)
        )

        private val sykedag = Sykedag(LocalDate.now(), tidspunktRapportert)
        private val egenmeldingsdag = Egenmeldingsdag(LocalDate.now(), tidspunktRapportert)
        private val sykHelgedag = SykHelgedag(LocalDate.now(), tidspunktRapportert)

        private var dato = LocalDate.of(2019, 1, 1)

        private val Int.sykedager get() = Sykdomstidslinje.sykedager(dato, dato.plusDays(this.toLong()), tidspunktRapportert).also {
            dato = it.sisteDag().plusDays(1)
        }
        private val Int.egenmeldingsdager get() = Sykdomstidslinje.egenmeldingsdager(dato, dato.plusDays(this.toLong()), tidspunktRapportert).also {
            dato = it.sisteDag().plusDays(1)
        }
        private val Int.implisittDager get() = Sykdomstidslinje.implisittdager(dato, dato.plusDays(this.toLong()), tidspunktRapportert).also {
            dato = it.sisteDag().plusDays(1)
        }
        private val Int.studieDager get() = Sykdomstidslinje.studiedager(dato, dato.plusDays(this.toLong()), tidspunktRapportert).also {
            dato = it.sisteDag().plusDays(1)
        }
        private val Int.utenlandsdager get() = Sykdomstidslinje.utenlandsdager(dato, dato.plusDays(this.toLong()), tidspunktRapportert).also {
            dato = it.sisteDag().plusDays(1)
        }
        private val Int.feriedager get() = Sykdomstidslinje.ferie(dato, dato.plusDays(this.toLong()), tidspunktRapportert).also {
            dato = it.sisteDag().plusDays(1)
        }

        private fun tidslinjeMedPerioder(periode1: Sykdomstidslinje, periode2: Sykdomstidslinje, test: Sykdomstidslinje.(Sykdomstidslinje, Sykdomstidslinje) -> Unit) {
            (periode1 + periode2).test(periode1, periode2)
        }

        private fun tidslinjeMedPerioder(periode1: Sykdomstidslinje, periode2: Sykdomstidslinje, periode3: Sykdomstidslinje, test: Sykdomstidslinje.(Sykdomstidslinje, Sykdomstidslinje, Sykdomstidslinje) -> Unit) {
            (periode1 + periode2 + periode3).test(periode1, periode2, periode3)
        }
    }
}
