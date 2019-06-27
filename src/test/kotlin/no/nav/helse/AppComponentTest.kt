package no.nav.helse

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.util.KtorExperimentalAPI
import io.prometheus.client.CollectorRegistry
import no.nav.common.JAASCredential
import no.nav.common.KafkaEnvironment
import no.nav.helse.serde.JsonNodeSerializer
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.StringSerializer
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.TimeUnit

@KtorExperimentalAPI
class AppComponentTest {

    companion object {

        private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        private const val username = "srvkafkaclient"
        private const val password = "kafkaclient"

        private val embeddedEnvironment = KafkaEnvironment(
                users = listOf(JAASCredential(username, password)),
                autoStart = false,
                withSchemaRegistry = false,
                withSecurity = true,
                topicNames = listOf("sykmeldinger", "soknader")
        )

        @BeforeAll
        @JvmStatic
        fun start() {
            embeddedEnvironment.start()
        }

        @AfterAll
        @JvmStatic
        fun stop() {
            embeddedEnvironment.tearDown()
        }
    }

    @Test
    fun `should start`() {
        testServer(config = mapOf(
                "KAFKA_BOOTSTRAP_SERVERS" to embeddedEnvironment.brokersURL,
                "KAFKA_USERNAME" to username,
                "KAFKA_PASSWORD" to password
        )) {

            val sykmeldingCounterBefore = getCounterValue("sykmeldinger_totals")
            val søknadCounterBefore = getCounterValue("soknader_totals")

            val sykmelding = objectMapper.readValue(sykmelding_json, JsonNode::class.java)
            produceOneMessage("sykmeldinger", sykmelding["id"].asText(), sykmelding)

            val søknad = objectMapper.readValue(søknad_json, JsonNode::class.java)
            produceOneMessage("soknader", søknad["id"].asText(), søknad)

            await()
                    .atMost(10, TimeUnit.SECONDS)
                    .untilAsserted {
                        val sykmeldingCounterAfter = getCounterValue("sykmeldinger_totals")
                        val søknadCounterAfter = getCounterValue("soknader_totals")

                        assertEquals(1, sykmeldingCounterAfter - sykmeldingCounterBefore)
                        assertEquals(1, søknadCounterAfter - søknadCounterBefore)
                    }
        }
    }

    private fun produceOneMessage(topic: String, key: String, message: JsonNode) {
        val producer = KafkaProducer<String, JsonNode>(producerProperties(), StringSerializer(), JsonNodeSerializer(objectMapper))
        producer.send(ProducerRecord(topic, key, message))
                .get(1, TimeUnit.SECONDS)
    }

    private fun producerProperties() =
            Properties().apply {
                put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, embeddedEnvironment.brokersURL)
                put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
                put(SaslConfigs.SASL_MECHANISM, "PLAIN")
                put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$username\" password=\"$password\";")
            }

    private fun getCounterValue(name: String, labelValues: List<String> = emptyList()) =
            (CollectorRegistry.defaultRegistry
                    .findMetricSample(name, labelValues)
                    ?.value ?: 0.0).toInt()

    private fun CollectorRegistry.findMetricSample(name: String, labelValues: List<String>) =
            findSamples(name).firstOrNull { sample ->
                sample.labelValues.size == labelValues.size && sample.labelValues.containsAll(labelValues)
            }

    private fun CollectorRegistry.findSamples(name: String) =
            filteredMetricFamilySamples(setOf(name))
                    .toList()
                    .flatMap { metricFamily ->
                        metricFamily.samples
                    }
}

private val sykmelding_json = """
{
    "id": "71bd853d-36a1-49df-a34c-6e02cf727cfa",
    "fnr": "11111111111",
    "lege": "Hans Hansen"
}
""".trimIndent()

private val søknad_json = """
{
    "id": "68da259c-ff7f-47cf-8fa0-c348ae95e220",
    "sykmeldingId": "71bd853d-36a1-49df-a34c-6e02cf727cfa",
    "status": "NY",
    "arbeidsgiver": "Nærbutikken AS"
}
""".trimIndent()
