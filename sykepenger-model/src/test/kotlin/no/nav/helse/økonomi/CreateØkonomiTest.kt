package no.nav.helse.økonomi

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.serde.PersonData
import no.nav.helse.serde.PersonData.ArbeidsgiverData.PeriodeData
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
import java.util.*

internal class CreateØkonomiTest {

    @Test
    fun `opprette bare prosenter`() {
        val data = sykdomstidslinjedag(79.5)
        createØkonomi(data).also { økonomi ->
            økonomi.medData { grad, arbeidsgiverRefusjonsbeløp, dekningsgrunnlag, _, _, aktuellDagsinntekt, _, _, _ ->
                assertEquals(79.5, grad)
                assertEquals(0.0, arbeidsgiverRefusjonsbeløp)
                assertNull(dekningsgrunnlag)
                assertNull(aktuellDagsinntekt)
            }
            // Indirect test of Økonomi state is KunGrad
            assertThrows<IllegalStateException> { listOf(økonomi).betal(1.januar) }
            assertThrows<IllegalStateException> { økonomi.inntekt(1200.daglig, skjæringstidspunkt = 1.januar) }
            assertDoesNotThrow { økonomi.arbeidsgiverperiode(null).inntekt(1200.daglig, skjæringstidspunkt = 1.januar) }
        }
    }

    @Test
    fun `opprette med bare inntekt`() {
        val data = utbetalingsdag(80.0, 420.0, 1500.0, 1199.6)
        createØkonomi(data).also { økonomi ->
            økonomi.medData { grad,
                              arbeidsgiverRefusjonsbeløp,
                              dekningsgrunnlag,
                              _,
                              _,
                              aktuellDagsinntekt,
                              arbeidsgiverbeløp,
                              personbeløp,
                              er6GBegrenset ->
                assertEquals(80.0, grad)
                assertEquals(420.0, arbeidsgiverRefusjonsbeløp)
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
        val data = utbetalingsdag(79.5, 420.0, 1500.0, 1199.6, 640.0, 320.0, null, true)
        createØkonomi(data).also { økonomi ->
            økonomi.medData { grad,
                              arbeidsgiverRefusjon,
                              dekningsgrunnlag,
                              _,
                              _,
                              aktuellDagsinntekt,
                              arbeidsgiverbeløp,
                              personbeløp,
                              er6GBegrenset ->
                assertEquals(79.5, grad)
                assertEquals(420.0, arbeidsgiverRefusjon)
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
        arbeidsgiverRefusjonsbeløp: Double,
        aktuellDagsinntekt: Double,
        dekningsgrunnlag: Double,
        arbeidsgiverbeløp: Double? = null,
        personbeløp: Double? = null,
        totalGrad: Prosentdel? = null,
        er6GBegrenset: Boolean = false,
        arbeidsgiverperiode: List<Periode>? = null
    ) = UtbetalingstidslinjeData.UtbetalingsdagData(
        UtbetalingstidslinjeData.TypeData.NavDag,
        arbeidsgiverperiode?.map { PeriodeData(it.start, it.endInclusive) },
        aktuellDagsinntekt,
        dekningsgrunnlag,
        1.januar,
        null,
        totalGrad?.toDouble(),
        null,
        null,
        grad,
        arbeidsgiverRefusjonsbeløp,
        arbeidsgiverbeløp,
        personbeløp,
        er6GBegrenset
    )

    private fun sykdomstidslinjedag(
        grad: Double,
    ) = PersonData.ArbeidsgiverData.SykdomstidslinjeData.DagData(
        PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.SYKEDAG,
        PersonData.ArbeidsgiverData.SykdomstidslinjeData.KildeData("type", UUID.randomUUID(), 1.januar.atStartOfDay()),
        grad,
        0.0,
        null,
        null,
        null,
        null,
        null,
        null,
        false,
        null
    )

    private fun createØkonomi(dagData: UtbetalingstidslinjeData.UtbetalingsdagData): Økonomi {
        lateinit var fangetØkonomi: Økonomi
        dagData.parseDag(1.januar).accept(object : UtbetalingsdagVisitor {
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
