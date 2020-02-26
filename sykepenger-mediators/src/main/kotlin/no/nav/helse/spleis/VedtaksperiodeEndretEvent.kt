package no.nav.helse.spleis

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.person.PersonObserver
import no.nav.helse.serde.reflection.toMap

private val objectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

internal fun PersonObserver.VedtaksperiodeEndretTilstandEvent.toJson() = objectMapper.writeValueAsString(
    mapOf(
        "@event_name" to "vedtaksperiode_endret",
        "aktørId" to this.aktørId,
        "fødselsnummer" to this.fødselsnummer,
        "organisasjonsnummer" to this.organisasjonsnummer,
        "vedtaksperiodeId" to this.id,
        "gjeldendeTilstand" to this.gjeldendeTilstand,
        "forrigeTilstand" to this.forrigeTilstand,
        "endringstidspunkt" to this.endringstidspunkt,
        "på_grunn_av" to (this.sykdomshendelse::class.simpleName ?: "UKJENT"),
        "aktivitetslogger" to this.aktivitetslogger.toMap(),
        "timeout" to this.timeout.toSeconds()
    )
)


