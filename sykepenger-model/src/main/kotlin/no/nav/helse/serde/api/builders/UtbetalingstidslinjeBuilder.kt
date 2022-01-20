package no.nav.helse.serde.api.builders

import no.nav.helse.serde.api.*
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

internal class UtbetalingstidslinjeBuilder(
    private val utbetalingstidslinjeMap: MutableList<UtbetalingstidslinjedagDTO>
) : BuilderState() {

    internal fun build() = utbetalingstidslinjeMap.toList()

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        økonomi.medAvrundetData { _, aktuellDagsinntekt ->
            utbetalingstidslinjeMap.add(
                IkkeUtbetaltDagDTO(
                    type = DagtypeDTO.Arbeidsdag,
                    inntekt = aktuellDagsinntekt,
                    dato = dato
                )
            )
        }
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        økonomi.medAvrundetData { _, aktuellDagsinntekt ->
            utbetalingstidslinjeMap.add(
                IkkeUtbetaltDagDTO(
                    type = DagtypeDTO.ArbeidsgiverperiodeDag,
                    inntekt = aktuellDagsinntekt,
                    dato = dato
                )
            )
        }
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.NavDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        // TODO: Trenger speil _egentlig_ doubles?
        val (grad, totalGrad) = økonomi.medData { grad, totalGrad, _ -> grad to totalGrad }
        økonomi.medAvrundetData { _, arbeidsgiverRefusjonsbeløp, _, _, _, aktuellDagsinntekt, arbeidsgiverbeløp, personbeløp, _ ->
            utbetalingstidslinjeMap.add(
                NavDagDTO(
                    type = DagtypeDTO.NavDag,
                    inntekt = aktuellDagsinntekt,
                    dato = dato,
                    utbetaling = arbeidsgiverbeløp!!,
                    arbeidsgiverbeløp = arbeidsgiverbeløp,
                    personbeløp = personbeløp!!,
                    refusjonsbeløp = arbeidsgiverRefusjonsbeløp,
                    grad = grad,
                    totalGrad = totalGrad
                )
            )
        }
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        økonomi.medData { grad, _, _ ->
            utbetalingstidslinjeMap.add(
                NavHelgedagDTO(
                    type = DagtypeDTO.NavHelgDag,
                    inntekt = 0,   // Speil needs zero here
                    dato = dato,
                    grad = grad
                )
            )
        }
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.Fridag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        utbetalingstidslinjeMap.add(
            IkkeUtbetaltDagDTO(
                type = if (dato.erHelg()) DagtypeDTO.Helgedag else DagtypeDTO.Feriedag,
                inntekt = 0,    // Speil needs zero here
                dato = dato
            )
        )
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.UkjentDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        utbetalingstidslinjeMap.add(
            IkkeUtbetaltDagDTO(
                type = DagtypeDTO.UkjentDag,
                inntekt = 0,    // Speil needs zero here
                dato = dato
            )
        )
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        utbetalingstidslinjeMap.add(
            AvvistDagDTO(
                type = DagtypeDTO.AvvistDag,
                inntekt = 0,    // Speil needs zero here
                dato = dato,
                begrunnelser = dag.begrunnelser.map { BegrunnelseDTO.fraBegrunnelse(it) },
                grad = 0.0 // Speil wants zero here
            )
        )
    }

    override fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.ForeldetDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        utbetalingstidslinjeMap.add(
            IkkeUtbetaltDagDTO(
                type = DagtypeDTO.ForeldetDag,
                inntekt = 0,    // Speil needs zero here
                dato = dato
            )
        )
    }

    override fun postVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) {
        popState()
    }
}
