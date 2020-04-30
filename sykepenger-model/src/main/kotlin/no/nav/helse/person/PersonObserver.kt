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
        val hendelser: Set<UUID>,
        val makstid: LocalDateTime
    )

    data class UtbetaltEvent(
        val aktørId: String,
        val fødselsnummer: String,
        val organisasjonsnummer: String,
        val hendelser: Set<UUID>,
        val oppdrag: List<Oppdrag>,
        val fom: LocalDate,
        val tom: LocalDate,
        val forbrukteSykedager: Int,
        val gjenståendeSykedager: Int,
        val opprettet: LocalDateTime
    ) {
        data class Oppdrag(
            val mottaker: String,
            val fagområde: String,
            val fagsystemId: String,
            val totalbeløp: Int,
            val utbetalingslinjer: List<Utbetalingslinje>
        ) {
            data class Utbetalingslinje(
                val fom: LocalDate,
                val tom: LocalDate,
                val dagsats: Int, //TODO: Missing in Utbetalingslinje
                val beløp: Int,
                val grad: Double
            )
        }
    }

    data class ManglendeInntektsmeldingEvent(
        val vedtaksperiodeId: UUID,
        val fødselsnummer: String,
        val organisasjonsnummer: String,
        val fom: LocalDate,
        val tom: LocalDate
    )

    fun vedtaksperiodePåminnet(påminnelse: Påminnelse) {}

    fun vedtaksperiodeEndret(event: VedtaksperiodeEndretTilstandEvent) {}

    fun vedtaksperiodeUtbetalt(event: UtbetaltEvent) {}

    fun personEndret(personEndretEvent: PersonEndretEvent) {}

    fun vedtaksperiodeIkkeFunnet(vedtaksperiodeEvent: VedtaksperiodeIkkeFunnetEvent) {}

    fun manglerInntektsmelding(event: ManglendeInntektsmeldingEvent) {}
}
