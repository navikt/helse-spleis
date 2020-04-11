package no.nav.helse.person

import no.nav.helse.hendelser.Påminnelse
import java.time.Duration
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
        val id: UUID,
        val aktørId: String,
        val fødselsnummer: String,
        val organisasjonsnummer: String,
        val gjeldendeTilstand: TilstandType,
        val forrigeTilstand: TilstandType,
        val sykdomshendelse: ArbeidstakerHendelse,
        val aktivitetslogg: Aktivitetslogg,
        val timeout: Duration,
        val hendelsesIder: Set<UUID>
    ) {
        val endringstidspunkt = LocalDateTime.now()
    }

    data class UtbetaltEvent(
        val aktørId: String,
        val fødselsnummer: String,
        val gruppeId: UUID,
        val vedtaksperiodeId: UUID,
        val utbetalingslinjer: List<Utbetalingslinjer>,
        val opprettet: LocalDateTime,
        val forbrukteSykedager: Int
    )

    data class Utbetalingslinjer(
        val utbetalingsreferanse: String,
        val utbetalingslinjer: List<Utbetalingslinje>
    )

    data class Utbetalingslinje(
        val fom: LocalDate,
        val tom: LocalDate,
        val dagsats: Int,
        val grad: Double
    )

    data class ManglendeInntektsmeldingEvent(
        val vedtaksperiodeId: UUID,
        val fødselsnummer: String,
        val organisasjonsnummer: String,
        val opprettet: LocalDateTime,
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
