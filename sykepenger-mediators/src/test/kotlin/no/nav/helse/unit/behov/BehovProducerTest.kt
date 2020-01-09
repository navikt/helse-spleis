package no.nav.helse.unit.behov

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.behov.Behov
import no.nav.helse.behov.BehovProducer
import no.nav.helse.behov.Behovstype
import no.nav.helse.person.ArbeidstakerHendelse
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

internal class BehovProducerTest {

    @Test
    fun `behov skal produseres`() {
        val producer = mockk<KafkaProducer<String, String>>(relaxed = true)
        val topic = "behov-topic"

        every {
            producer.send(any())
        } returns DummyFuture(RecordMetadata(TopicPartition(topic, 0), 0L, 0L, 0L, 0L, 0, 0))

        BehovProducer(topic, producer)
            .sendNyttBehov(
                Behov.nyttBehov(
                    ArbeidstakerHendelse.Hendelsestype.Ytelser,
                    listOf(Behovstype.Sykepengehistorikk),
                    "aktørId",
                    "fnr",
                    "orgnr",
                    UUID.randomUUID(),
                    emptyMap()
                )
            )

        verify(exactly = 1) {
            producer.send(match { record ->
                record.value().contains(Behovstype.Sykepengehistorikk.name)
            })
        }
    }

    class DummyFuture(private val recordMetadata: RecordMetadata) : Future<RecordMetadata> {
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
