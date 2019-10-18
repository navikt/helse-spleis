package no.nav.helse.behov

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

class BehovProducer(private val topic: String, private val producer: KafkaProducer<String, String>) {
    companion object {
        private val log = LoggerFactory.getLogger(BehovProducer::class.java)
    }

    fun sendNyttBehov(behov: Behov) =
            producer.send(ProducerRecord(topic, behov.id().toString(), behov.toJson()))
                    .get().also {
                        log.info("produserte behov=$behov, recordMetadata=$it")
                    }
}
