package no.nav.helse.component.person

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.util.KtorExperimentalAPI
import no.nav.common.JAASCredential
import no.nav.common.KafkaEnvironment
import no.nav.helse.TestConstants.inntektsmeldingDTO
import no.nav.helse.TestConstants.søknadDTO
import no.nav.helse.Topics.behovTopic
import no.nav.helse.Topics.inntektsmeldingTopic
import no.nav.helse.Topics.søknadTopic
import no.nav.helse.behov.BehovProducer
import no.nav.helse.createHikariConfig
import no.nav.helse.inntektsmelding.InntektsmeldingConsumer.Companion.inntektsmeldingObjectMapper
import no.nav.helse.sakskompleks.db.runMigration
import no.nav.helse.serde.JsonNodeDeserializer
import no.nav.helse.serde.JsonNodeSerializer
import no.nav.helse.testServer
import no.nav.helse.toJsonNode
import no.nav.syfo.kafka.sykepengesoknad.dto.ArbeidsgiverDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsstatusDTO
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit

@KtorExperimentalAPI
internal class PersonComponentTest {

    companion object {

        private const val username = "srvkafkaclient"
        private const val password = "kafkaclient"

        private val embeddedKafkaEnvironment = KafkaEnvironment(
                autoStart = false,
                withSchemaRegistry = false,
                withSecurity = false,
                topicNames = listOf(søknadTopic, inntektsmeldingTopic, behovTopic)
        )

        private lateinit var embeddedPostgres: EmbeddedPostgres
        private lateinit var postgresConnection: Connection

        private lateinit var hikariConfig: HikariConfig

        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        @BeforeAll
        @JvmStatic
        internal fun `start embedded environment`() {
            embeddedPostgres = EmbeddedPostgres.builder().start()
            postgresConnection = embeddedPostgres.postgresDatabase.connection
            hikariConfig = createHikariConfig(embeddedPostgres.getJdbcUrl("postgres", "postgres"))
            runMigration(HikariDataSource(hikariConfig))
            embeddedKafkaEnvironment.start()

        }

        @AfterAll
        @JvmStatic
        internal fun `stop embedded environment`() {
            embeddedKafkaEnvironment.tearDown()
            postgresConnection.close()
            embeddedPostgres.close()
        }


    }


    @Test
    fun `sender ny søknad til kafka`() {
        testServer(config = mapOf(
                "KAFKA_BOOTSTRAP_SERVERS" to embeddedKafkaEnvironment.brokersURL,
                "KAFKA_USERNAME" to username,
                "KAFKA_PASSWORD" to password,
                "DATABASE_JDBC_URL" to embeddedPostgres.getJdbcUrl("postgres", "postgres")
        )) {

            val nySøknad = søknadDTO(status = SoknadsstatusDTO.NY)
            sendKafkaMessage(søknadTopic, nySøknad.id!!, nySøknad.toJsonNode())
            val resultConsumer = KafkaConsumer<String, String>(consumerProperties(), StringDeserializer(), StringDeserializer())
            resultConsumer.subscribe(listOf(søknadTopic))

            await()
                    .atMost(30, TimeUnit.SECONDS)
                    .untilAsserted {
                        val record = resultConsumer.poll(Duration.ofSeconds(1))
                        Assertions.assertFalse(record!!.isEmpty)
                    }
            resultConsumer.unsubscribe()
        }
    }


    @Test
    fun `komplett sak fører til at sykepengehistorikk blir etterspurt`() {
        testServer(config = mapOf(
                "KAFKA_BOOTSTRAP_SERVERS" to embeddedKafkaEnvironment.brokersURL,
                "KAFKA_USERNAME" to username,
                "KAFKA_PASSWORD" to password,
                "DATABASE_JDBC_URL" to embeddedPostgres.getJdbcUrl("postgres", "postgres")
        )) {

            val aktorID = "1234567890123"
            val virksomhetsnummer = "123456789"

            val nySøknad = søknadDTO(aktørId = aktorID, arbeidsgiver = ArbeidsgiverDTO(orgnummer = virksomhetsnummer), status = SoknadsstatusDTO.NY)
            val sendtSøknad = søknadDTO(aktørId = aktorID, arbeidsgiver = ArbeidsgiverDTO(orgnummer = virksomhetsnummer), status = SoknadsstatusDTO.SENDT)
            val inntektsMelding = inntektsmeldingDTO(aktørId = aktorID, virksomhetsnummer = virksomhetsnummer)

            val resultConsumer = KafkaConsumer<String, String>(consumerProperties(), StringDeserializer(), StringDeserializer())
            resultConsumer.subscribe(listOf(behovTopic, søknadTopic, inntektsmeldingTopic))

            sendKafkaMessage(søknadTopic, nySøknad.id!!, nySøknad.toJsonNode())
            assertMessageReadyToBeConsumed(resultConsumer, søknadTopic)

            sendKafkaMessage(søknadTopic, sendtSøknad.id!!, sendtSøknad.toJsonNode())
            assertMessageReadyToBeConsumed(resultConsumer, søknadTopic)

            sendKafkaMessage(inntektsmeldingTopic, inntektsMelding.inntektsmeldingId, inntektsMelding.toJsonNode())
            assertMessageReadyToBeConsumed(resultConsumer, inntektsmeldingTopic)

            assertMessageReadyToBeConsumed(resultConsumer, behovTopic)

            resultConsumer.unsubscribe()
        }
    }

    private fun assertMessageReadyToBeConsumed(resultConsumer: KafkaConsumer<String, String>, topic: String) {
        await()
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted {
                    val record = resultConsumer.poll(Duration.ofSeconds(1))
                    assertEquals(1, record.records(topic).count(), "Antall meldinger på topic $topic skulle vært 1, men var ${record.records(topic).count()}")
                }
    }

    private fun sendKafkaMessage(topic: String, key: String, message: JsonNode, objectMapper: ObjectMapper = inntektsmeldingObjectMapper) {
        val producer = KafkaProducer<String, JsonNode>(producerProperties(), StringSerializer(), JsonNodeSerializer(objectMapper))
        producer.send(ProducerRecord(topic, key, message))
        producer.flush()
    }

    private fun producerProperties() =
            Properties().apply {
                put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaEnvironment.brokersURL)
                put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT")
                put(SaslConfigs.SASL_MECHANISM, "PLAIN")
            }


    private fun consumerProperties(): MutableMap<String, Any>? {
        return HashMap<String, Any>().apply {
            put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaEnvironment.brokersURL)
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT")
            put(SaslConfigs.SASL_MECHANISM, "PLAIN")
            put(ConsumerConfig.GROUP_ID_CONFIG, "personComponentTestGroupID")
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        }
    }

}
