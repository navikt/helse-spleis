package no.nav.helse.utbetalingslinjer

import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag.Companion.reflectedArbeidsgiverBeløp
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag.Companion.reflectedPersonBeløp
import java.time.LocalDate

interface Beløpkilde {
    fun arbeidsgiverbeløp(): Int
    fun personbeløp(): Int
}

internal enum class Fagområde(
    internal val verdi: String,
    private val beløpStrategy: (Beløpkilde) -> Int,
    private val klassekode: Klassekode
) {
    SykepengerRefusjon("SPREF", reflectedArbeidsgiverBeløp, Klassekode.RefusjonIkkeOpplysningspliktig),
    Sykepenger("SP", reflectedPersonBeløp, Klassekode.SykepengerArbeidstakerOrdinær);

    override fun toString() = verdi

    internal fun linje(fagsystemId: String, økonomi: Beløpkilde, dato: LocalDate, grad: Int, beløp: Int) =
        Utbetalingslinje(dato, dato, Satstype.Daglig, beløpStrategy(økonomi), beløp, grad, fagsystemId, klassekode = klassekode)

    internal fun linje(fagsystemId: String, dato: LocalDate, grad: Int) =
        Utbetalingslinje(dato, dato, Satstype.Daglig, null, 0, grad, fagsystemId, klassekode = klassekode)

    internal fun oppdaterLinje(linje: Utbetalingslinje, dato: LocalDate, økonomi: Beløpkilde, beløp: Int) {
        linje.beløp = beløpStrategy(økonomi)
        linje.aktuellDagsinntekt = beløp
        linje.fom = dato
    }

    internal fun kanLinjeUtvides(linje: Utbetalingslinje, økonomi: Beløpkilde, grad: Int) =
        grad == linje.grad && (linje.beløp == null || linje.beløp == beløpStrategy(økonomi))

    internal companion object {
        private val map = values().associateBy(Fagområde::verdi)
        fun from(verdi: String) = requireNotNull(map[verdi]) { "Støtter ikke klassekode: $verdi" }
    }
}
