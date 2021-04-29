package no.nav.helse.økonomi

import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.serde.PersonData
import no.nav.helse.serde.PersonData.UtbetalingstidslinjeData
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.testhelpers.januar
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class CreateØkonomiTest {

    @Test
    fun `opprette bare prosenter`() {
        val data = sykdomstidslinjedag(79.5, 66.67)
        createØkonomi(data).also { økonomi ->
            økonomi.reflection { grad, arbeidsgiverBetalingProsent, dekningsgrunnlag, _, _, aktuellDagsinntekt, _, _, _ ->
                assertEquals(79.5, grad)
                assertEquals(66.67, arbeidsgiverBetalingProsent)
                assertNull(dekningsgrunnlag)
                assertNull(aktuellDagsinntekt)
            }
            // Indirect test of Økonomi state is KunGrad
            assertThrows<IllegalStateException> { listOf(økonomi).betal(1.januar) }
            assertDoesNotThrow { økonomi.inntekt(1200.daglig, skjæringstidspunkt = 1.januar) }
        }
    }

    @Test
    fun `opprette med bare inntekt`() {
        val data = utbetalingsdag(79.5, 66.67, 1500.0, 1199.6)
        createØkonomi(data).also { økonomi ->
            økonomi.reflection {
                    grad,
                    arbeidsgiverBetalingProsent,
                    dekningsgrunnlag,
                    _,
                    _,
                    aktuellDagsinntekt,
                    arbeidsgiverbeløp,
                    personbeløp,
                    er6GBegrenset ->
                assertEquals(79.5, grad)
                assertEquals(66.67, arbeidsgiverBetalingProsent)
                assertEquals(1500.0, aktuellDagsinntekt)
                assertEquals(1199.6, dekningsgrunnlag)
                assertNull(arbeidsgiverbeløp)
                assertNull(personbeløp)
                assertNull(er6GBegrenset)
            }
            // Indirect test of Økonomi state is HarLønn
            assertThrows<IllegalStateException> { økonomi.inntekt(1200.daglig, skjæringstidspunkt = 1.januar) }
            assertDoesNotThrow { listOf(økonomi).betal(1.januar) }
        }
    }

    @Test
    fun `har betalinger`() {
        val data = utbetalingsdag(79.5, 66.67, 1500.0, 1199.6, 640.0, 320.0, null, true)
        createØkonomi(data).also { økonomi ->
            økonomi.reflection {
                    grad,
                    arbeidsgiverBetalingProsent,
                    dekningsgrunnlag,
                    _,
                    _,
                    aktuellDagsinntekt,
                    arbeidsgiverbeløp,
                    personbeløp,
                    er6GBegrenset ->
                assertEquals(79.5, grad)
                assertEquals(66.67, arbeidsgiverBetalingProsent)
                assertEquals(1500.0, aktuellDagsinntekt)
                assertEquals(1199.6, dekningsgrunnlag)
                assertEquals(640.0, arbeidsgiverbeløp)
                assertEquals(320.0, personbeløp)
                assertTrue(er6GBegrenset as Boolean)
            }
            // Indirect test of Økonomi state
            assertThrows<IllegalStateException> { økonomi.inntekt(1200.daglig, skjæringstidspunkt = 1.januar) }
            assertDoesNotThrow { listOf(økonomi).betal(1.januar) }
        }
    }

    private fun utbetalingsdag(
        grad: Double,
        arbeidsgiverBetalingProsent: Double,
        aktuellDagsinntekt: Double,
        dekningsgrunnlag: Double,
        arbeidsgiverbeløp: Double? = null,
        personbeløp: Double? = null,
        totalGrad: Prosentdel? = null,
        er6GBegrenset: Boolean = false
    ) = UtbetalingstidslinjeData.UtbetalingsdagData(
        UtbetalingstidslinjeData.TypeData.NavDag,
        1.januar,
        aktuellDagsinntekt,
        dekningsgrunnlag,
        1.januar,
        totalGrad?.toDouble(),
        null,
        null,
        grad,
        arbeidsgiverBetalingProsent,
        arbeidsgiverbeløp,
        personbeløp,
        er6GBegrenset
    )

    private fun sykdomstidslinjedag(
        grad: Double,
        arbeidsgiverBetalingProsent: Double,
        aktuellDagsinntekt: Double? = null,
        dekningsgrunnlag: Double? = null,
        arbeidsgiverbeløp: Int? = null,
        personbeløp: Int? = null,
        er6GBegrenset: Boolean = false,
        totalGrad: Double? = null
    ) = PersonData.ArbeidsgiverData.SykdomstidslinjeData.DagData(
        1.januar,
        PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.SYKEDAG,
        PersonData.ArbeidsgiverData.SykdomstidslinjeData.KildeData("type", UUID.randomUUID(), LocalDateTime.now()),
        grad,
        arbeidsgiverBetalingProsent,
        aktuellDagsinntekt,
        dekningsgrunnlag,
        null,
        totalGrad,
        arbeidsgiverbeløp,
        personbeløp,
        er6GBegrenset,
        null
    )

    private fun createØkonomi(dagData: UtbetalingstidslinjeData.UtbetalingsdagData): Økonomi {
        lateinit var fangetØkonomi: Økonomi
        dagData.parseDag().accept(object : UtbetalingsdagVisitor {
            override fun visit(
                dag: Utbetalingstidslinje.Utbetalingsdag.NavDag,
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
        dagData.parseDag().accept(object : SykdomstidslinjeVisitor {
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
