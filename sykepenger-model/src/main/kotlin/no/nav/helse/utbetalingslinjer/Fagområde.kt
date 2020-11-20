package no.nav.helse.utbetalingslinjer

import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag.Companion.reflectedArbeidsgiverBeløp
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag.Companion.reflectedPersonBeløp
import no.nav.helse.økonomi.Økonomi

internal enum class Fagområde(
    internal val verdi: String,
    private val linjerStrategy: (Utbetaling) -> Oppdrag,
    private val beløpStrategy: (Økonomi) -> Int
) {
    SykepengerRefusjon("SPREF", Utbetaling::arbeidsgiverOppdrag, reflectedArbeidsgiverBeløp),
    Sykepenger("SP", Utbetaling::personOppdrag, reflectedPersonBeløp);

    override fun toString() = verdi

    internal fun beløp(økonomi: Økonomi) =
        beløpStrategy(økonomi)

    internal fun utbetalingslinjer(utbetaling: Utbetaling): Oppdrag =
        linjerStrategy(utbetaling)

    internal companion object {
        private val map = values().associateBy(Fagområde::verdi)
        fun from(verdi: String) = requireNotNull(map[verdi]) { "Støtter ikke klassekode: $verdi" }
    }
}
