package no.nav.helse.utbetalingslinjer

import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag.Companion.reflectedArbeidsgiverBeløp
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag.Companion.reflectedPersonBeløp
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

internal enum class Fagområde(
    internal val verdi: String,
    private val beløpStrategy: (Økonomi) -> Int,
    private val klassekode: Klassekode
) {
    SykepengerRefusjon("SPREF", reflectedArbeidsgiverBeløp, Klassekode.RefusjonIkkeOpplysningspliktig),
    Sykepenger("SP", reflectedPersonBeløp, Klassekode.SykepengerArbeidstakerOrdinær);

    override fun toString() = verdi

    internal fun linje(fagsystemId: String, økonomi: Økonomi, dato: LocalDate, grad: Int, beløp: Int) =
        Utbetalingslinje(dato, dato, Satstype.DAG, beløpStrategy(økonomi), beløp, grad, fagsystemId, klassekode = klassekode)

    internal fun linje(fagsystemId: String, dato: LocalDate, grad: Int) =
        Utbetalingslinje(dato, dato, Satstype.DAG, null, 0, grad, fagsystemId, klassekode = klassekode)

    internal fun oppdaterLinje(linje: Utbetalingslinje, dato: LocalDate, økonomi: Økonomi, beløp: Int) {
        linje.beløp = beløpStrategy(økonomi)
        linje.aktuellDagsinntekt = beløp
        linje.fom = dato
    }

    internal fun kanLinjeUtvides(linje: Utbetalingslinje, økonomi: Økonomi, grad: Int) =
        grad == linje.grad && (linje.beløp == null || linje.beløp == beløpStrategy(økonomi))

    internal companion object {
        private val map = values().associateBy(Fagområde::verdi)
        fun from(verdi: String) = requireNotNull(map[verdi]) { "Støtter ikke klassekode: $verdi" }
    }
}
