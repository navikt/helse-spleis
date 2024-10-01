package no.nav.helse.utbetalingslinjer

import java.time.LocalDate
import no.nav.helse.økonomi.Økonomi

enum class Fagområde(
    val verdi: String,
    private val klassekode: Klassekode
) {
    SykepengerRefusjon("SPREF", Klassekode.RefusjonIkkeOpplysningspliktig),
    Sykepenger("SP", Klassekode.SykepengerArbeidstakerOrdinær);

    override fun toString() = verdi

    fun linje(fagsystemId: String, økonomi: Økonomi, dato: LocalDate, grad: Int) =
        Utbetalingslinje(dato, dato, Satstype.Daglig, økonomi.dagligBeløpForFagområde(this), grad, fagsystemId, klassekode = klassekode)

    fun linje(fagsystemId: String, dato: LocalDate, grad: Int) =
        Utbetalingslinje(dato, dato, Satstype.Daglig, null, grad, fagsystemId, klassekode = klassekode)

    fun utvidLinje(linje: Utbetalingslinje, dato: LocalDate, økonomi: Økonomi) =
        linje.kopier(tom = dato, beløp = økonomi.dagligBeløpForFagområde(this))

    fun kanLinjeUtvides(linje: Utbetalingslinje, økonomi: Økonomi, grad: Int) =
        grad == linje.grad && (linje.beløp == null || linje.beløp == økonomi.dagligBeløpForFagområde(this))

    companion object {
        private val map = entries.associateBy(Fagområde::verdi)
        fun from(verdi: String) = requireNotNull(map[verdi]) { "Støtter ikke klassekode: $verdi" }
    }
}
