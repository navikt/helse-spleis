package no.nav.helse.sak

import java.util.*

interface SakObserver : VedtaksperiodeObserver {
    data class SakEndretEvent(val aktørId: String,
                              val sykdomshendelse: ArbeidstakerHendelse,
                              val memento: Sak.Memento)

    data class VedtaksperiodeIkkeFunnetEvent(val vedtaksperiodeId: UUID,
                                             val aktørId: String,
                                             val fødselsnummer: String,
                                             val organisasjonsnummer: String)

    fun sakEndret(sakEndretEvent: SakEndretEvent)

    fun vedtaksperiodeIkkeFunnet(vedtaksperiodeEvent: VedtaksperiodeIkkeFunnetEvent) {}
}
