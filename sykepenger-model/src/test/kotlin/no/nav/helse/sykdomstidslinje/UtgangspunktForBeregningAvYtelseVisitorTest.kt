package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Testhendelse
import no.nav.helse.sykdomstidslinje.dag.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.streams.toList

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

        tidslinjeMedPerioder(2.sykedager, 2.implisittDager, 2.sykedager) { _, _, periode3 ->
            assertFørsteDagErUtgangspunktForBeregning(periode3, this)
        }
        tidslinjeMedPerioder(2.sykedager, 2.feriedager, 2.sykedager) { periode1, _, _ ->
            assertFørsteDagErUtgangspunktForBeregning(periode1, this)
        }
        tidslinjeMedPerioder(2.sykedager, 2.permisjonsdager, 2.sykedager) { periode1, _, _ ->
            assertFørsteDagErUtgangspunktForBeregning(periode1, this)
        }
    }

    @Test
    internal fun `tidslinjer som slutter med ignorerte dager`() {
        tidslinjeMedPerioder(2.sykedager, 2.feriedager) { periode1, _ ->
            assertFørsteDagErUtgangspunktForBeregning(periode1, this)
        }
        tidslinjeMedPerioder(2.sykedager, 2.permisjonsdager) { periode1, _ ->
            assertFørsteDagErUtgangspunktForBeregning(periode1, this)
        }
    }

    @Test
    internal fun `tidslinjer som slutter med dager som ikke er sykedager, egenmeldingsdager eller sykHelgedag`() {
        assertUgyldigTilstand(1.sykedager + 1.arbeidsdager)
        assertUgyldigTilstand(1.sykedager + 1.implisittDager)
    }

    private companion object {

        private val testhendelse = Testhendelse(
            rapportertdato = LocalDateTime.of(2019, 7, 31, 20, 0)
        )

        private val sykedag = Sykedag(LocalDate.now(), testhendelse)
        private val egenmeldingsdag = Egenmeldingsdag(LocalDate.now(), testhendelse)
        private val sykHelgedag = SykHelgedag(LocalDate.now(), testhendelse)

        private var dato = LocalDate.of(2019, 1, 1)

        private val Int.sykedager get() = lagTidslinje(this, ::Sykedag)
        private val Int.egenmeldingsdager get() = lagTidslinje(this, ::Egenmeldingsdag)
        private val Int.arbeidsdager get() = lagTidslinje(this, ::Arbeidsdag)
        private val Int.implisittDager get() = lagTidslinje(this, ::ImplisittDag)
        private val Int.studieDager get() = lagTidslinje(this, ::Studiedag)
        private val Int.utenlandsdager get() = lagTidslinje(this, ::Utenlandsdag)
        private val Int.feriedager get() = lagTidslinje(this, ::Feriedag)
        private val Int.permisjonsdager get() = lagTidslinje(this, ::Permisjonsdag)

        private fun lagTidslinje(antallDager: Int, generator: (LocalDate, SykdomstidslinjeHendelse) -> Dag): Sykdomstidslinje =
            dato.datesUntil(dato.plusDays(antallDager.toLong()))
                .map { generator(it, testhendelse) }
                .let { CompositeSykdomstidslinje(it.toList()) }
                .also {
                    dato = dato.plusDays(antallDager.toLong())
                }

        private fun tidslinjeMedPerioder(periode1: Sykdomstidslinje, periode2: Sykdomstidslinje, test: Sykdomstidslinje.(Sykdomstidslinje, Sykdomstidslinje) -> Unit) {
            (periode1 + periode2).test(periode1, periode2)
        }

        private fun tidslinjeMedPerioder(periode1: Sykdomstidslinje, periode2: Sykdomstidslinje, periode3: Sykdomstidslinje, test: Sykdomstidslinje.(Sykdomstidslinje, Sykdomstidslinje, Sykdomstidslinje) -> Unit) {
            (periode1 + periode2 + periode3).test(periode1, periode2, periode3)
        }

        private fun assertDagenErUtgangspunktForBeregning(dagen: Dag) {
            assertDagenErUtgangspunktForBeregning(dagen.dagen, dagen)
        }

        private fun assertDagenErUtgangspunktForBeregning(dagen: LocalDate, sykdomstidslinje: Sykdomstidslinje) {
            assertEquals(dagen, sykdomstidslinje.utgangspunktForBeregningAvYtelse())
        }

        private fun assertFørsteDagErUtgangspunktForBeregning(sykdomstidslinje: Sykdomstidslinje) {
            assertEquals(sykdomstidslinje.førsteDag(), sykdomstidslinje.utgangspunktForBeregningAvYtelse())
        }

        private fun assertFørsteDagErUtgangspunktForBeregning(perioden: Sykdomstidslinje, sykdomstidslinje: Sykdomstidslinje) {
            assertDagenErUtgangspunktForBeregning(perioden.førsteDag(), sykdomstidslinje)
        }

        private fun assertUgyldigTilstand(sykdomstidslinje: Sykdomstidslinje) {
            assertThrows<IllegalStateException> {
                sykdomstidslinje.utgangspunktForBeregningAvYtelse()
            }
        }
    }
}
