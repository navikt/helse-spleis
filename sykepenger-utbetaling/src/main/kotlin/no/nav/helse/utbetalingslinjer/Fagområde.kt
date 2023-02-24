package no.nav.helse.utbetalingslinjer

import java.time.LocalDate

enum class Fagområde(
    val verdi: String,
    private val beløpStrategy: (Beløpkilde) -> Int,
    private val klassekode: Klassekode
) {
    SykepengerRefusjon("SPREF", { beløpkilde: Beløpkilde -> beløpkilde.arbeidsgiverbeløp() },
        Klassekode.RefusjonIkkeOpplysningspliktig
    ),
    Sykepenger("SP", { beløpkilde: Beløpkilde -> beløpkilde.personbeløp() }, Klassekode.SykepengerArbeidstakerOrdinær);

    override fun toString() = verdi

    fun linje(fagsystemId: String, økonomi: Beløpkilde, dato: LocalDate, grad: Int, beløp: Int) =
        Utbetalingslinje(dato, dato, Satstype.Daglig, beløpStrategy(økonomi), beløp, grad, fagsystemId, klassekode = klassekode)

    fun linje(fagsystemId: String, dato: LocalDate, grad: Int) =
        Utbetalingslinje(dato, dato, Satstype.Daglig, null, 0, grad, fagsystemId, klassekode = klassekode)

    fun utvidLinje(linje: Utbetalingslinje, dato: LocalDate, økonomi: Beløpkilde, beløp: Int) =
        linje.kopier(tom = dato, beløp = beløpStrategy(økonomi), aktuellDagsinntekt = beløp)

    fun kanLinjeUtvides(linje: Utbetalingslinje, økonomi: Beløpkilde, grad: Int) =
        grad == linje.grad && (linje.beløp == null || linje.beløp == beløpStrategy(økonomi))

    companion object {
        private val map = values().associateBy(Fagområde::verdi)
        fun from(verdi: String) = requireNotNull(map[verdi]) { "Støtter ikke klassekode: $verdi" }
    }
}
