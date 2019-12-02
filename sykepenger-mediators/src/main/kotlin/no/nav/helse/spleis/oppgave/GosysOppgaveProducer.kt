package no.nav.helse.spleis.oppgave

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.Topics
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.util.*

class GosysOppgaveProducer(commonKafkaProperties: Properties) {

    private val oppgaveProducerProperties = commonKafkaProperties.apply {
        put(ProducerConfig.ACKS_CONFIG, "all")
        put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
    }

    private val kafkaProducer = KafkaProducer<String, String>(oppgaveProducerProperties, StringSerializer(), StringSerializer())

    fun opprettOppgave(aktørId: String, fødselsnummer: String) {
        kafkaProducer.send(ProducerRecord(
                Topics.opprettGosysOppgaveTopic,
                aktørId,
                OpprettGosysOppgaveDto(aktorId = aktørId, fødselsnummer = fødselsnummer).toJson()
        )).get()
    }

    internal class OpprettGosysOppgaveDto(internal val aktorId: String, internal val fødselsnummer: String) {
        private companion object {
            private val objectMapper = jacksonObjectMapper()
                    .registerModule(JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }

        fun toJson() = objectMapper.writeValueAsString(this)
    }
}
