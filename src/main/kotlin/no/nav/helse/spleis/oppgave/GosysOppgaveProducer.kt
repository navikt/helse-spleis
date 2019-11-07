package no.nav.helse.spleis.oppgave

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.Topics
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import java.util.*

class GosysOppgaveProducer(commonKafkaProperties: Properties) {

    private companion object {
        private val log = LoggerFactory.getLogger(GosysOppgaveProducer::class.java)
    }

    private val oppgaveProducerProperties = commonKafkaProperties.apply {
        put(ProducerConfig.ACKS_CONFIG, "all")
        put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
    }

    private val kafkaProducer = KafkaProducer<String, String>(oppgaveProducerProperties, StringSerializer(), StringSerializer())

    fun opprettOppgave(aktørId: String) {
        kafkaProducer.send(ProducerRecord(
                Topics.opprettGosysOppgaveTopic,
                aktørId,
                OpprettGosysOppgaveDto(aktorId = aktørId).toJson()
        )).get().also {
            log.info("produserte gosysOppgave for aktør=$aktørId, recordMetadata=$it")
        }
    }

    internal class OpprettGosysOppgaveDto(val aktorId: String) {
        private companion object {
            private val objectMapper = jacksonObjectMapper()
                    .registerModule(JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }

        fun toJson() = objectMapper.writeValueAsString(this)
    }
}
