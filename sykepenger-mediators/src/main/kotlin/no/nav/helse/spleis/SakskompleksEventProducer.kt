package no.nav.helse.spleis

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.Topics
import no.nav.helse.sak.TilstandType
import no.nav.helse.sak.VedtaksperiodeObserver
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.util.*

class VedtaksperiodeEventProducer(commonKafkaProperties: Properties) {

    private val properties = commonKafkaProperties.apply {
        put(ProducerConfig.ACKS_CONFIG, "all")
    }

    private val kafkaProducer = KafkaProducer<String, String>(properties, StringSerializer(), StringSerializer())

    fun sendEndringEvent(event: VedtaksperiodeObserver.StateChangeEvent) {
        kafkaProducer.send(
            ProducerRecord(
                Topics.vedtaksperiodeEventTopic,
                event.aktørId,
                VedtaksperiodeEventDto(
                    aktørId = event.aktørId,
                    organisasjonsnummer = event.organisasjonsnummer,
                    vedtaksperiodeId = event.id,
                    currentState = event.currentState,
                    previousState = event.previousState
                ).toJson()
            )
        )
    }

    internal class VedtaksperiodeEventDto(
        val aktørId: String,
        val organisasjonsnummer: String,
        val vedtaksperiodeId: UUID,
        val currentState: TilstandType,
        val previousState: TilstandType
    ) {
        private companion object {
            private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }

        internal fun toJson() = objectMapper.writeValueAsString(this)
    }
}
