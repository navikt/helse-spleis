package no.nav.helse.spleis

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.Topics
import no.nav.helse.sak.VedtaksperiodeObserver
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.util.*

class VedtaksperiodeEventProducer(commonKafkaProperties: Properties) {

    private companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    private val properties = commonKafkaProperties.apply {
        put(ProducerConfig.ACKS_CONFIG, "all")
    }

    private val kafkaProducer = KafkaProducer<String, String>(properties, StringSerializer(), StringSerializer())

    fun sendEndringEvent(event: VedtaksperiodeObserver.StateChangeEvent) {
        kafkaProducer.send(
            ProducerRecord(
                Topics.vedtaksperiodeEventTopic,
                event.aktørId,
                toJson(event)
            )
        )
    }

    private fun toJson(event: VedtaksperiodeObserver.StateChangeEvent) = objectMapper.writeValueAsString(
        mapOf(
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
}
