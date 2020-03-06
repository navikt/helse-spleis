package no.nav.helse.e2e

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import no.nav.common.KafkaEnvironment
import no.nav.helse.ApplicationBuilder
import no.nav.helse.Topics.rapidTopic
import no.nav.helse.handleRequest
import no.nav.helse.randomPort
import no.nav.helse.responseBody
import org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig.*
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM
import org.apache.kafka.common.serialization.StringSerializer
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import java.sql.Connection
import java.util.*
import java.util.concurrent.TimeUnit.SECONDS

@KtorExperimentalAPI
@TestInstance(Lifecycle.PER_CLASS)
internal class EndToEndTest {

    private val username = "srvkafkaclient"
    private val password = "kafkaclient"
    private val kafkaApplicationId = "spleis-v1"

    private val embeddedKafkaEnvironment = KafkaEnvironment(
        autoStart = false,
        noOfBrokers = 1,
        topicInfos = listOf(KafkaEnvironment.TopicInfo(rapidTopic, partitions = 1)),
        withSchemaRegistry = false,
        withSecurity = false
    )

    private lateinit var adminClient: AdminClient
    private lateinit var kafkaProducer: KafkaProducer<String, String>

    private lateinit var embeddedPostgres: EmbeddedPostgres
    private lateinit var postgresConnection: Connection

    private val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())
    private lateinit var jwtStub: JwtStub

    private lateinit var app: ApplicationBuilder
    private lateinit var appBaseUrl: String

    private fun applicationConfig(wiremockBaseUrl: String, port: Int): Map<String, String> {
        return mapOf(
            "KAFKA_APP_ID" to kafkaApplicationId,
            "KAFKA_BOOTSTRAP_SERVERS" to embeddedKafkaEnvironment.brokersURL,
            "KAFKA_USERNAME" to username,
            "KAFKA_PASSWORD" to password,
            "KAFKA_COMMIT_INTERVAL_MS_CONFIG" to "100", // Consumer commit interval must be low because we want quick feedback in the [assertMessageIsConsumed] method
            "DATABASE_JDBC_URL" to embeddedPostgres.getJdbcUrl("postgres", "postgres"),
            "AZURE_CONFIG_URL" to "$wiremockBaseUrl/config",
            "AZURE_CLIENT_ID" to "spleis_azure_ad_app_id",
            "AZURE_CLIENT_SECRET" to "el_secreto",
            "AZURE_REQUIRED_GROUP" to "sykepenger-saksbehandler-gruppe",
            "HTTP_PORT" to "$port"
        )
    }

    private fun producerProperties() =
        Properties().apply {
            put(BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaEnvironment.brokersURL)
            put(SECURITY_PROTOCOL_CONFIG, "PLAINTEXT")
            // Make sure our producer waits until the message is received by Kafka before returning. This is to make sure the tests can send messages in a specific order
            put(ACKS_CONFIG, "all")
            put(MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
            put(LINGER_MS_CONFIG, "0")
            put(RETRIES_CONFIG, "0")
            put(SASL_MECHANISM, "PLAIN")
        }

    @BeforeAll
    internal fun `start embedded environment`() {
        embeddedPostgres = EmbeddedPostgres.builder().start()
        postgresConnection = embeddedPostgres.postgresDatabase.connection

        embeddedKafkaEnvironment.start()
        adminClient = embeddedKafkaEnvironment.adminClient ?: fail("Klarte ikke få tak i adminclient")
        kafkaProducer = KafkaProducer(
            producerProperties(), StringSerializer(), StringSerializer()
        )

        //Stub ID provider (for authentication of REST endpoints)
        wireMockServer.start()
        jwtStub = JwtStub("Microsoft Azure AD", wireMockServer)
        stubFor(jwtStub.stubbedJwkProvider())
        stubFor(jwtStub.stubbedConfigProvider())

        val port = randomPort()
        appBaseUrl = "http://localhost:$port"
        app = ApplicationBuilder(
            applicationConfig(
                wireMockServer.baseUrl(),
                port
            )
        )

        GlobalScope.launch { app.start() }

        // send one initial message and wait for the application to commit the offsets (i.e. the app is ready)
        sendKafkaMessage(rapidTopic, "key", "{}")
            .get()
            .assertMessageIsConsumed(10L)
    }

    @AfterAll
    internal fun `stop embedded environment`() {
        app.stop()
        wireMockServer.stop()
        adminClient.close()
        embeddedKafkaEnvironment.tearDown()

        postgresConnection.close()
        embeddedPostgres.close()
    }

    @Test
    fun `rest apis`() {
        ("/api/person/aktørId").httpGet(HttpStatusCode.NotFound)
        ("/api/utbetaling/utbetalingsreferanse").httpGet(HttpStatusCode.NotFound)
    }

    private fun String.httpGet(expectedStatus: HttpStatusCode = HttpStatusCode.OK, testBlock: String.() -> Unit = {}) {
        val token = jwtStub.createTokenFor(
            subject = "en_saksbehandler_ident",
            groups = listOf("sykepenger-saksbehandler-gruppe"),
            audience = "spleis_azure_ad_app_id"
        )

        val connection = appBaseUrl.handleRequest(HttpMethod.Get, this,
            builder = {
                setRequestProperty(Authorization, "Bearer $token")
            })

        assertEquals(expectedStatus.value, connection.responseCode)
        connection.responseBody.testBlock()
    }

    private fun sendKafkaMessage(topic: String, key: String, message: String) =
        kafkaProducer.send(ProducerRecord(topic, key, message))

    private fun RecordMetadata.assertMessageIsConsumed(timeoutSeconds: Long) {
        await()
            .atMost(timeoutSeconds, SECONDS)
            .until {
                val offsetAndMetadataMap =
                    adminClient.listConsumerGroupOffsets(
                        kafkaApplicationId
                    ).partitionsToOffsetAndMetadata().get()
                val topicPartition = TopicPartition(this.topic(), this.partition())
                val currentPositionOfSentMessage = this.offset()
                val currentConsumerGroupPosition = offsetAndMetadataMap[topicPartition]?.offset() ?: -1

                currentConsumerGroupPosition > currentPositionOfSentMessage
            }
    }
}
