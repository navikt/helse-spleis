package no.nav.helse.økonomi

import java.util.*
import no.nav.helse.Grunnbeløp.Companion.`6G`
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.serde.PersonData
import no.nav.helse.serde.PersonData.UtbetalingstidslinjeData
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class CreateØkonomiTest {

    @Test
    fun `opprette bare prosenter`() {
        val data = sykdomstidslinjedag(79.5)
        createØkonomi(data).also { økonomi ->
            assertEquals(79.5, økonomi.inspektør.grad.toDouble())
            assertEquals(INGEN, økonomi.inspektør.arbeidsgiverRefusjonsbeløp)
            assertEquals(INGEN, økonomi.inspektør.dekningsgrunnlag)
            assertEquals(INGEN, økonomi.inspektør.aktuellDagsinntekt)
            assertNull(økonomi.inspektør.arbeidsgiverbeløp)
            assertNull(økonomi.inspektør.personbeløp)
            økonomi
                .inntekt(1200.daglig, refusjonsbeløp = INGEN)
                .also { nyØkonomi ->
                    assertEquals(79.5, nyØkonomi.inspektør.grad.toDouble())
                    assertEquals(INGEN, nyØkonomi.inspektør.arbeidsgiverRefusjonsbeløp)
                    assertEquals(1200.daglig, nyØkonomi.inspektør.dekningsgrunnlag)
                    assertEquals(1200.daglig, nyØkonomi.inspektør.aktuellDagsinntekt)
                    assertNull(nyØkonomi.inspektør.arbeidsgiverbeløp)
                    assertNull(nyØkonomi.inspektør.personbeløp)
                }
        }
    }

    @Test
    fun `kan sette arbeidsgiverperiode`() {
        val data = sykdomstidslinjedag(79.5)
        createØkonomi(data).also { økonomi ->
            assertDoesNotThrow {
                økonomi
                    .inntekt(1200.daglig, refusjonsbeløp = 1200.daglig)
            }
        }
    }

    @Test
    fun `opprette med bare inntekt`() {
        val data = utbetalingsdag(80.0, 420.0, 1500.0, 1199.6)
        createØkonomi(data).also { økonomi ->
            assertEquals(80.0, økonomi.inspektør.grad.toDouble())
            assertEquals(420.daglig, økonomi.inspektør.arbeidsgiverRefusjonsbeløp)
            assertEquals(1500.daglig, økonomi.inspektør.aktuellDagsinntekt)
            assertEquals(1199.6.daglig, økonomi.inspektør.dekningsgrunnlag)
            assertNull(økonomi.inspektør.arbeidsgiverbeløp)
            assertNull(økonomi.inspektør.personbeløp)
            // Indirect test of Økonomi state is HarLønn
            assertThrows<IllegalStateException> { økonomi.inntekt(1200.daglig, refusjonsbeløp = 1200.daglig) }
            assertDoesNotThrow { listOf(økonomi).betal(`6G`.beløp(1.januar)) }
        }
    }

    @Test
    fun `har betalinger`() {
        val data = utbetalingsdag(79.5, 420.0, 1500.0, 1199.6, 640.0, 320.0, 79.5)
        createØkonomi(data).also { økonomi ->
            assertEquals(79.5, økonomi.inspektør.grad.toDouble())
            assertEquals(79, økonomi.brukTotalGrad { totalGrad -> totalGrad })
            assertEquals(420.daglig, økonomi.inspektør.arbeidsgiverRefusjonsbeløp)
            assertEquals(1500.daglig, økonomi.inspektør.aktuellDagsinntekt)
            assertEquals(1199.6.daglig, økonomi.inspektør.dekningsgrunnlag)
            assertEquals(640.daglig, økonomi.inspektør.arbeidsgiverbeløp)
            assertEquals(320.daglig, økonomi.inspektør.personbeløp)
            // Indirect test of Økonomi state
            assertThrows<IllegalStateException> { økonomi.inntekt(1200.daglig, refusjonsbeløp = 1200.daglig) }
            assertThrows<IllegalStateException> { listOf(økonomi).betal(`6G`.beløp(1.januar)) }
        }
    }

    private fun utbetalingsdag(
        grad: Double,
        arbeidsgiverRefusjonsbeløp: Double,
        aktuellDagsinntekt: Double,
        dekningsgrunnlag: Double,
        arbeidsgiverbeløp: Double? = null,
        personbeløp: Double? = null,
        totalGrad: Double = grad
    ) = UtbetalingstidslinjeData.UtbetalingsdagData(
        type = UtbetalingstidslinjeData.TypeData.NavDag,
        aktuellDagsinntekt = aktuellDagsinntekt,
        dekningsgrunnlag = dekningsgrunnlag,
        begrunnelser = null,
        grad = grad,
        totalGrad = totalGrad,
        utbetalingsgrad = 100.0,
        arbeidsgiverRefusjonsbeløp = arbeidsgiverRefusjonsbeløp,
        arbeidsgiverbeløp = arbeidsgiverbeløp,
        personbeløp = personbeløp,
        dato = 1.januar,
        fom = null,
        tom = null
    )

    private fun sykdomstidslinjedag(grad: Double) = PersonData.ArbeidsgiverData.SykdomstidslinjeData.DagData(
        type = PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.SYKEDAG,
        kilde = PersonData.ArbeidsgiverData.SykdomstidslinjeData.KildeData("type", UUID.randomUUID(), 1.januar.atStartOfDay()),
        grad = grad,
        other = null,
        melding = null,
        fom = null,
        tom = null,
        dato = 1.januar
    )

    private fun createØkonomi(dagData: UtbetalingstidslinjeData.UtbetalingsdagData): Økonomi {
        val dagtype = dagData.tilDto()
            .map { Utbetalingsdag.gjenopprett(it) }
            .single()

        return when (dagtype) {
            is Utbetalingsdag.NavDag -> dagtype.økonomi
            else -> error("Finner ikke økonomi for $dagtype, denne when-blokka er jo ikke exhaustive!")
        }
    }

    private fun createØkonomi(dagData: PersonData.ArbeidsgiverData.SykdomstidslinjeData.DagData): Økonomi {
        val dagtype = dagData.tilDto()
            .map { Dag.gjenopprett(it) }
            .single()

        return when (dagtype) {
            is Dag.Sykedag -> dagtype.økonomi
            else -> error("Finner ikke økonomi for $dagtype, denne when-blokka er jo ikke exhaustive!")
        }
    }
}
