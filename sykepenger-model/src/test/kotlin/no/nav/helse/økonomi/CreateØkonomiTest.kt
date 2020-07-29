package no.nav.helse.økonomi

import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.serde.PersonData
import no.nav.helse.serde.PersonData.UtbetalingstidslinjeData
import no.nav.helse.serde.mapping.JsonDagType
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.testhelpers.januar
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.*

internal class CreateØkonomiTest {

    @Test
    fun `opprette bare prosenter`() {
        val data = sykdomstidslinjedag(79.5, 66.67)
        createØkonomi(data).also { økonomi ->
            økonomi.toMap().also { map ->
                assertEquals(79.5, map["grad"])
                assertEquals(66.67, map["arbeidsgiverBetalingProsent"])
                assertNull(map["dekningsgrunnlag"])
                assertNull(map["aktuellDagsinntekt"])
            }
            // Indirect test of Økonomi state is KunGrad
            assertThrows<IllegalStateException> { listOf(økonomi).betal(1.januar) }
            assertDoesNotThrow { økonomi.inntekt(1200) }
        }
    }

    @Test
    fun `opprette med bare inntekt`() {
        val data = utbetalingsdag(79.5, 66.67, 1500.0, 1199.6)
        createØkonomi(data).also { økonomi ->
            økonomi.toMap().also { map ->
                assertEquals(79.5, map["grad"])
                assertEquals(66.67, map["arbeidsgiverBetalingProsent"])
                assertEquals(1500.0, map["aktuellDagsinntekt"])
                assertEquals(1199.6, map["dekningsgrunnlag"])
                assertNull(map["arbeidsgiverbeløp"])
                assertNull(map["personbeløp"])
                assertNull(map["er6GBegrenset"])
            }
            // Indirect test of Økonomi state is HarLønn
            assertThrows<IllegalStateException> { økonomi.inntekt(1200) }
            assertDoesNotThrow { listOf(økonomi).betal(1.januar) }
        }
    }

    @Test
    fun `har betalinger`() {
        val data = utbetalingsdag(79.5, 66.67, 1500.0, 1199.6, 640, 320, true)
        createØkonomi(data).also { økonomi ->
            økonomi.toMap().also { map ->
                assertEquals(79.5, map["grad"])
                assertEquals(66.67, map["arbeidsgiverBetalingProsent"])
                assertEquals(1500.0, map["aktuellDagsinntekt"])
                assertEquals(1199.6, map["dekningsgrunnlag"])
                assertEquals(640, map["arbeidsgiverbeløp"])
                assertEquals(320, map["personbeløp"])
                assertTrue(map["er6GBegrenset"] as Boolean)
            }
            // Indirect test of Økonomi state
            assertThrows<IllegalStateException> { økonomi.inntekt(1200) }
            assertThrows<IllegalStateException> { listOf(økonomi).betal(1.januar) }
        }
    }

    private fun utbetalingsdag(
        grad: Double,
        arbeidsgiverBetalingProsent: Double,
        aktuellDagsinntekt: Double,
        dekningsgrunnlag: Double,
        arbeidsgiverbeløp: Int? = null,
        personbeløp: Int? = null,
        er6GBegrenset: Boolean = false
    ) = UtbetalingstidslinjeData.UtbetalingsdagData(
        UtbetalingstidslinjeData.TypeData.NavDag,
        1.januar,
        aktuellDagsinntekt,
        dekningsgrunnlag,
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
        er6GBegrenset: Boolean = false
    ) = PersonData.ArbeidsgiverData.SykdomstidslinjeData.DagData(
        1.januar,
        JsonDagType.SYKEDAG,
        PersonData.ArbeidsgiverData.SykdomstidslinjeData.KildeData("type", UUID.randomUUID()),
        grad,
        arbeidsgiverBetalingProsent,
        aktuellDagsinntekt,
        dekningsgrunnlag,
        arbeidsgiverbeløp,
        personbeløp,
        er6GBegrenset,
        null
    )

    private fun createØkonomi(dagData: UtbetalingstidslinjeData.UtbetalingsdagData): Økonomi {
        lateinit var _økonomi: Økonomi
        dagData.parseDag().accept(object : UtbetalingsdagVisitor {
            override fun visit(
                dag: Utbetalingstidslinje.Utbetalingsdag.NavDag,
                dato: LocalDate,
                økonomi: Økonomi
            ) {
                _økonomi = økonomi
            }
        })
        return _økonomi
    }

    private fun createØkonomi(dagData: PersonData.ArbeidsgiverData.SykdomstidslinjeData.DagData): Økonomi {
        lateinit var _økonomi: Økonomi
        dagData.parseDag().accept(object : SykdomstidslinjeVisitor {
            override fun visitDag(
                dag: Dag.Sykedag,
                dato: LocalDate,
                økonomi: Økonomi,
                grad: Prosentdel,
                arbeidsgiverBetalingProsent: Prosentdel,
                kilde: SykdomstidslinjeHendelse.Hendelseskilde
            ) {
                _økonomi = økonomi
            }
        })
        return _økonomi
    }
}
