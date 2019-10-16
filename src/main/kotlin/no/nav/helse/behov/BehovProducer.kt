package no.nav.helse.behov

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.util.*

class BehovProducer(private val topic: String, private val producer: KafkaProducer<String, String>) {
    companion object {
        private val log = LoggerFactory.getLogger(BehovProducer::class.java)
    }

    fun sendNyttBehov(type: String, additionalParams: Map<String, Any>) {
        publiser(Behov.nyttBehov(type, additionalParams))
    }

    private fun publiser(behov: Behov) =
            producer.send(ProducerRecord(topic, behov.id().toString(), behov.toJson()))
                    .get().also {
                        log.info("produserte behov=$behov, recordMetadata=$it")
                    }
}
