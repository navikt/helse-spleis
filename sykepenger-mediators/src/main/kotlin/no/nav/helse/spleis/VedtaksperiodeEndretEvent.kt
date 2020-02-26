package no.nav.helse.spleis

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.Topics
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.toMap
import no.nav.helse.serde.reflection.toMap
import org.apache.kafka.clients.producer.ProducerRecord

internal fun PersonObserver.VedtaksperiodeEndretTilstandEvent.producerRecord() =
    ProducerRecord<String, String>(
        Topics.rapidTopic,
        fødselsnummer,
        toJson(this)
    )

private val objectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

private fun toJson(event: PersonObserver.VedtaksperiodeEndretTilstandEvent) = objectMapper.writeValueAsString(
    mapOf(
        "@event_name" to "vedtaksperiode_endret",
        "aktørId" to event.aktørId,
        "fødselsnummer" to event.fødselsnummer,
        "organisasjonsnummer" to event.organisasjonsnummer,
        "vedtaksperiodeId" to event.id,
        "gjeldendeTilstand" to event.gjeldendeTilstand,
        "forrigeTilstand" to event.forrigeTilstand,
        "endringstidspunkt" to event.endringstidspunkt,
        "på_grunn_av" to (event.sykdomshendelse::class.simpleName ?: "UKJENT"),
        "aktivitetslogg" to event.aktivitetslogg.toMap(),
        "timeout" to event.timeout.toSeconds()
    )
)


