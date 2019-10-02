package no.nav.helse.behov

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.Test
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

internal class BehovProducerTest {

    @Test
    fun `behov skal produseres`() {
        val producer = mockk< KafkaProducer <String, String>>(relaxed = true)
        val topic = "behov-topic"

        every {
            producer.send(any())
        } returns DummyFuture(RecordMetadata(TopicPartition(topic, 0), 0L, 0L, 0L, 0L, 0, 0))

        val behovId = BehovProducer(topic, producer)
                .nyttBehov("sykepengehistorikk")

        verify(exactly = 1) {
            producer.send(match { record ->
                behovId.toString() == record.key()
            })
        }
    }

    class DummyFuture(private val recordMetadata: RecordMetadata): Future<RecordMetadata> {
        override fun isDone(): Boolean {
            TODO("not implemented")
        }

        override fun get(): RecordMetadata {
            return recordMetadata
        }

        override fun get(timeout: Long, unit: TimeUnit): RecordMetadata {
            TODO("not implemented")
        }

        override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
            TODO("not implemented")
        }

        override fun isCancelled(): Boolean {
            TODO("not implemented")
        }

    }
}
