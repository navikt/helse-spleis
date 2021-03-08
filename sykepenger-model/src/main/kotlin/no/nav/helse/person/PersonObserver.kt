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

    data class VedtaksperiodeEndretTilstandEvent(
        val vedtaksperiodeId: UUID,
        val aktørId: String,
        val fødselsnummer: String,
        val organisasjonsnummer: String,
        val gjeldendeTilstand: TilstandType,
        val forrigeTilstand: TilstandType,
        val aktivitetslogg: Map<String, List<Map<String, Any>>>,
        val vedtaksperiodeaktivitetslogg: Map<String, List<Map<String, Any>>>,
        val hendelser: List<UUID>,
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
        val aktørId: String,
        val fødselsnummer: String,
        val organisasjonsnummer: String,
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
                val dagsats: Int,
                val beløp: Int,
                val grad: Double,
                val sykedager: Int
            )
        }

        data class IkkeUtbetaltDag(
            val dato: LocalDate,
            val type: Type
        ) {
            enum class Type {
                SykepengedagerOppbrukt,
                MinimumInntekt,
                EgenmeldingUtenforArbeidsgiverperiode,
                MinimumSykdomsgrad,
                Annullering,
                Fridag,
                Arbeidsdag,
                EtterDødsdato
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
        val saksbehandlerEpost: String
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
        val personOppdrag: Map<String, Any>
    )

    data class InntektsmeldingLagtPåKjølEvent(
        val hendelseId: UUID,
    )

    data class VedtaksperiodeReberegnetEvent(val vedtaksperiodeId: UUID)

    fun vedtaksperiodeReplay(event: VedtaksperiodeReplayEvent) {}
    fun inntektsmeldingReplay(event: InntektsmeldingReplayEvent) {}
    fun vedtaksperiodePåminnet(vedtaksperiodeId: UUID, påminnelse: Påminnelse) {}
    fun vedtaksperiodeIkkePåminnet(påminnelse: Påminnelse, vedtaksperiodeId: UUID, nåværendeTilstand: TilstandType) {}
    fun vedtaksperiodeEndret(event: VedtaksperiodeEndretTilstandEvent) {}
    fun vedtaksperiodeReberegnet(vedtaksperiodeId: UUID) {}
    fun vedtaksperiodeAvbrutt(event: VedtaksperiodeAvbruttEvent) {}
    fun vedtaksperiodeUtbetalt(event: UtbetaltEvent) {}
    fun personEndret(personEndretEvent: PersonEndretEvent) {}
    fun vedtaksperiodeIkkeFunnet(vedtaksperiodeEvent: VedtaksperiodeIkkeFunnetEvent) {}
    fun inntektsmeldingLagtPåKjøl(event: InntektsmeldingLagtPåKjølEvent) {}
    fun manglerInntektsmelding(event: ManglendeInntektsmeldingEvent) {}
    fun trengerIkkeInntektsmelding(event: TrengerIkkeInntektsmeldingEvent) {}
    fun utbetalingEndret(event: UtbetalingEndretEvent) {}
    fun utbetalingUtbetalt(event: UtbetalingUtbetaltEvent) {}
    fun utbetalingUtenUtbetaling(event: UtbetalingUtbetaltEvent) {}
    fun annullering(event: UtbetalingAnnullertEvent) {}
    fun avstemt(result: Map<String, Any>) {}
    fun vedtakFattet(
        vedtaksperiodeId: UUID,
        periode: Periode,
        hendelseIder: List<UUID>,
        skjæringstidspunkt: LocalDate,
        sykepengegrunnlag: Double,
        inntekt: Double,
        utbetalingId: UUID?
    ) {}
}
