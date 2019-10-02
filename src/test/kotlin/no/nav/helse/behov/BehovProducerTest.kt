package no.nav.helse.behov

import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.Test

internal class BehovProducerTest {

    @Test
    fun `behov skal produseres`() {
        val producer = mockk< KafkaProducer <String, String>>(relaxed = true)
        val topic = "behov-topic"

        val behovId = BehovProducer(topic, producer)
                .nyttBehov("sykepengehistorikk")

        verify(exactly = 1) {
            producer.send(match { record ->
                behovId.toString() == record.key()
            })
        }
    }
}
