package no.nav.helse.nais

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.client.HttpClient
import io.ktor.http.HttpMethod
import io.ktor.util.KtorExperimentalAPI
import no.nav.common.JAASCredential
import no.nav.common.KafkaEnvironment
import no.nav.helse.AppComponentTest
import no.nav.helse.handleRequest
import no.nav.helse.randomPort
import no.nav.helse.serde.JsonNodeSerializer
import no.nav.helse.testServer
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.StringSerializer
import org.awaitility.Awaitility.await
import org.awaitility.Duration
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.sql.Connection
import java.util.Properties
import java.util.concurrent.TimeUnit

@KtorExperimentalAPI
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NaisRoutesTest {
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
            topicNames = listOf("privat-syfo-sm2013-automatiskBehandling", "syfo-soknad-v2")
        )

        @BeforeAll
        @JvmStatic
        fun start() {
            embeddedEnvironment.start()
        }

        @AfterAll
        @JvmStatic
        fun stop() {
            println("after static")
            embeddedEnvironment.tearDown()
        }
    }

    private lateinit var embeddedPostgres: EmbeddedPostgres
    private lateinit var postgresConnection: Connection

    @BeforeEach
    fun `start postgres`() {
        embeddedPostgres = EmbeddedPostgres.builder()
            .start()

        postgresConnection = embeddedPostgres.postgresDatabase.connection
    }

    @AfterEach
    fun `stop postgres`() {
        postgresConnection.close()
        embeddedPostgres.close()
    }

    @Test
    fun tesdt() {
        testServer(
            config = mapOf(
                "KAFKA_BOOTSTRAP_SERVERS" to embeddedEnvironment.brokersURL,
                "KAFKA_USERNAME" to username,
                "KAFKA_PASSWORD" to password,
                "DATABASE_JDBC_URL" to embeddedPostgres.getJdbcUrl("postgres", "postgres")
            ), shutdownTimeoutMs = 20000L
        ) {

            handleRequest(HttpMethod.Get, "/isalive") {
                Assertions.assertTrue(
                    responseCode in 200 until 299,
                    "GET on /isalive should result in server error on faulty liveness check"
                )
            }

            produceOneMessage("syfo-soknad-v2", "key-1", objectMapper.valueToTree(mapOf("garbage" to "message")))

            await()
                .atMost(Duration(10, TimeUnit.SECONDS))
                .untilAsserted {
                    handleRequest(HttpMethod.Get, "/isalive") {
                        print("\nSvar fra isAlive: $responseCode")
                        Assertions.assertTrue(
                            responseCode in 400 until 600,
                            "GET on /isalive should result in server error on faulty liveness check"
                        )
                    }
                }
        }
        print(" --- ute ---")
    }

    private fun produceOneMessage(topic: String, key: String, message: JsonNode) {
        val producer = KafkaProducer<String, JsonNode>(
            producerProperties(), StringSerializer(), JsonNodeSerializer(
                objectMapper
            )
        )
        producer.send(ProducerRecord(topic, key, message))
            .get(1, TimeUnit.SECONDS)
    }

    private fun producerProperties() =
        Properties().apply {
            put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, embeddedEnvironment.brokersURL)
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
            put(SaslConfigs.SASL_MECHANISM, "PLAIN")
            put(
                SaslConfigs.SASL_JAAS_CONFIG,
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${username}\" password=\"${password}\";"
            )
        }
}
