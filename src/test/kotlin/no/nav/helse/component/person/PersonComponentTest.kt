package no.nav.helse.component.person

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.util.KtorExperimentalAPI
import no.nav.common.KafkaEnvironment
import no.nav.helse.TestConstants.inntektsmeldingDTO
import no.nav.helse.TestConstants.søknadDTO
import no.nav.helse.Topics.behovTopic
import no.nav.helse.Topics.inntektsmeldingTopic
import no.nav.helse.Topics.søknadTopic
import no.nav.helse.behov.Behov
import no.nav.helse.behov.BehovsTyper.Personopplysninger
import no.nav.helse.behov.BehovsTyper.Sykepengehistorikk
import no.nav.helse.createHikariConfig
import no.nav.helse.sakskompleks.db.runMigration
import no.nav.helse.serde.JsonNodeSerializer
import no.nav.helse.testServer
import no.nav.helse.toJsonNode
import no.nav.syfo.kafka.sykepengesoknad.dto.ArbeidsgiverDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsstatusDTO
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit

@KtorExperimentalAPI
internal class PersonComponentTest {

    companion object {

        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        private const val username = "srvkafkaclient"
        private const val password = "kafkaclient"

        private val embeddedKafkaEnvironment = KafkaEnvironment(
            autoStart = false,
            withSchemaRegistry = false,
            withSecurity = false,
            topicNames = listOf(søknadTopic, inntektsmeldingTopic, behovTopic)
        )

        private val kafkaProducer = KafkaProducer<String, JsonNode>(producerProperties(), StringSerializer(), JsonNodeSerializer(objectMapper))

        private lateinit var embeddedPostgres: EmbeddedPostgres
        private lateinit var postgresConnection: Connection

        private lateinit var hikariConfig: HikariConfig

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

        private fun producerProperties() =
            Properties().apply {
                put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaEnvironment.brokersURL)
                put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT")
                put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
                put(ProducerConfig.RETRIES_CONFIG, "0")
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

    @Test
    fun `innsendt Nysøknad, Søknad og Inntektmelding fører til at sykepengehistorikk blir etterspurt`() {

        testServer(shutdownTimeoutMs = 40000, config = mapOf(
            "KAFKA_BOOTSTRAP_SERVERS" to embeddedKafkaEnvironment.brokersURL,
            "KAFKA_USERNAME" to username,
            "KAFKA_PASSWORD" to password,
            "DATABASE_JDBC_URL" to embeddedPostgres.getJdbcUrl("postgres", "postgres")
        )) {

            // Alt 1: Inntektsmelding kommer sist
            val aktørID = "1234567890123"
            val virksomhetsnummer = "123456789"

            ventTilMeldingErSendt(id = sendNySøknad(aktørID, virksomhetsnummer), topic = søknadTopic)
            ventTilMeldingErSendt(id = sendSøknad(aktørID, virksomhetsnummer), topic = søknadTopic)
            ventTilMeldingErSendt(id = sendInnteksmelding(aktørID, virksomhetsnummer), topic = inntektsmeldingTopic)

            assertBehov(aktørId = aktørID, virksomhetsnummer = virksomhetsnummer, typer = listOf(Sykepengehistorikk.name, Personopplysninger.name))

            // Alt 2: Sendt søknad kommer sist
            val aktørId2 = "0123456789012"
            val virksomhetsnummer2 = "012345678"

            ventTilMeldingErSendt(id = sendNySøknad(aktørId2, virksomhetsnummer2), topic = søknadTopic)
            ventTilMeldingErSendt(id = sendInnteksmelding(aktørId2, virksomhetsnummer2), topic = inntektsmeldingTopic)
            ventTilMeldingErSendt(id = sendSøknad(aktørId2, virksomhetsnummer2), topic = søknadTopic)

            assertBehov(aktørId = aktørId2, virksomhetsnummer = virksomhetsnummer2, typer = listOf(Sykepengehistorikk.name, Personopplysninger.name))
        }
    }



    private fun ventTilMeldingErSendt(topic: String, id: String) {
        val resultConsumer = KafkaConsumer(consumerProperties(), StringDeserializer(), StringDeserializer())
        resultConsumer.subscribe(listOf(topic))
        assertMessageConsumed(resultConsumer, topic, id)
        resultConsumer.unsubscribe()
    }

    private fun sendInnteksmelding(aktorID: String, virksomhetsnummer: String): String {
        val inntektsMelding = inntektsmeldingDTO(aktørId = aktorID, virksomhetsnummer = virksomhetsnummer)
        sendKafkaMessage(inntektsmeldingTopic, inntektsMelding.inntektsmeldingId, inntektsMelding.toJsonNode())
        return inntektsMelding.inntektsmeldingId
    }

    private fun sendSøknad(aktorID: String, virksomhetsnummer: String): String {
        val sendtSøknad = søknadDTO(aktørId = aktorID, arbeidsgiver = ArbeidsgiverDTO(orgnummer = virksomhetsnummer), status = SoknadsstatusDTO.SENDT)
        sendKafkaMessage(søknadTopic, sendtSøknad.id!!, sendtSøknad.toJsonNode())
        return sendtSøknad.id!!
    }

    private fun sendNySøknad(aktorID: String, virksomhetsnummer: String): String {
        val nySøknad = søknadDTO(aktørId = aktorID, arbeidsgiver = ArbeidsgiverDTO(orgnummer = virksomhetsnummer), status = SoknadsstatusDTO.NY)
        sendKafkaMessage(søknadTopic, nySøknad.id!!, nySøknad.toJsonNode())
        return nySøknad.id!!
    }

    private fun <K, V> assertMessageConsumed(resultConsumer: KafkaConsumer<K, V>, topic: String, id: String): List<ConsumerRecord<K, V>> {
        var record: ConsumerRecords<K, V>? = null
        await()
            .atMost(30, TimeUnit.SECONDS)
            .untilAsserted {
                record = resultConsumer.poll(Duration.ofSeconds(1))
                assertTrue(record?.records(topic)?.any {
                    it.key()?.equals(id) ?: false
                }
                    ?: false, "Antall meldinger på topic $topic skulle vært 1, men var ${record?.records(topic)?.count()}")
            }
        return record?.records(topic)?.toList() ?: emptyList()
    }

    private fun assertBehov(virksomhetsnummer: String, aktørId: String, typer: List<String>) {
        val resultConsumer = KafkaConsumer(consumerProperties(), StringDeserializer(), StringDeserializer())
        resultConsumer.subscribe(listOf(behovTopic))

        await()
            .atMost(30, TimeUnit.SECONDS)
            .untilAsserted {
                val records = resultConsumer.poll(Duration.ofSeconds(1))
                val behov = records.records(behovTopic).map { Behov.fromJson(it.value()) }.filter { it.get<String>("aktørId").equals(aktørId) }

                assertEquals(typer.size, behov.size, "Antall meldinger på topic $behovTopic skulle vært ${typer.size}, men var ${records?.records(behovTopic)?.count()}")
                assertTrue(behov.all { aktørId.equals(it.get("aktørId")) })
                assertTrue(behov.all { virksomhetsnummer.equals(it.get("organisasjonsnummer")) })
                assertTrue(behov.all { typer.contains(it.behovType()) })

            }

        resultConsumer.unsubscribe()
    }

    private fun sendKafkaMessage(topic: String, key: String, message: JsonNode) {
        kafkaProducer.send(ProducerRecord(topic, key, message)).get()
        kafkaProducer.flush()
    }

}
