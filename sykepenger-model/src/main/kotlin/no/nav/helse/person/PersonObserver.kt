package no.nav.helse.person

import java.util.*

interface PersonObserver : VedtaksperiodeObserver {
    data class PersonEndretEvent(val aktørId: String,
                                 val sykdomshendelse: ArbeidstakerHendelse,
                                 val memento: Person.Memento)

    data class VedtaksperiodeIkkeFunnetEvent(val vedtaksperiodeId: UUID,
                                             val aktørId: String,
                                             val fødselsnummer: String,
                                             val organisasjonsnummer: String)

    fun personEndret(personEndretEvent: PersonEndretEvent)

    fun vedtaksperiodeIkkeFunnet(vedtaksperiodeEvent: VedtaksperiodeIkkeFunnetEvent) {}
}
