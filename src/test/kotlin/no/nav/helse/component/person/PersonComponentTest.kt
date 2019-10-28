package no.nav.helse.component.person

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import no.nav.common.KafkaEnvironment
import no.nav.helse.TestConstants.inntektsmeldingDTO
import no.nav.helse.TestConstants.søknadDTO
import no.nav.helse.Topics.behovTopic
import no.nav.helse.Topics.inntektsmeldingTopic
import no.nav.helse.Topics.opprettGosysOppgaveTopic
import no.nav.helse.Topics.søknadTopic
import no.nav.helse.behov.Behov
import no.nav.helse.behov.BehovsTyper.Personopplysninger
import no.nav.helse.behov.BehovsTyper.Sykepengehistorikk
import no.nav.helse.createHikariConfig
import no.nav.helse.createTestApplicationConfig
import no.nav.helse.oppgave.GosysOppgaveProducer.OpprettGosysOppgaveDto
import no.nav.helse.runMigration
import no.nav.helse.serde.JsonNodeSerializer
import no.nav.helse.toJsonNode
import no.nav.syfo.kafka.sykepengesoknad.dto.ArbeidsgiverDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsstatusDTO
import org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG
import org.apache.kafka.clients.producer.ProducerConfig.LINGER_MS_CONFIG
import org.apache.kafka.clients.producer.ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION
import org.apache.kafka.clients.producer.ProducerConfig.RETRIES_CONFIG
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.time.Duration.ofSeconds
import java.util.HashMap
import java.util.Properties
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.SECONDS

@KtorExperimentalAPI
internal class PersonComponentTest {

    companion object {

        private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        private const val username = "srvkafkaclient"
        private const val password = "kafkaclient"
        private const val kafkaApplicationId = "spleis-v1"

        private val topics = listOf(søknadTopic, inntektsmeldingTopic, behovTopic, opprettGosysOppgaveTopic)
        // Use one partition per topic to make message sending more predictable
        private val topicInfos = topics.map { KafkaEnvironment.TopicInfo(it, partitions = 1) }

        private val embeddedKafkaEnvironment = KafkaEnvironment(
                autoStart = false,
                noOfBrokers = 1,
                topicInfos = topicInfos,
                withSchemaRegistry = false,
                withSecurity = false,
                topicNames = listOf(søknadTopic, inntektsmeldingTopic, behovTopic, opprettGosysOppgaveTopic)
        )

        private lateinit var adminClient: AdminClient
        private lateinit var kafkaConsumer: KafkaConsumer<String, String>
        private lateinit var kafkaProducer: KafkaProducer<String, JsonNode>

        private lateinit var embeddedPostgres: EmbeddedPostgres
        private lateinit var postgresConnection: Connection

        private lateinit var hikariConfig: HikariConfig

        private lateinit var embeddedServer: ApplicationEngine

        private fun applicationConfig(): Map<String, String> {
            return mapOf(
                    "KAFKA_APP_ID" to kafkaApplicationId,
                    "KAFKA_BOOTSTRAP_SERVERS" to embeddedKafkaEnvironment.brokersURL,
                    "KAFKA_USERNAME" to username,
                    "KAFKA_PASSWORD" to password,
                    "KAFKA_COMMIT_INTERVAL_MS_CONFIG" to "100", // Consumer commit interval must be low because we want quick feedback in the [assertMessageIsConsumed] method
                    "DATABASE_JDBC_URL" to embeddedPostgres.getJdbcUrl("postgres", "postgres")
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

        private fun consumerProperties(): MutableMap<String, Any>? {
            return HashMap<String, Any>().apply {
                put(BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaEnvironment.brokersURL)
                put(SECURITY_PROTOCOL_CONFIG, "PLAINTEXT")
                put(SASL_MECHANISM, "PLAIN")
                put(GROUP_ID_CONFIG, "personComponentTest")
                put(AUTO_OFFSET_RESET_CONFIG, "earliest")
            }
        }

        @BeforeAll
        @JvmStatic
        internal fun `start embedded environment`() {
            embeddedPostgres = EmbeddedPostgres.builder().start()
            postgresConnection = embeddedPostgres.postgresDatabase.connection
            hikariConfig = createHikariConfig(embeddedPostgres.getJdbcUrl("postgres", "postgres"))
            runMigration(HikariDataSource(hikariConfig))

            embeddedKafkaEnvironment.start()
            adminClient = embeddedKafkaEnvironment.adminClient ?: fail("Klarte ikke få tak i adminclient")
            kafkaConsumer = KafkaConsumer(consumerProperties(), StringDeserializer(), StringDeserializer())
            kafkaConsumer.subscribe(topics)
            kafkaProducer = KafkaProducer<String, JsonNode>(producerProperties(), StringSerializer(), JsonNodeSerializer(objectMapper))

            embeddedServer = embeddedServer(Netty, createTestApplicationConfig(applicationConfig()))
                    .start(wait = false)
        }

        @AfterAll
        @JvmStatic
        internal fun `stop embedded environment`() {
            embeddedServer.stop(1, 1, TimeUnit.SECONDS)
            kafkaConsumer.unsubscribe()
            kafkaConsumer.close()
            adminClient.close()
            embeddedKafkaEnvironment.tearDown()

            postgresConnection.close()
            embeddedPostgres.close()
        }

    }

    @Test
    fun `innsendt Nysøknad, Søknad og Inntektmelding fører til at sykepengehistorikk blir etterspurt`() {
        val aktørID = "1234567890123"
        val virksomhetsnummer = "123456789"

        sendNySøknad(aktørID, virksomhetsnummer)
        sendSøknad(aktørID, virksomhetsnummer)
        sendInnteksmelding(aktørID, virksomhetsnummer)

        await()
                .atMost(5, SECONDS)
                .untilAsserted {
                    val records = kafkaConsumer.poll(ofSeconds(1))
                    assertBehov(records = records, aktørId = aktørID, virksomhetsnummer = virksomhetsnummer, typer = listOf(Sykepengehistorikk.name, Personopplysninger.name))
                    assertOpprettGosysOppgave(records = records, aktørId = aktørID)
                }
    }

    @Test
    fun `innsendt Nysøknad, Inntektmelding og Søknad fører til at sykepengehistorikk blir etterspurt`() {
        val aktørId2 = "0123456789012"
        val virksomhetsnummer2 = "012345678"

        sendNySøknad(aktørId2, virksomhetsnummer2)
        sendInnteksmelding(aktørId2, virksomhetsnummer2)
        sendSøknad(aktørId2, virksomhetsnummer2)

        await()
                .atMost(5, SECONDS)
                .untilAsserted {
                    val records = kafkaConsumer.poll(ofSeconds(1))
                    assertBehov(records = records, aktørId = aktørId2, virksomhetsnummer = virksomhetsnummer2, typer = listOf(Sykepengehistorikk.name, Personopplysninger.name))
                    assertOpprettGosysOppgave(records = records, aktørId = aktørId2)
                }
    }

    @Test
    fun `sendt søknad uten uten ny søknad først skal behandles manuelt av saksbehandler`() {
        val aktørID = "2345678901234"
        val virksomhetsnummer = "234567890"

        sendSøknad(aktørID, virksomhetsnummer)

        await()
                .atMost(5, SECONDS)
                .untilAsserted {
                    val records = kafkaConsumer.poll(ofSeconds(1))
                    assertOpprettGosysOppgave(records = records, aktørId = aktørID)
                }
    }

    private fun sendInnteksmelding(aktorID: String, virksomhetsnummer: String) {
        val inntektsMelding = inntektsmeldingDTO(aktørId = aktorID, virksomhetsnummer = virksomhetsnummer)
        synchronousSendKafkaMessage(inntektsmeldingTopic, inntektsMelding.inntektsmeldingId, inntektsMelding.toJsonNode())
    }

    private fun sendSøknad(aktorID: String, virksomhetsnummer: String) {
        val sendtSøknad = søknadDTO(aktørId = aktorID, arbeidsgiver = ArbeidsgiverDTO(orgnummer = virksomhetsnummer), status = SoknadsstatusDTO.SENDT)
        synchronousSendKafkaMessage(søknadTopic, sendtSøknad.id!!, sendtSøknad.toJsonNode())
    }

    private fun sendNySøknad(aktorID: String, virksomhetsnummer: String) {
        val nySøknad = søknadDTO(aktørId = aktorID, arbeidsgiver = ArbeidsgiverDTO(orgnummer = virksomhetsnummer), status = SoknadsstatusDTO.NY)
        synchronousSendKafkaMessage(søknadTopic, nySøknad.id!!, nySøknad.toJsonNode())
    }

    private fun assertBehov(records: ConsumerRecords<String, String>, virksomhetsnummer: String, aktørId: String, typer: List<String>) {
        val behov = records.records(behovTopic).map { Behov.fromJson(it.value()) }.filter { it.get<String>("aktørId").equals(aktørId) }

        assertEquals(typer.size, behov.size, "Antall meldinger på topic $behovTopic skulle vært ${typer.size}, men var ${records?.records(behovTopic)?.count()}")
        assertTrue(behov.all { aktørId == it["aktørId"] })
        assertTrue(behov.all { virksomhetsnummer == it["organisasjonsnummer"] })
        assertTrue(behov.all { typer.contains(it.behovType()) })
    }

    private fun assertOpprettGosysOppgave(records: ConsumerRecords<String, String>, aktørId: String) {
        val opprettGosysOppgaveList = records.records(opprettGosysOppgaveTopic).map { objectMapper.readValue<OpprettGosysOppgaveDto>(it.value()) }
        assertTrue(opprettGosysOppgaveList.any { aktørId == it.aktorId })
    }

    /**
     * Trick Kafka into behaving synchronously by sending the message, and then confirming that it is read by the consumer group
     */
    private fun synchronousSendKafkaMessage(topic: String, key: String, message: JsonNode) {
        val metadata = kafkaProducer.send(ProducerRecord(topic, key, message))
        kafkaProducer.flush()
        metadata.get().assertMessageIsConsumed()
    }

    /**
     * Check that the consumers has received this message, by comparing the position of the message with the reported last read message of the consumer group
     */
    private fun RecordMetadata.assertMessageIsConsumed(recordMetadata: RecordMetadata = this) {
        await()
                .atMost(5, SECONDS)
                .untilAsserted {
                    val admin = adminClient.listConsumerGroupOffsets(kafkaApplicationId).partitionsToOffsetAndMetadata().get()
                    val offsetAndMetadataMap = admin
                    val topicPartition = TopicPartition(recordMetadata.topic(), recordMetadata.partition())
                    val currentPositionOfSentMessage = recordMetadata.offset()
                    val currentConsumerGroupPosition = offsetAndMetadataMap[topicPartition]?.offset()?.minus(1)
                            ?: fail() // This offset represents next position to read from, so we subtract 1 to get the last read offset
                    assertEquals(currentConsumerGroupPosition, currentPositionOfSentMessage)
                }
    }

}
