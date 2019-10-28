package no.nav.helse.sykdomstidslinje

import no.nav.helse.sykdomstidslinje.dag.Dag
import no.nav.helse.sykdomstidslinje.dag.Sykedag
import java.math.BigDecimal

data class Syketilfelle(
    val arbeidsgiverperiode: Sykdomstidslinje?,
    val dagerEtterArbeidsgiverperiode: Sykdomstidslinje?
) {
    init {
        assert(arbeidsgiverperiode != null || dagerEtterArbeidsgiverperiode != null)
    }

    fun tilUtbetalingstidslinje(): List<Utbetalingsdag> =
        arbeidsgiverperiode.tilUtbetalingsdager(BigDecimal.ZERO, true) +
                dagerEtterArbeidsgiverperiode.tilUtbetalingsdager(BigDecimal.TEN, false)

    private fun Sykdomstidslinje?.tilUtbetalingsdager(dagsats: BigDecimal, arbeidsgiverperiode: Boolean) =
        this?.flatten()?.filterIsInstance<Sykedag>()?.map { Utbetalingsdag(dagsats, it, arbeidsgiverperiode) } ?: emptyList()


    val tidslinje
        get() = when {
            dagerEtterArbeidsgiverperiode != null -> arbeidsgiverperiode?.plus(dagerEtterArbeidsgiverperiode)
                ?: dagerEtterArbeidsgiverperiode
            arbeidsgiverperiode != null -> arbeidsgiverperiode
            else -> throw RuntimeException("Arbeidsgiverperiode og dager etter arbeidsgiverperiode er begge null. Syketilfellet inneholder ingen data")
        }
}

data class Utbetalingsdag(
    val dagsats: BigDecimal,
    val dag: Dag,
    val arbeidsgiverperiode: Boolean
    )