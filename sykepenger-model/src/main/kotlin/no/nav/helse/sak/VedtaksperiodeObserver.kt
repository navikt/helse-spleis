package no.nav.helse.sak

import no.nav.helse.behov.Behov
import java.util.*

interface VedtaksperiodeObserver {
    data class StateChangeEvent(val id: UUID,
                                val aktørId: String,
                                val fødselsnummer: String,
                                val organisasjonsnummer: String,
                                val currentState: TilstandType,
                                val previousState: TilstandType,
                                val sykdomshendelse: ArbeidstakerHendelse)

    data class UtbetalingEvent(val vedtaksperiodeId: UUID,
                               val aktørId: String,
                               val fødselsnummer: String,
                               val organisasjonsnummer: String,
                               val utbetalingsreferanse: String)


    fun vedtaksperiodeEndret(event: StateChangeEvent) {}

    fun vedtaksperiodeTilUtbetaling(event: UtbetalingEvent) {}

    fun vedtaksperiodeTrengerLøsning(event: Behov) {}

}
