package no.nav.helse.økonomi

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Grunnbeløp.Companion.`6G`
import no.nav.helse.inspectors.ØkonomiAsserter
import no.nav.helse.januar
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.utbetalingstidslinje.UtbetalingsdagVisitor
import no.nav.helse.serde.PersonData
import no.nav.helse.serde.PersonData.UtbetalingstidslinjeData
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class CreateØkonomiTest {

    @Test
    fun `betale uten inntekt gir 0 i beløp`() {
        val data = sykdomstidslinjedag(79.5)
        createØkonomi(data).also { økonomi ->
            listOf(økonomi).betal()
            økonomi.accept(ØkonomiAsserter { grad, arbeidsgiverRefusjonsbeløp, dekningsgrunnlag, _, aktuellDagsinntekt, arbeidsgiverbeløp, personbeløp, begrenset ->
                assertEquals(79.5, grad)
                assertEquals(0.0, arbeidsgiverRefusjonsbeløp)
                assertEquals(0.0, dekningsgrunnlag)
                assertEquals(0.0, aktuellDagsinntekt)
                assertEquals(0.0, arbeidsgiverbeløp)
                assertEquals(0.0, personbeløp)
                assertNotNull(begrenset)
                assertFalse(begrenset)
            })
        }
    }

    @Test
    fun `opprette bare prosenter`() {
        val data = sykdomstidslinjedag(79.5)
        createØkonomi(data).also { økonomi ->
            økonomi.accept(ØkonomiAsserter { grad, arbeidsgiverRefusjonsbeløp, dekningsgrunnlag, _, aktuellDagsinntekt, arbeidsgiverbeløp, personbeløp, begrenset ->
                assertEquals(79.5, grad)
                assertEquals(0.0, arbeidsgiverRefusjonsbeløp)
                assertEquals(0.0, dekningsgrunnlag)
                assertEquals(0.0, aktuellDagsinntekt)
                assertNull(arbeidsgiverbeløp)
                assertNull(personbeløp)
                assertNull(begrenset)
            })
            økonomi
                .inntekt(1200.daglig, `6G` = `6G`.beløp(1.januar), refusjonsbeløp = INGEN)
                .accept(ØkonomiAsserter { grad, arbeidsgiverRefusjonsbeløp, dekningsgrunnlag, _, aktuellDagsinntekt, arbeidsgiverbeløp, personbeløp, begrenset ->
                    assertEquals(79.5, grad)
                    assertEquals(0.0, arbeidsgiverRefusjonsbeløp)
                    assertEquals(1200.0, dekningsgrunnlag)
                    assertEquals(1200.0, aktuellDagsinntekt)
                    assertNull(arbeidsgiverbeløp)
                    assertNull(personbeløp)
                    assertNull(begrenset)
                })
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
            økonomi.accept(ØkonomiAsserter { grad, arbeidsgiverRefusjonsbeløp, dekningsgrunnlag, _, aktuellDagsinntekt, arbeidsgiverbeløp, personbeløp, begrenset ->
                assertEquals(80.0, grad)
                assertEquals(420.0, arbeidsgiverRefusjonsbeløp)
                assertEquals(1500.0, aktuellDagsinntekt)
                assertEquals(1199.6, dekningsgrunnlag)
                assertNull(arbeidsgiverbeløp)
                assertNull(personbeløp)
                // assertNull(begrenset) Er det virkelig funksjonelt viktig at 'begrenset' er null?
            })
            // Indirect test of Økonomi state is HarLønn
            assertThrows<IllegalStateException> { økonomi.inntekt(1200.daglig, `6G` = `6G`.beløp(1.januar), refusjonsbeløp = 1200.daglig) }
            assertDoesNotThrow { listOf(økonomi).betal() }
        }
    }

    @Test
    fun `har betalinger`() {
        val data = utbetalingsdag(79.5, 420.0, 1500.0, 1199.6, 640.0, 320.0, 79.5, true)
        createØkonomi(data).also { økonomi ->
            økonomi.accept(ØkonomiAsserter { grad, arbeidsgiverRefusjonsbeløp, dekningsgrunnlag, totalGrad, aktuellDagsinntekt, arbeidsgiverbeløp, personbeløp, begrenset ->
                assertEquals(79.5, grad)
                assertEquals(79.5, totalGrad)
                assertEquals(420.0, arbeidsgiverRefusjonsbeløp)
                assertEquals(1500.0, aktuellDagsinntekt)
                assertEquals(1199.6, dekningsgrunnlag)
                assertEquals(640.0, arbeidsgiverbeløp)
                assertEquals(320.0, personbeløp)
                assertTrue(begrenset as Boolean)
            })
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
        dekningsgrunnlag,
        `6G`.beløp(1.januar).reflection { årlig, _, _, _ -> årlig },
        null,
        null,
        grad,
        totalGrad,
        arbeidsgiverRefusjonsbeløp,
        arbeidsgiverbeløp,
        personbeløp,
        er6GBegrenset
    )

    private fun sykdomstidslinjedag(grad: Double) = PersonData.ArbeidsgiverData.SykdomstidslinjeData.DagData(
        PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.SYKEDAG,
        PersonData.ArbeidsgiverData.SykdomstidslinjeData.KildeData("type", UUID.randomUUID(), 1.januar.atStartOfDay()),
        grad,
        null,
        null
    )

    private fun createØkonomi(dagData: UtbetalingstidslinjeData.UtbetalingsdagData): Økonomi {
        lateinit var fangetØkonomi: Økonomi
        dagData.parseDag(1.januar).accept(object : UtbetalingsdagVisitor {
            override fun visit(
                dag: Utbetalingsdag.NavDag,
                dato: LocalDate,
                økonomi: Økonomi
            ) {
                fangetØkonomi = økonomi
            }
        })
        return fangetØkonomi
    }

    private fun createØkonomi(dagData: PersonData.ArbeidsgiverData.SykdomstidslinjeData.DagData): Økonomi {
        lateinit var fangetØkonomi: Økonomi
        dagData.parseDag(1.januar).accept(object : SykdomstidslinjeVisitor {
            override fun visitDag(
                dag: Dag.Sykedag,
                dato: LocalDate,
                økonomi: Økonomi,
                kilde: SykdomstidslinjeHendelse.Hendelseskilde
            ) {
                fangetØkonomi = økonomi
            }
        })
        return fangetØkonomi
    }
}
