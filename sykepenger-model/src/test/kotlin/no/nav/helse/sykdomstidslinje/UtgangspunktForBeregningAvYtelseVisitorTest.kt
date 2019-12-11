package no.nav.helse.sykdomstidslinje

import no.nav.helse.*
import no.nav.helse.hendelser.Testhendelse
import no.nav.helse.sykdomstidslinje.dag.Dag
import no.nav.helse.sykdomstidslinje.dag.Egenmeldingsdag
import no.nav.helse.sykdomstidslinje.dag.SykHelgedag
import no.nav.helse.sykdomstidslinje.dag.Sykedag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime

internal class UtgangspunktForBeregningAvYtelseVisitorTest {

    @Test
    internal fun `kaster exception for ugyldige situasjoner`() {
        assertThrows<IllegalStateException> { UtgangspunktForBeregningAvYtelseVisitor().utgangspunktForBeregningAvYtelse() }
        assertUgyldigTilstand(1.utenlandsdager)
        assertUgyldigTilstand(1.studieDager)
        assertUgyldigTilstand(1.feriedager)
        assertUgyldigTilstand(1.permisjonsdager)
        assertUgyldigTilstand(1.implisittDager)
        assertUgyldigTilstand(1.arbeidsdager)
        assertUgyldigTilstand(1.sykedager + 1.utenlandsdager)
        assertUgyldigTilstand(1.sykedager + 1.utenlandsdager + 1.sykedager)
        assertUgyldigTilstand(1.sykedager + 1.studieDager)
        assertUgyldigTilstand(1.sykedager + 1.studieDager + 1.sykedager)
    }

    @Test
    internal fun `utgangspunkt for beregning er sykedag, egenmeldingsdag eller syk helgedag`() {
        assertDagenErUtgangspunktForBeregning(sykedag)
        assertDagenErUtgangspunktForBeregning(egenmeldingsdag)
        assertDagenErUtgangspunktForBeregning(sykHelgedag)
    }

    @Test
    internal fun `utgangspunkt for beregning av ytelse er første egenmeldingsdag, sykedag eller sykhelgdag i en sammenhengende periode`() {
        assertFørsteDagErUtgangspunktForBeregning(2.sykedager)
        assertFørsteDagErUtgangspunktForBeregning(2.egenmeldingsdager)

        perioder(2.sykedager, 2.implisittDager, 2.sykedager) { _, _, periode3 ->
            assertFørsteDagErUtgangspunktForBeregning(periode3, this)
        }
        perioder(2.sykedager, 2.feriedager, 2.sykedager) { periode1, _, _ ->
            assertFørsteDagErUtgangspunktForBeregning(periode1, this)
        }
        perioder(2.sykedager, 2.permisjonsdager, 2.sykedager) { periode1, _, _ ->
            assertFørsteDagErUtgangspunktForBeregning(periode1, this)
        }
    }

    @Test
    internal fun `tidslinjer som slutter med ignorerte dager`() {
        perioder(2.sykedager, 2.feriedager) { periode1, _ ->
            assertFørsteDagErUtgangspunktForBeregning(periode1, this)
        }
        perioder(2.sykedager, 2.permisjonsdager) { periode1, _ ->
            assertFørsteDagErUtgangspunktForBeregning(periode1, this)
        }
    }

    @Test
    internal fun `tidslinjer som slutter med dager som ikke er sykedager, egenmeldingsdager eller sykHelgedag`() {
        assertUgyldigTilstand(1.sykedager + 1.arbeidsdager)
        assertUgyldigTilstand(1.sykedager + 1.implisittDager)
    }

    companion object {

        private val testhendelse = Testhendelse(rapportertdato = LocalDateTime.now())

        internal val sykedag = Sykedag(LocalDate.now(), testhendelse)
        internal val egenmeldingsdag = Egenmeldingsdag(LocalDate.now(), testhendelse)
        internal val sykHelgedag = SykHelgedag(LocalDate.now(), testhendelse)

        private fun assertDagenErUtgangspunktForBeregning(dagen: Dag) {
            assertDagenErUtgangspunktForBeregning(dagen.dagen, dagen)
        }

        private fun assertDagenErUtgangspunktForBeregning(dagen: LocalDate, sykdomstidslinje: ConcreteSykdomstidslinje) {
            assertEquals(dagen, sykdomstidslinje.utgangspunktForBeregningAvYtelse())
        }

        private fun assertFørsteDagErUtgangspunktForBeregning(sykdomstidslinje: ConcreteSykdomstidslinje) {
            assertEquals(sykdomstidslinje.førsteDag(), sykdomstidslinje.utgangspunktForBeregningAvYtelse())
        }

        private fun assertFørsteDagErUtgangspunktForBeregning(perioden: ConcreteSykdomstidslinje, sykdomstidslinje: ConcreteSykdomstidslinje) {
            assertDagenErUtgangspunktForBeregning(perioden.førsteDag(), sykdomstidslinje)
        }

        private fun assertUgyldigTilstand(sykdomstidslinje: ConcreteSykdomstidslinje) {
            assertThrows<IllegalStateException> {
                sykdomstidslinje.utgangspunktForBeregningAvYtelse()
            }
        }
    }
}
