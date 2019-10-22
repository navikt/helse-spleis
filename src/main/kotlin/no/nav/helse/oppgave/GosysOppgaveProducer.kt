package no.nav.helse.oppgave

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.Topics
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.util.*

private val objectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

class GosysOppgaveProducer(commonKafkaProperties: Properties) {

    val oppgaveProducerProperties = commonKafkaProperties.apply {
        put(ProducerConfig.ACKS_CONFIG, "all")
        put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
    }

    private val kafkaProducer = KafkaProducer<String, String>(oppgaveProducerProperties, StringSerializer(), StringSerializer())

    internal class OpprettGosysOppgaveDto(val aktorId: String)

    fun opprettOppgave(aktørId: String){
        kafkaProducer.send(ProducerRecord(
            Topics.opprettGosysOppgaveTopic,
            aktørId,
            objectMapper.writeValueAsString(OpprettGosysOppgaveDto(aktorId = aktørId))
        ))
    }
}