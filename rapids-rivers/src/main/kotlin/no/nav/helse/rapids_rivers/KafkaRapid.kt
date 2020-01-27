package no.nav.helse.rapids_rivers

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.errors.WakeupException
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class KafkaRapid(
    consumerConfig: Properties,
    producerConfig: Properties,
    private val topic: String
) : RapidsConnection() {

    private val log = LoggerFactory.getLogger(KafkaRapid::class.java)

    private val running = AtomicBoolean(false)

    private val stringDeserializer = StringDeserializer()
    private val stringSerializer = StringSerializer()
    private val consumer = KafkaConsumer(consumerConfig, stringDeserializer, stringDeserializer)
    private val producer = KafkaProducer(producerConfig, stringSerializer, stringSerializer)

    fun isRunning() = running.get()

    fun start() {
        log.info("starting rapid")
        running.set(true)
        try {
            consumer.use { consumeMessages() }
        } finally {
            stop()
        }
    }

    fun stop() {
        log.info("stopping rapid")
        if (!running.get()) return log.info("rappid already stopped")
        running.set(false)
        producer.close()
        consumer.wakeup()
    }

    private fun consumeMessages() {
        try {
            consumer.subscribe(listOf(topic))
            while (running.get()) {
                val records = consumer.poll(Duration.ofSeconds(1))
                records.forEach { record ->
                    val context = KafkaMessageContext(record, producer)
                    listeners.forEach { it.onMessage(record.value(), context) }
                }
            }
        } catch (err: WakeupException) {
            // throw exception if we have not been told to stop
            if (running.get()) throw err
        } finally {
            log.info("stopped consuming messages")
            consumer.unsubscribe()
        }
    }

    private class KafkaMessageContext(
        private val record: ConsumerRecord<String, String>,
        private val producer: Producer<String, String>
    ) : MessageContext {
        override fun send(message: String) {
            send(record.key(), message)
        }

        override fun send(key: String, message: String) {
            producer.send(ProducerRecord(record.topic(), key, message))
        }
    }
}
