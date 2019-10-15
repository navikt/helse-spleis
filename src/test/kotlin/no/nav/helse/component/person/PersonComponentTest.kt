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
import kafka.security.auth.Topic
import no.nav.common.JAASCredential
import no.nav.common.KafkaEnvironment
import no.nav.helse.TestConstants.inntektsmeldingDTO
import no.nav.helse.TestConstants.søknadDTO
import no.nav.helse.Topics
import no.nav.helse.Topics.inntektsmeldingTopic
import no.nav.helse.Topics.søknadTopic
import no.nav.helse.behov.BehovProducer
import no.nav.helse.createHikariConfig
import no.nav.helse.inntektsmelding.InntektsmeldingConsumer.Companion.inntektsmeldingObjectMapper
import no.nav.helse.person.PersonMediator
import no.nav.helse.person.PersonPostgresRepository
import no.nav.helse.sakskompleks.db.runMigration
import no.nav.helse.serde.JsonNodeDeserializer
import no.nav.helse.serde.JsonNodeSerializer
import no.nav.helse.testServer
import no.nav.helse.toJsonNode
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

        private val embeddedEnvironment = KafkaEnvironment(
            users = listOf(JAASCredential(username, password)),
            autoStart = false,
            withSchemaRegistry = false,
            withSecurity = true,
            topicNames = listOf(søknadTopic, inntektsmeldingTopic)
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
            embeddedEnvironment.start()

        }

        @AfterAll
        @JvmStatic
        internal fun `stop embedded environment`() {
            embeddedEnvironment.tearDown()
            postgresConnection.close()
            embeddedPostgres.close()
        }


    }

    @Test
    internal fun `testtopology`() {
        val repo = PersonPostgresRepository(HikariDataSource(hikariConfig))

        val personMediator = PersonMediator(
            personRepository = repo
        )

    }

    @Test
    fun `inntektsmelding som kommer først, blir ignorert`() {
        testServer(config = mapOf(
            "KAFKA_BOOTSTRAP_SERVERS" to embeddedEnvironment.brokersURL,
            "KAFKA_USERNAME" to username,
            "KAFKA_PASSWORD" to password,
            "DATABASE_JDBC_URL" to embeddedPostgres.getJdbcUrl("postgres", "postgres")
        )) {

            val søknadDTO = søknadDTO(status = SoknadsstatusDTO.NY)
            val sendtSøknadDTO = søknadDTO(status = SoknadsstatusDTO.SENDT)
            sendKafkaMessage(søknadTopic, søknadDTO.id!!, søknadDTO.toJsonNode())
            sendKafkaMessage(søknadTopic, sendtSøknadDTO.id!!, sendtSøknadDTO.toJsonNode())
            sendKafkaMessage(inntektsmeldingTopic, inntektsmeldingDTO().inntektsmeldingId, inntektsmeldingDTO().toJsonNode())

            await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted {
                    val record = lyttPåTopic(objectMapper, Topics.behovTopic)
                    Assertions.assertFalse(record!!.isEmpty)

                }
        }
    }

    private fun sendKafkaMessage(topic: String, key: String, message: JsonNode, objectMapper: ObjectMapper = inntektsmeldingObjectMapper) {
        val producer = KafkaProducer<String, JsonNode>(producerProperties(), StringSerializer(), JsonNodeSerializer(objectMapper))
        producer.send(ProducerRecord(topic, key, message))
        producer.flush()
    }

    private fun lyttPåTopic(objectMapper: ObjectMapper, topic: String): ConsumerRecords<String, JsonNode>? {
        val resultConsumer = KafkaConsumer<String, JsonNode>(consumerProperties(), StringDeserializer(), JsonNodeDeserializer(objectMapper))
        resultConsumer.subscribe(listOf(topic))
        resultConsumer.seekToBeginning(resultConsumer.assignment())
        return resultConsumer.poll(Duration.ofSeconds(1))
    }

    private fun producerProperties() =
        Properties().apply {
            put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, embeddedEnvironment.brokersURL)
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
            put(SaslConfigs.SASL_MECHANISM, "PLAIN")
            put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$username\" password=\"$password\";")
        }


    private fun consumerProperties(): MutableMap<String, Any>? {
        return HashMap<String, Any>().apply {
            put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, embeddedEnvironment.brokersURL)
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
            put(SaslConfigs.SASL_MECHANISM, "PLAIN")
            put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$username\" password=\"$password\";")
            put(ConsumerConfig.GROUP_ID_CONFIG, "spa-e2e-verification")
        }
    }

}
