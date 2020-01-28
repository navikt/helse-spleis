package no.nav.helse.rapids_rivers

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
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

    override fun publish(message: String) {
        producer.send(ProducerRecord(topic, message))
    }

    override fun publish(key: String, message: String) {
        producer.send(ProducerRecord(topic, key, message))
    }

    override fun start() {
        log.info("starting rapid")
        running.set(true)
        try {
            consumer.use { consumeMessages() }
        } finally {
            stop()
        }
    }

    override fun stop() {
        log.info("stopping rapid")
        if (!running.get()) return log.info("rappid already stopped")
        running.set(false)
        producer.close()
        consumer.wakeup()
    }

    private fun onRecord(record: ConsumerRecord<String, String>) {
        val context = KafkaMessageContext(record, this)
        listeners.forEach { it.onMessage(record.value(), context) }
    }

    private fun consumeMessages() {
        try {
            consumer.subscribe(listOf(topic))
            while (running.get()) {
                consumer.poll(Duration.ofSeconds(1))
                    .forEach(::onRecord)
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
        private val rapidsConnection: RapidsConnection
    ) : MessageContext {
        override fun send(message: String) {
            send(record.key(), message)
        }

        override fun send(key: String, message: String) {
            rapidsConnection.publish(key, message)
        }
    }
}
