package no.nav.helse.serde.reflection

import no.nav.helse.person.PersonObserver
import no.nav.helse.serde.PersonData
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje

internal class UtbetalingsdagerReflect(val utbetalingstidslinje: Utbetalingstidslinje) {

    fun toList(): List<PersonObserver.Utbetalingsdag> {
        return utbetalingstidslinje.map { utbetalingsdag ->
            PersonObserver.Utbetalingsdag(
                utbetalingsdag.dato,
                mapDagtype(utbetalingsdag),
                mapBegrunnelser(utbetalingsdag)
            )
        }
    }

    private fun mapBegrunnelser(utbetalingsdag: Utbetalingstidslinje.Utbetalingsdag) =
        if (utbetalingsdag is Utbetalingstidslinje.Utbetalingsdag.AvvistDag)
            utbetalingsdag.begrunnelser.map { begrunnelse -> begrunnelse.javaClass.simpleName }
        else null

    private fun mapDagtype(it: Utbetalingstidslinje.Utbetalingsdag) = when (it::class) {
        Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag::class -> PersonData.UtbetalingstidslinjeData.TypeData.Arbeidsdag.name
        Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag::class -> PersonData.UtbetalingstidslinjeData.TypeData.ArbeidsgiverperiodeDag.name
        Utbetalingstidslinje.Utbetalingsdag.NavDag::class -> PersonData.UtbetalingstidslinjeData.TypeData.NavDag.name
        Utbetalingstidslinje.Utbetalingsdag.NavHelgDag::class -> PersonData.UtbetalingstidslinjeData.TypeData.NavHelgDag.name
        Utbetalingstidslinje.Utbetalingsdag.Fridag::class -> PersonData.UtbetalingstidslinjeData.TypeData.Fridag.name
        Utbetalingstidslinje.Utbetalingsdag.UkjentDag::class -> PersonData.UtbetalingstidslinjeData.TypeData.UkjentDag.name
        Utbetalingstidslinje.Utbetalingsdag.AvvistDag::class -> PersonData.UtbetalingstidslinjeData.TypeData.AvvistDag.name
        Utbetalingstidslinje.Utbetalingsdag.ForeldetDag::class -> PersonData.UtbetalingstidslinjeData.TypeData.ForeldetDag.name
        else -> throw IllegalStateException("Uventet utbetalingsdag")
    }
}

