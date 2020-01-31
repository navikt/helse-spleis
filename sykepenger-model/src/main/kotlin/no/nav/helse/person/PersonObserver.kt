package no.nav.helse.person

import java.util.UUID

interface PersonObserver : VedtaksperiodeObserver {
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

    fun personEndret(personEndretEvent: PersonEndretEvent) {}

    fun vedtaksperiodeIkkeFunnet(vedtaksperiodeEvent: VedtaksperiodeIkkeFunnetEvent) {}
}
