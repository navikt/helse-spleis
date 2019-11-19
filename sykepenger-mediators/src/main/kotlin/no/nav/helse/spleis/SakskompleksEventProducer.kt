package no.nav.helse.spleis

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.Topics
import no.nav.helse.sak.Sakskompleks
import no.nav.helse.sak.SakskompleksObserver
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.util.*

class SakskompleksEventProducer (commonKafkaProperties: Properties) {

    private val properties = commonKafkaProperties.apply {
        put(ProducerConfig.ACKS_CONFIG, "all")
    }

    private val kafkaProducer = KafkaProducer<String, String>(properties, StringSerializer(), StringSerializer())

    fun sendEndringEvent(event: SakskompleksObserver.StateChangeEvent) {
        kafkaProducer.send(ProducerRecord(
                Topics.sakskompleksEventTopic,
                event.aktørId,
                SakskompleksEventDto(
                        aktørId = event.aktørId,
                        organisasjonsnummer = event.organisasjonsnummer,
                        sakskompleksId = event.id,
                        currentState = event.currentState,
                        previousState = event.previousState
                ).toJson()
        ))
    }

    internal class SakskompleksEventDto(val aktørId: String, val organisasjonsnummer: String, val sakskompleksId: UUID, val currentState: Sakskompleks.TilstandType, val previousState: Sakskompleks.TilstandType) {
        private companion object {
            private val objectMapper = jacksonObjectMapper()
                    .registerModule(JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }

        internal fun toJson() = objectMapper.writeValueAsString(this)
    }
}
