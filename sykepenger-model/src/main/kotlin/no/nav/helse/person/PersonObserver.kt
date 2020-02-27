package no.nav.helse.person

import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingslinje
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
        val aktivitetslogger: Aktivitetslogger,
        val aktivitetslogg: Aktivitetslogg,
        val timeout: Duration
    ) {
        val endringstidspunkt = LocalDateTime.now()
    }

    data class UtbetalingEvent(
        val vedtaksperiodeId: UUID,
        val aktørId: String,
        val fødselsnummer: String,
        val organisasjonsnummer: String,
        val utbetalingsreferanse: String,
        val utbetalingslinjer: List<Utbetalingslinje>,
        val opprettet: LocalDate
    )

    data class UtbetaltEvent(
        val vedtaksperiodeId: UUID,
        val aktørId: String,
        val fødselsnummer: String,
        val utbetalingsreferanse: String,
        val utbetalingslinjer: List<Utbetalingslinje>,
        val opprettet: LocalDate,
        val forbrukteSykedager: Int
    )

    fun vedtaksperiodePåminnet(påminnelse: Påminnelse) {}

    fun vedtaksperiodeEndret(event: VedtaksperiodeEndretTilstandEvent) {}

    fun vedtaksperiodeTilUtbetaling(event: UtbetalingEvent) {}

    fun vedtaksperiodeUtbetalt(event: UtbetaltEvent) {}

    fun personEndret(personEndretEvent: PersonEndretEvent) {}

    fun vedtaksperiodeIkkeFunnet(vedtaksperiodeEvent: VedtaksperiodeIkkeFunnetEvent) {}
}
