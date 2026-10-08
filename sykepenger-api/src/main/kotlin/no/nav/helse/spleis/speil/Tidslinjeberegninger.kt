package no.nav.helse.spleis.speil

import no.nav.helse.spleis.speil.dto.*

internal fun List<Sykdomstidslinjedag>.merge(utbetalingstidslinje: List<Utbetalingstidslinjedag>): List<SammenslåttDag> {
    fun begrunnelser(utbetalingsdag: Utbetalingstidslinjedag) = if (utbetalingsdag is AvvistDag) utbetalingsdag.begrunnelser else null

    return map { sykdomsdag ->
        val utbetalingsdag = utbetalingstidslinje.find { it.dato.isEqual(sykdomsdag.dagen) }
        SammenslåttDag(
            sykdomsdag.dagen,
            sykdomsdag.type,
            utbetalingsdag?.type ?: UtbetalingstidslinjedagType.UkjentDag,
            kilde = sykdomsdag.kilde,
            grad = sykdomsdag.grad,
            utbetalingsinfo = utbetalingsdag?.utbetalingsinfo(),
            begrunnelser = utbetalingsdag?.let { begrunnelser(it) },
        )
    }
}
