package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Fødselsnummer
import no.nav.helse.hendelser.Hendelseskontekst
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.serde.api.BegrunnelseDTO

interface PersonObserver {
    data class VedtaksperiodeIkkeFunnetEvent(
        val vedtaksperiodeId: UUID
    )

    data class VedtaksperiodeEndretEvent(
        val gjeldendeTilstand: TilstandType,
        val forrigeTilstand: TilstandType,
        val aktivitetslogg: Map<String, List<Map<String, Any>>>,
        val harVedtaksperiodeWarnings: Boolean,
        val hendelser: Set<UUID>,
        val makstid: LocalDateTime
    )

    data class VedtaksperiodeAvbruttEvent(
        val gjeldendeTilstand: TilstandType,
    )

    data class OpprettOppgaveForSpeilsaksbehandlereEvent(
        val hendelser: Set<UUID>,
    )

    data class OpprettOppgaveEvent(
        val hendelser: Set<UUID>,
    )

    data class UtsettOppgaveEvent(
        val hendelse: UUID
    )

    data class RevurderingAvvistEvent(
        val fødselsnummer: String,
        val errors: List<String>
    ) {
        fun toJsonMap(): Map<String, Any> =
            mapOf(
                "fødselsnummer" to fødselsnummer,
                "errors" to errors
            )
    }

    data class ManglendeInntektsmeldingEvent(
        val fom: LocalDate,
        val tom: LocalDate
    )

    data class TrengerIkkeInntektsmeldingEvent(
        val fom: LocalDate,
        val tom: LocalDate
    )

    data class UtbetalingAnnullertEvent(
        val utbetalingId: UUID,
        val korrelasjonsId: UUID,
        val arbeidsgiverFagsystemId: String?,
        val personFagsystemId: String?,
        val fom: LocalDate,
        val tom: LocalDate,
        val utbetalingslinjer: List<Utbetalingslinje>,
        val annullertAvSaksbehandler: LocalDateTime,
        val saksbehandlerEpost: String,
        val saksbehandlerIdent: String
    ) {
        init {
            require(arbeidsgiverFagsystemId != null || personFagsystemId != null) {
                "Enten arbeidsgiverFagsystemId eller personfagsystemId må være satt"
            }
        }
        data class Utbetalingslinje(
            val fom: LocalDate,
            val tom: LocalDate,
            val beløp: Int,
            val grad: Double
        )
    }

    data class UtbetalingEndretEvent(
        val utbetalingId: UUID,
        val type: String,
        val forrigeStatus: String,
        val gjeldendeStatus: String,
        val arbeidsgiverOppdrag: Map<String, Any?>,
        val personOppdrag: Map<String, Any?>
    )

    data class UtbetalingUtbetaltEvent(
        val utbetalingId: UUID,
        val korrelasjonsId: UUID,
        val type: String,
        val fom: LocalDate,
        val tom: LocalDate,
        val maksdato: LocalDate,
        val forbrukteSykedager: Int,
        val gjenståendeSykedager: Int,
        val stønadsdager: Int,
        val epost: String,
        val tidspunkt: LocalDateTime,
        val automatiskBehandling: Boolean,
        val arbeidsgiverOppdrag: Map<String, Any?>,
        val personOppdrag: Map<String, Any?>,
        val utbetalingsdager: List<Utbetalingsdag>,
        val vedtaksperiodeIder: List<UUID>,
        val ident: String
    )

    data class Utbetalingsdag(
        val dato: LocalDate,
        val type: Dagtype,
        val begrunnelser: List<BegrunnelseDTO>? = null
    ) {
        enum class Dagtype {
            ArbeidsgiverperiodeDag,
            NavDag,
            NavHelgDag,
            Arbeidsdag,
            Fridag,
            AvvistDag,
            UkjentDag,
            ForeldetDag,
            Permisjonsdag,
            Feriedag
        }
    }

    data class FeriepengerUtbetaltEvent(
        val arbeidsgiverOppdrag: Map<String, Any?>,
        val personOppdrag: Map<String, Any> = mapOf("linjer" to emptyList<String>())
    )

    data class HendelseIkkeHåndtertEvent(
        val hendelseId: UUID,
        val årsaker: List<String>,
    )

    data class VedtakFattetEvent(
        val periode: Periode,
        val hendelseIder: Set<UUID>,
        val skjæringstidspunkt: LocalDate,
        val sykepengegrunnlag: Double,
        val grunnlagForSykepengegrunnlag: Double,
        val grunnlagForSykepengegrunnlagPerArbeidsgiver: Map<String, Double>,
        val inntekt: Double,
        val utbetalingId: UUID?,
        val sykepengegrunnlagsbegrensning: String,
        val vedtakFattetTidspunkt: LocalDateTime
    )

    fun inntektsmeldingReplay(fødselsnummer: Fødselsnummer, vedtaksperiodeId: UUID) {}
    fun vedtaksperiodePåminnet(hendelseskontekst: Hendelseskontekst, påminnelse: Påminnelse) {}
    fun vedtaksperiodeIkkePåminnet(hendelseskontekst: Hendelseskontekst, nåværendeTilstand: TilstandType) {}
    fun vedtaksperiodeEndret(hendelseskontekst: Hendelseskontekst, event: VedtaksperiodeEndretEvent) {}
    fun vedtaksperiodeAvbrutt(hendelseskontekst: Hendelseskontekst, event: VedtaksperiodeAvbruttEvent) {}
    fun opprettOppgaveForSpeilsaksbehandlere(hendelseskontekst: Hendelseskontekst, event: OpprettOppgaveForSpeilsaksbehandlereEvent) {}
    fun opprettOppgave(hendelseskontekst: Hendelseskontekst, event: OpprettOppgaveEvent) {}
    fun utsettOppgave(hendelseskontekst: Hendelseskontekst, event: UtsettOppgaveEvent) {}
    fun personEndret(hendelseskontekst: Hendelseskontekst) {}
    fun vedtaksperiodeIkkeFunnet(hendelseskontekst: Hendelseskontekst, vedtaksperiodeEvent: VedtaksperiodeIkkeFunnetEvent) {}
    fun manglerInntektsmelding(hendelseskontekst: Hendelseskontekst, orgnr: String, event: ManglendeInntektsmeldingEvent) {}
    fun trengerIkkeInntektsmelding(hendelseskontekst: Hendelseskontekst, event: TrengerIkkeInntektsmeldingEvent) {}
    fun utbetalingEndret(hendelseskontekst: Hendelseskontekst, event: UtbetalingEndretEvent) {}
    fun utbetalingUtbetalt(hendelseskontekst: Hendelseskontekst, event: UtbetalingUtbetaltEvent) {}
    fun utbetalingUtenUtbetaling(hendelseskontekst: Hendelseskontekst, event: UtbetalingUtbetaltEvent) {}
    fun feriepengerUtbetalt(hendelseskontekst: Hendelseskontekst, event: FeriepengerUtbetaltEvent) {}
    fun annullering(hendelseskontekst: Hendelseskontekst, event: UtbetalingAnnullertEvent) {}
    fun avstemt(hendelseskontekst: Hendelseskontekst, result: Map<String, Any>) {}
    fun hendelseIkkeHåndtert(hendelseskontekst: Hendelseskontekst, event: HendelseIkkeHåndtertEvent) {}
    fun vedtakFattet(hendelseskontekst: Hendelseskontekst, event: VedtakFattetEvent) {}
    fun revurderingAvvist(hendelseskontekst: Hendelseskontekst, event: RevurderingAvvistEvent) {}
}
