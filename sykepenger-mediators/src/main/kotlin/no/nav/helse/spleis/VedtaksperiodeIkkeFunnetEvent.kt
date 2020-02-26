package no.nav.helse.spleis

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.person.PersonObserver.VedtaksperiodeIkkeFunnetEvent

private val objectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

internal fun VedtaksperiodeIkkeFunnetEvent.toJson() = objectMapper.writeValueAsString(
    mapOf(
        "@event_name" to "vedtaksperiode_ikke_funnet",
        "aktørId" to this.aktørId,
        "fødselsnummer" to this.fødselsnummer,
        "organisasjonsnummer" to this.organisasjonsnummer,
        "vedtaksperiodeId" to this.vedtaksperiodeId
    )
)


