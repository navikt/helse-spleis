package no.nav.helse.økonomi

import java.util.UUID
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class CreateØkonomiTest {

    @Test
    fun `kan ikke betale økonomi med kun grad`() {
        val data = sykdomstidslinjedag(79.5)
        createØkonomi(data).also { økonomi ->
            assertThrows<IllegalStateException> { listOf(økonomi).betal() }
        }
    }

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
                .inntekt(1200.daglig, `6G` = `6G`.beløp(1.januar), refusjonsbeløp = INGEN)
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
            assertDoesNotThrow { økonomi
                .inntekt(1200.daglig, `6G` = `6G`.beløp(1.januar), refusjonsbeløp = 1200.daglig)
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
            assertThrows<IllegalStateException> { økonomi.inntekt(1200.daglig, `6G` = `6G`.beløp(1.januar), refusjonsbeløp = 1200.daglig) }
            assertDoesNotThrow { listOf(økonomi).betal() }
        }
    }

    @Test
    fun `har betalinger`() {
        val data = utbetalingsdag(79.5, 420.0, 1500.0, 1199.6, 640.0, 320.0, 79.5, true)
        createØkonomi(data).also { økonomi ->
            assertEquals(79.5, økonomi.inspektør.grad.toDouble())
            assertEquals(79, økonomi.brukTotalGrad { totalGrad -> totalGrad })
            assertEquals(420.daglig, økonomi.inspektør.arbeidsgiverRefusjonsbeløp)
            assertEquals(1500.daglig, økonomi.inspektør.aktuellDagsinntekt)
            assertEquals(1199.6.daglig, økonomi.inspektør.dekningsgrunnlag)
            assertEquals(640.daglig, økonomi.inspektør.arbeidsgiverbeløp)
            assertEquals(320.daglig, økonomi.inspektør.personbeløp)
            assertTrue(økonomi.er6GBegrenset())
            // Indirect test of Økonomi state
            assertThrows<IllegalStateException> { økonomi.inntekt(1200.daglig, `6G` = `6G`.beløp(1.januar), refusjonsbeløp = 1200.daglig) }
            assertThrows<IllegalStateException> { listOf(økonomi).betal() }
        }
    }

    private fun utbetalingsdag(
        grad: Double,
        arbeidsgiverRefusjonsbeløp: Double,
        aktuellDagsinntekt: Double,
        dekningsgrunnlag: Double,
        arbeidsgiverbeløp: Double? = null,
        personbeløp: Double? = null,
        totalGrad: Double = grad,
        er6GBegrenset: Boolean = false
    ) = UtbetalingstidslinjeData.UtbetalingsdagData(
        UtbetalingstidslinjeData.TypeData.NavDag,
        aktuellDagsinntekt,
        aktuellDagsinntekt,
        dekningsgrunnlag,
        `6G`.beløp(1.januar).reflection { årlig, _, _, _ -> årlig },
        null,
        grad,
        totalGrad,
        arbeidsgiverRefusjonsbeløp,
        arbeidsgiverbeløp,
        personbeløp,
        er6GBegrenset,
        dato = 1.januar,
        fom = null,
        tom = null
    )

    private fun sykdomstidslinjedag(grad: Double) = PersonData.ArbeidsgiverData.SykdomstidslinjeData.DagData(
        PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.SYKEDAG,
        PersonData.ArbeidsgiverData.SykdomstidslinjeData.KildeData("type", UUID.randomUUID(), 1.januar.atStartOfDay()),
        grad,
        null,
        null,
        null,
        null,
        1.januar
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
