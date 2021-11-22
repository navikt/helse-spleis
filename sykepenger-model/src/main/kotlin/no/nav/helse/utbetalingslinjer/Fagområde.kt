package no.nav.helse.utbetalingslinjer

import no.nav.helse.Toggle
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.*
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag.Companion.reflectedArbeidsgiverBeløp
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag.Companion.reflectedPersonBeløp
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

private fun behovStrategy(nårToggleErDisabled: Behovtype, nårToggleErEnabled: Behovtype): () -> Behovtype = {
    if (Toggle.NyeBehovForUtbetaling.enabled) nårToggleErEnabled else nårToggleErDisabled
}

internal enum class Fagområde(
    internal val verdi: String,
    private val beløpStrategy: (Økonomi) -> Int,
    private val klassekode: Klassekode,
    private val behovtypeUtbetaling: () -> Behovtype,
    private val behovtypeSimulering: () -> Behovtype
) {
    SykepengerRefusjon("SPREF", reflectedArbeidsgiverBeløp, Klassekode.RefusjonIkkeOpplysningspliktig, behovStrategy(Utbetaling, UtbetalingArbeidsgiver), behovStrategy(Simulering, SimuleringArbeidsgiver)),
    Sykepenger("SP", reflectedPersonBeløp, Klassekode.SykepengerArbeidstakerOrdinær, behovStrategy(Utbetaling, UtbetalingPerson), behovStrategy(Simulering, SimuleringPerson));

    override fun toString() = verdi

    internal fun linje(fagsystemId: String, økonomi: Økonomi, dato: LocalDate, grad: Double, beløp: Int) =
        Utbetalingslinje(dato, dato, Satstype.DAG, beløpStrategy(økonomi), beløp, grad, fagsystemId, klassekode = klassekode)

    internal fun linje(fagsystemId: String, dato: LocalDate, grad: Double) =
        Utbetalingslinje(dato, dato, Satstype.DAG, null, 0, grad, fagsystemId, klassekode = klassekode)

    internal fun oppdaterLinje(linje: Utbetalingslinje, dato: LocalDate, økonomi: Økonomi, beløp: Int) {
        linje.beløp = beløpStrategy(økonomi)
        linje.aktuellDagsinntekt = beløp
        linje.fom = dato
    }

    internal fun kanLinjeUtvides(linje: Utbetalingslinje, økonomi: Økonomi, grad: Double) =
        grad == linje.grad && (linje.beløp == null || linje.beløp == beløpStrategy(økonomi))

    internal fun overfør(aktivitetslogg: IAktivitetslogg, oppdragdetaljer: Map<String, Any?>) {
        aktivitetslogg.behov(behovtypeUtbetaling(), "Trenger å sende utbetaling til Oppdrag", oppdragdetaljer)
    }

    internal fun simuler(aktivitetslogg: IAktivitetslogg, oppdragdetaljer: Map<String, Any>) {
        aktivitetslogg.behov(behovtypeSimulering(), "Trenger simulering fra Oppdragssystemet", oppdragdetaljer)
    }

    internal companion object {
        private val map = values().associateBy(Fagområde::verdi)
        fun from(verdi: String) = requireNotNull(map[verdi]) { "Støtter ikke klassekode: $verdi" }
    }
}
