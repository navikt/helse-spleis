package no.nav.helse.spleis

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.Topics
import no.nav.helse.person.VedtaksperiodeMediator
import org.apache.kafka.clients.producer.ProducerRecord

internal fun VedtaksperiodeMediator.StateChangeEvent.producerRecord() =
    ProducerRecord<String, String>(
        Topics.rapidTopic,
        fødselsnummer,
        toJson(this)
    )

private val objectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

private fun toJson(event: VedtaksperiodeMediator.StateChangeEvent) = objectMapper.writeValueAsString(
    mapOf(
        "@event_name" to "vedtaksperiode_endret",
        "aktørId" to event.aktørId,
        "fødselsnummer" to event.fødselsnummer,
        "organisasjonsnummer" to event.organisasjonsnummer,
        "vedtaksperiodeId" to event.id,
        "gjeldendeTilstand" to event.gjeldendeTilstand,
        "forrigeTilstand" to event.forrigeTilstand,
        "endringstidspunkt" to event.endringstidspunkt,
        "timeout" to event.timeout.toSeconds()
    )
)


