package no.nav.helse.person

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
        val aktivitetslogg: Aktivitetslogg,
        val vedtaksperiodeaktivitetslogg: Aktivitetslogg,
        val hendelser: List<UUID>,
        val makstid: LocalDateTime
    )

    data class VedtaksperiodeForkastetEvent(
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
        val oppdrag: List<Utbetalt>,
        val ikkeUtbetalteDager: List<IkkeUtbetaltDag>,
        val fom: LocalDate,
        val tom: LocalDate,
        val forbrukteSykedager: Int,
        val gjenståendeSykedager: Int,
        val opprettet: LocalDateTime,
        val sykepengegrunnlag: Double
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

        data class  IkkeUtbetaltDag(
            val dato: LocalDate,
            val type: Type
        ) {
            enum class Type {
                SykepengedagerOppbrukt,
                MinimumInntekt,
                EgenmeldingUtenforArbeidsgiverperiode,
                MinimumSykdomsgrad,
                Fridag
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

    fun vedtaksperiodeReplay(event: VedtaksperiodeReplayEvent) {}

    fun vedtaksperiodePåminnet(påminnelse: Påminnelse) {}

    fun vedtaksperiodeEndret(event: VedtaksperiodeEndretTilstandEvent) {}

    fun vedtaksperiodeForkastet(event: VedtaksperiodeForkastetEvent) {}

    fun vedtaksperiodeUtbetalt(event: UtbetaltEvent) {}

    fun personEndret(personEndretEvent: PersonEndretEvent) {}

    fun vedtaksperiodeIkkeFunnet(vedtaksperiodeEvent: VedtaksperiodeIkkeFunnetEvent) {}

    fun manglerInntektsmelding(event: ManglendeInntektsmeldingEvent) {}
}
