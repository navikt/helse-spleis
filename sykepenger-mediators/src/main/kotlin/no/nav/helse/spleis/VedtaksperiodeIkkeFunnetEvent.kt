package no.nav.helse.spleis

import no.nav.helse.person.PersonObserver.VedtaksperiodeIkkeFunnetEvent

internal fun VedtaksperiodeIkkeFunnetEvent.toJson() = objectMapper.writeValueAsString(
    mapOf(
        "@event_name" to "vedtaksperiode_ikke_funnet",
        "aktørId" to this.aktørId,
        "fødselsnummer" to this.fødselsnummer,
        "organisasjonsnummer" to this.organisasjonsnummer,
        "vedtaksperiodeId" to this.vedtaksperiodeId
    )
)
