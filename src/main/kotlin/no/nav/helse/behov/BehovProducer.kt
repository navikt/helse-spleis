package no.nav.helse.behov

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

class BehovProducer(private val topic: String, private val producer: KafkaProducer<String, String>) {

    private companion object {
        private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        private val log = LoggerFactory.getLogger(BehovProducer::class.java)

    }

    fun nyttBehov(type: String, additionalParams: Map<String, Any> = emptyMap()): UUID =
            Pakke(type, additionalParams).publiser(topic).first

    inner class Pakke internal constructor(private val type: String, private val additionalParams: Map<String, Any>) {

        private val id = UUID.randomUUID()

        private val opprettet = LocalDateTime.now()

        internal fun publiser(topic: String) =
                id to producer.send(ProducerRecord(topic, key(), value()))
                        .get().also {
                            log.info("produserte behov id=$id, recordMetadata=$it")
                        }

        private fun key() = id.toString()

        private fun value(): String = objectMapper.writeValueAsString(additionalParams + mapOf(
                "@behov" to type,
                "@id" to id.toString(),
                "@opprettet" to opprettet.toString()
        ))

        override fun toString() = "$type:$id"
    }
}
