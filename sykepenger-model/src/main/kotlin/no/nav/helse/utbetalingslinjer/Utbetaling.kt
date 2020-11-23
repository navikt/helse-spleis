package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.*
import no.nav.helse.person.*
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.simulering
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.utbetaling
import no.nav.helse.utbetalingslinjer.Fagområde.Sykepenger
import no.nav.helse.utbetalingslinjer.Fagområde.SykepengerRefusjon
import no.nav.helse.utbetalingslinjer.Utbetaling.Status.*
import no.nav.helse.utbetalingstidslinje.Historie
import no.nav.helse.utbetalingstidslinje.Oldtidsutbetalinger
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
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
    private var annullert: Boolean,
    private val maksdato: LocalDate,
    private val forbrukteSykedager: Int?,
    private val gjenståendeSykedager: Int?,
    private var vurdering: Vurdering?,
    private var overføringstidspunkt: LocalDateTime?,
    private var avstemmingsnøkkel: Long?
) : Aktivitetskontekst {
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
        false,
        maksdato,
        forbrukteSykedager,
        gjenståendeSykedager,
        null,
        null,
        null
    )

    internal enum class Status {
        IKKE_UTBETALT,
        IKKE_GODKJENT,
        GODKJENT,
        SENDT,
        OVERFØRT,
        UTBETALT,
        UTBETALING_FEILET
    }

    internal fun erUtbetalt() = status == UTBETALT
    internal fun erFeilet() = status == UTBETALING_FEILET
    internal fun erAnnullert() = annullert

    internal fun håndter(hendelse: Utbetalingsgodkjenning) {
        hendelse.kontekst(this)
        status = if (hendelse.valider().hasErrorsOrWorse()) IKKE_GODKJENT else GODKJENT
        vurdering = hendelse.vurdering()
    }

    internal fun håndter(utbetaling: UtbetalingHendelse) {
        if (!utbetaling.erRelevant(arbeidsgiverOppdrag.fagsystemId())) return
        utbetaling.kontekst(this)
        status = if (utbetaling.hasErrorsOrWorse()) UTBETALING_FEILET else UTBETALT
        annullert = utbetaling.annullert
    }

    internal fun utbetalingFeilet(aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.kontekst(this)
        aktivitetslogg.error("Feilrespons fra oppdrag")
        status = UTBETALING_FEILET
    }

    internal fun håndter(utbetalingOverført: UtbetalingOverført) {
        if (!utbetalingOverført.erRelevant(arbeidsgiverOppdrag.fagsystemId())) return
        utbetalingOverført.kontekst(this)
        status = OVERFØRT
        overføringstidspunkt = utbetalingOverført.overføringstidspunkt
        avstemmingsnøkkel = utbetalingOverført.avstemmingsnøkkel
        utbetalingOverført.info(
            "Utbetalingen ble overført til Oppdrag/UR ${utbetalingOverført.overføringstidspunkt}, " +
                "og har fått avstemmingsnøkkel ${utbetalingOverført.avstemmingsnøkkel}"
        )
    }

    internal fun ferdigstill(
        hendelse: ArbeidstakerHendelse,
        person: Person,
        periode: Periode,
        sykepengegrunnlag: Inntekt,
        inntekt: Inntekt,
        hendelseIder: List<UUID>
    ) {
        // TODO: korte perioder uten utbetaling blir ikke utbetalt, men blir Avsluttet automatisk.
        // skal vi fortsatt drive å sende Utbetalt-event da?
        check(status in listOf(GODKJENT, UTBETALT)) { "Forventet status GODKJENT eller UTBETALT. Er $status" }
        val vurdering = checkNotNull(vurdering) { "Mangler vurdering" }
        vurdering.ferdigstill(hendelse, this, person, periode, sykepengegrunnlag, inntekt, hendelseIder)
    }

    internal fun arbeidsgiverOppdrag() = arbeidsgiverOppdrag

    internal fun personOppdrag() = personOppdrag

    override fun toSpesifikkKontekst() =
        SpesifikkKontekst("Utbetaling", mapOf("utbetalingId" to "$id"))

    companion object {

        private const val systemident = "SPLEIS"

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

    internal fun utbetalingstidslinje(periode: Periode) = utbetalingstidslinje.subset(periode)

    internal fun annuller(hendelse: AnnullerUtbetaling) = Utbetaling(
        UUID.randomUUID(),
        utbetalingstidslinje,
        arbeidsgiverOppdrag.emptied().minus(arbeidsgiverOppdrag, hendelse),
        personOppdrag.emptied().minus(personOppdrag, hendelse),
        LocalDateTime.now(),
        IKKE_UTBETALT,
        true,
        LocalDate.MAX,
        null,
        null,
        Vurdering(
            hendelse.saksbehandlerIdent,
            hendelse.saksbehandlerEpost,
            hendelse.opprettet,
            false
        ),
        null,
        null
    )

    internal fun append(organisasjonsnummer: String, oldtid: Oldtidsutbetalinger) {
        oldtid.add(organisasjonsnummer, utbetalingstidslinje)
    }

    internal fun append(organisasjonsnummer: String, bøtte: Historie.Historikkbøtte) {
        bøtte.add(organisasjonsnummer, utbetalingstidslinje)
    }

    internal fun utbetal(hendelse: ArbeidstakerHendelse) {
        val vurdering = requireNotNull(vurdering) { "Kan ikke overføre oppdrag som ikke er vurdert" }
        status = SENDT
        vurdering.utbetale(hendelse, arbeidsgiverOppdrag, maksdato, annullert)
    }

    internal fun simuler(hendelse: ArbeidstakerHendelse) {
        simulering(
            aktivitetslogg = hendelse,
            oppdrag = arbeidsgiverOppdrag.utenUendretLinjer(),
            maksdato = maksdato,
            saksbehandler = systemident
        )
    }

    internal fun valider(simulering: Simulering): IAktivitetslogg {
        return simulering.valider(arbeidsgiverOppdrag.utenUendretLinjer())
    }

    internal class Vurdering(
        private val ident: String,
        private val epost: String,
        private val tidspunkt: LocalDateTime,
        private val automatiskBehandling: Boolean
    ) {

        internal fun utbetale(aktivitetslogg: IAktivitetslogg, oppdrag: Oppdrag, maksdato: LocalDate, annullering: Boolean) {
            utbetaling(
                aktivitetslogg = aktivitetslogg,
                oppdrag = oppdrag,
                maksdato = maksdato.takeUnless { annullering },
                godkjenttidspunkt = tidspunkt,
                saksbehandler = ident,
                saksbehandlerEpost = epost,
                annullering = annullering
            )
        }

        fun ferdigstill(
            hendelse: ArbeidstakerHendelse,
            utbetaling: Utbetaling,
            person: Person,
            periode: Periode,
            sykepengegrunnlag: Inntekt,
            inntekt: Inntekt,
            hendelseIder: List<UUID>
        ) {
            person.vedtaksperiodeUtbetalt(
                tilUtbetaltEvent(
                    aktørId = hendelse.aktørId(),
                    fødselnummer = hendelse.fødselsnummer(),
                    orgnummer = hendelse.organisasjonsnummer(),
                    utbetaling = utbetaling,
                    utbetalingstidslinje = utbetaling.utbetalingstidslinje(periode),
                    sykepengegrunnlag = sykepengegrunnlag,
                    inntekt = inntekt,
                    forbrukteSykedager = requireNotNull(utbetaling.forbrukteSykedager),
                    gjenståendeSykedager = requireNotNull(utbetaling.gjenståendeSykedager),
                    godkjentAv = ident,
                    automatiskBehandling = automatiskBehandling,
                    hendelseIder = hendelseIder,
                    periode = periode,
                    maksdato = utbetaling.maksdato
                )
            )
        }
    }
}
