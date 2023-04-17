package no.nav.helse.serde.api.speil

import no.nav.helse.serde.api.dto.AvvistDag
import no.nav.helse.serde.api.dto.SammenslåttDag
import no.nav.helse.serde.api.dto.Sykdomstidslinjedag
import no.nav.helse.serde.api.dto.Utbetalingstidslinjedag
import no.nav.helse.serde.api.dto.UtbetalingstidslinjedagType

internal fun List<Sykdomstidslinjedag>.merge(utbetalingstidslinje: List<Utbetalingstidslinjedag>): List<SammenslåttDag> {

    fun begrunnelser(utbetalingsdag: Utbetalingstidslinjedag) =
        if (utbetalingsdag is AvvistDag) utbetalingsdag.begrunnelser else null

    return map { sykdomsdag ->
        val utbetalingsdag = utbetalingstidslinje.find { it.dato.isEqual(sykdomsdag.dagen) }
        SammenslåttDag(
            sykdomsdag.dagen,
            sykdomsdag.type,
            utbetalingsdag?.type ?: UtbetalingstidslinjedagType.UkjentDag,
            kilde = sykdomsdag.kilde,
            grad = sykdomsdag.grad,
            utbetalingsinfo = utbetalingsdag?.utbetalingsinfo(),
            begrunnelser = utbetalingsdag?.let { begrunnelser(it) }
        )
    }
}
