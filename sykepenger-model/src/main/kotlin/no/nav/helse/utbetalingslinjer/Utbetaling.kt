package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.UtbetalingVisitor
import no.nav.helse.utbetalingslinjer.Fagområde.Sykepenger
import no.nav.helse.utbetalingslinjer.Fagområde.SykepengerRefusjon
import no.nav.helse.utbetalingslinjer.Utbetaling.Status.*
import no.nav.helse.utbetalingstidslinje.Historie
import no.nav.helse.utbetalingstidslinje.Oldtidsutbetalinger
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

// Understands related payment activities for an Arbeidsgiver
internal class Utbetaling private constructor(
    private val id: UUID,
    private val utbetalingstidslinje: Utbetalingstidslinje,
    private val arbeidsgiverOppdrag: Oppdrag,
    private val personOppdrag: Oppdrag,
    private val tidsstempel: LocalDateTime,
    private var status: Status,
    private var annullert: Boolean
) {
    internal constructor(
        fødselsnummer: String,
        organisasjonsnummer: String,
        utbetalingstidslinje: Utbetalingstidslinje,
        sisteDato: LocalDate,
        aktivitetslogg: IAktivitetslogg,
        maksdato: LocalDate,
        forbrukteSykedager: Int,
        gjenståendeSykedager: Int,
        utbetalinger: List<Utbetaling>
    ) : this(
        UUID.randomUUID(),
        utbetalingstidslinje,
        buildArb(organisasjonsnummer, utbetalingstidslinje, sisteDato, aktivitetslogg, maksdato, forbrukteSykedager, gjenståendeSykedager, utbetalinger),
        buildPerson(fødselsnummer, utbetalingstidslinje, sisteDato, aktivitetslogg, maksdato, forbrukteSykedager, gjenståendeSykedager, utbetalinger),
        LocalDateTime.now(),
        IKKE_UTBETALT,
        false
    )

    internal enum class Status {
        IKKE_UTBETALT,
        UTBETALT,
        UTBETALING_FEILET
    }

    internal fun erUtbetalt() = status == UTBETALT
    internal fun erFeilet() = status == UTBETALING_FEILET
    internal fun erAnnullert() = annullert

    internal fun håndter(utbetaling: UtbetalingHendelse) {
        status = if (utbetaling.hasErrorsOrWorse()) UTBETALING_FEILET else UTBETALT
        annullert = utbetaling.annullert
    }

    internal fun arbeidsgiverOppdrag() = arbeidsgiverOppdrag

    internal fun personOppdrag() = personOppdrag

    companion object {

        private fun buildArb(
            organisasjonsnummer: String,
            tidslinje: Utbetalingstidslinje,
            sisteDato: LocalDate,
            aktivitetslogg: IAktivitetslogg,
            maksdato: LocalDate,
            forbrukteSykedager: Int,
            gjenståendeSykedager: Int,
            utbetalinger: List<Utbetaling>
        ) = OppdragBuilder(tidslinje, organisasjonsnummer, SykepengerRefusjon, sisteDato)
            .result()
            .minus(sisteGyldig(utbetalinger) { Oppdrag(organisasjonsnummer, SykepengerRefusjon) }, aktivitetslogg)
            .also { oppdrag ->
                utbetalinger.lastOrNull()?.arbeidsgiverOppdrag?.also { oppdrag.nettoBeløp(it) }
                aktivitetslogg.info(
                    if (oppdrag.isEmpty()) "Ingen utbetalingslinjer bygget"
                    else "Utbetalingslinjer bygget vellykket"
                )
            }

        private fun buildPerson(        // TODO("To be completed when payments to employees is supported")
            fødselsnummer: String,
            tidslinje: Utbetalingstidslinje,
            sisteDato: LocalDate,
            aktivitetslogg: IAktivitetslogg,
            maksdato: LocalDate,
            forbrukteSykedager: Int,
            gjenståendeSykedager: Int,
            utbetalinger: List<Utbetaling>
        ) = Oppdrag(fødselsnummer, Sykepenger)

        private fun sisteGyldig(utbetalinger: List<Utbetaling>, default: () -> Oppdrag) =
            utbetalinger
                .utbetalte()
                .lastOrNull()
                ?.arbeidsgiverOppdrag
                ?: default()

        internal fun List<Utbetaling>.utbetalte() =
            filterNot { harAnnullerte(it.arbeidsgiverOppdrag.fagsystemId()) }
                .filter { it.status == UTBETALT }

        private fun List<Utbetaling>.harAnnullerte(fagsystemId: String) =
            filter { it.arbeidsgiverOppdrag.fagsystemId() == fagsystemId }
                .any { it.annullert }
    }

    internal fun accept(visitor: UtbetalingVisitor) {
        visitor.preVisitUtbetaling(this, tidsstempel)
        utbetalingstidslinje.accept(visitor)
        visitor.preVisitArbeidsgiverOppdrag(arbeidsgiverOppdrag)
        arbeidsgiverOppdrag.accept(visitor)
        visitor.postVisitArbeidsgiverOppdrag(arbeidsgiverOppdrag)
        visitor.preVisitPersonOppdrag(personOppdrag)
        personOppdrag.accept(visitor)
        visitor.postVisitPersonOppdrag(personOppdrag)
        visitor.postVisitUtbetaling(this, tidsstempel)
    }

    internal fun utbetalingstidslinje() = utbetalingstidslinje

    internal fun annuller(aktivitetslogg: Aktivitetslogg) = Utbetaling(
        UUID.randomUUID(),
        utbetalingstidslinje,
        arbeidsgiverOppdrag.emptied().minus(arbeidsgiverOppdrag, aktivitetslogg),
        personOppdrag.emptied().minus(personOppdrag, aktivitetslogg),
        LocalDateTime.now(),
        IKKE_UTBETALT,
        true
    )

    internal fun append(organisasjonsnummer: String, oldtid: Oldtidsutbetalinger) {
        oldtid.add(organisasjonsnummer, utbetalingstidslinje)
    }

    internal fun append(organisasjonsnummer: String, bøtte: Historie.Historikkbøtte) {
        bøtte.add(organisasjonsnummer, utbetalingstidslinje)
    }
}
