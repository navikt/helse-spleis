package no.nav.helse.person

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Påminnelse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

interface PersonObserver {
    data class PersonEndretEvent(
        val aktørId: String,
        val person: Person,
        val fødselsnummer: String
    )

    data class VedtaksperiodeReplayEvent(
        val vedtaksperiodeId: UUID,
        val aktørId: String,
        val fødselsnummer: String,
        val organisasjonsnummer: String,
        val hendelseIder: List<UUID>
    )

    data class InntektsmeldingReplayEvent(
        val fnr: String,
        val vedtaksperiodeId: UUID
    )

    data class VedtaksperiodeIkkeFunnetEvent(
        val vedtaksperiodeId: UUID,
        val aktørId: String,
        val fødselsnummer: String,
        val organisasjonsnummer: String
    )

    data class VedtaksperiodeEndretEvent(
        val vedtaksperiodeId: UUID,
        val aktørId: String,
        val fødselsnummer: String,
        val organisasjonsnummer: String,
        val gjeldendeTilstand: TilstandType,
        val forrigeTilstand: TilstandType,
        val aktivitetslogg: Map<String, List<Map<String, Any>>>,
        val harVedtaksperiodeWarnings: Boolean,
        val hendelser: Set<UUID>,
        val makstid: LocalDateTime
    )

    data class VedtaksperiodeAvbruttEvent(
        val vedtaksperiodeId: UUID,
        val aktørId: String,
        val fødselsnummer: String,
        val organisasjonsnummer: String,
        val gjeldendeTilstand: TilstandType
    )

    data class UtbetaltEvent(
        val hendelser: Set<UUID>,
        val utbetalingId: UUID,
        val oppdrag: List<Utbetalt>,
        val ikkeUtbetalteDager: List<IkkeUtbetaltDag>,
        val fom: LocalDate,
        val tom: LocalDate,
        val forbrukteSykedager: Int,
        val gjenståendeSykedager: Int,
        val godkjentAv: String,
        val automatiskBehandling: Boolean,
        val opprettet: LocalDateTime,
        val sykepengegrunnlag: Double,
        val månedsinntekt: Double,
        val maksdato: LocalDate
    ) {
        data class Utbetalt(
            val mottaker: String,
            val fagområde: String,
            val fagsystemId: String,
            val totalbeløp: Int,
            val utbetalingslinjer: List<Utbetalingslinje>
        ) {
            data class Utbetalingslinje(
                val fom: LocalDate,
                val tom: LocalDate,
                val sats: Int,
                val beløp: Int,
                val grad: Double,
                val sykedager: Int
            )
        }

        data class IkkeUtbetaltDag(
            val dato: LocalDate,
            val type: Type,
            val begrunnelser: List<Begrunnelse>? = null
        ) {
            enum class Type {
                Annullering,
                Fridag,
                Arbeidsdag,
                AvvistDag
            }
            enum class Begrunnelse {
                SykepengedagerOppbrukt,
                MinimumInntekt,
                EgenmeldingUtenforArbeidsgiverperiode,
                MinimumSykdomsgrad,
                EtterDødsdato,
                ManglerMedlemskap,
                ManglerOpptjening
            }
        }
    }

    data class ManglendeInntektsmeldingEvent(
        val vedtaksperiodeId: UUID,
        val fødselsnummer: String,
        val organisasjonsnummer: String,
        val fom: LocalDate,
        val tom: LocalDate
    )

    data class TrengerIkkeInntektsmeldingEvent(
        val vedtaksperiodeId: UUID,
        val fødselsnummer: String,
        val organisasjonsnummer: String,
        val fom: LocalDate,
        val tom: LocalDate
    )

    data class UtbetalingAnnullertEvent(
        val utbetalingId: UUID,
        val fagsystemId: String,
        val fom: LocalDate,
        val tom: LocalDate,
        val utbetalingslinjer: List<Utbetalingslinje>,
        val annullertAvSaksbehandler: LocalDateTime,
        val saksbehandlerEpost: String,
        val saksbehandlerIdent: String
    ) {
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
        val arbeidsgiverOppdrag: Map<String, Any>,
        val personOppdrag: Map<String, Any>
    )

    data class UtbetalingUtbetaltEvent(
        val utbetalingId: UUID,
        val type: String,
        val fom: LocalDate,
        val tom: LocalDate,
        val maksdato: LocalDate,
        val forbrukteSykedager: Int,
        val gjenståendeSykedager: Int,
        val ident: String,
        val epost: String,
        val tidspunkt: LocalDateTime,
        val automatiskBehandling: Boolean,
        val arbeidsgiverOppdrag: Map<String, Any>,
        val personOppdrag: Map<String, Any>,
        val utbetalingsdager: List<Utbetalingsdag>,
    )

    data class Utbetalingsdag(
        val dato: LocalDate,
        val type: String,
        val begrunnelser: List<String>?
    )

    data class FeriepengerUtbetaltEvent(
        val arbeidsgiverOppdrag: Map<String, Any>,
        val personOppdrag: Map<String, Any> = mapOf("linjer" to emptyList<String>())
    )

    data class HendelseIkkeHåndtertEvent(
        val hendelseId: UUID
    )

    data class VedtakFattetEvent(
        val vedtaksperiodeId: UUID,
        val periode: Periode,
        val hendelseIder: Set<UUID>,
        val skjæringstidspunkt: LocalDate,
        val sykepengegrunnlag: Double,
        val inntekt: Double,
        val utbetalingId: UUID?
    )

    fun inntektsmeldingReplay(event: InntektsmeldingReplayEvent) {}
    fun vedtaksperiodePåminnet(vedtaksperiodeId: UUID, påminnelse: Påminnelse) {}
    fun vedtaksperiodeIkkePåminnet(påminnelse: Påminnelse, vedtaksperiodeId: UUID, nåværendeTilstand: TilstandType) {}
    fun vedtaksperiodeEndret(event: VedtaksperiodeEndretEvent) {}
    fun vedtaksperiodeReberegnet(vedtaksperiodeId: UUID) {}
    fun vedtaksperiodeAvbrutt(event: VedtaksperiodeAvbruttEvent) {}
    @Deprecated("Fjernes til fordel for utbetaling_utbetalt")
    fun vedtaksperiodeUtbetalt(event: UtbetaltEvent) {}
    fun personEndret(personEndretEvent: PersonEndretEvent) {}
    fun vedtaksperiodeIkkeFunnet(vedtaksperiodeEvent: VedtaksperiodeIkkeFunnetEvent) {}
    fun manglerInntektsmelding(event: ManglendeInntektsmeldingEvent) {}
    fun trengerIkkeInntektsmelding(event: TrengerIkkeInntektsmeldingEvent) {}
    fun utbetalingEndret(event: UtbetalingEndretEvent) {}
    fun utbetalingUtbetalt(event: UtbetalingUtbetaltEvent) {}
    fun utbetalingUtenUtbetaling(event: UtbetalingUtbetaltEvent) {}
    fun feriepengerUtbetalt(event: FeriepengerUtbetaltEvent) {}
    fun annullering(event: UtbetalingAnnullertEvent) {}
    fun avstemt(result: Map<String, Any>) {}
    fun hendelseIkkeHåndtert(event: HendelseIkkeHåndtertEvent) {}
    fun vedtakFattet(event: VedtakFattetEvent) {}
}
