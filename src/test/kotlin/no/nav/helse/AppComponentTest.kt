package no.nav.helse

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.util.KtorExperimentalAPI
import io.prometheus.client.CollectorRegistry
import no.nav.common.JAASCredential
import no.nav.common.KafkaEnvironment
import no.nav.helse.sakskompleks.SakskompleksDao
import no.nav.helse.sakskompleks.SakskompleksService
import no.nav.helse.serde.JsonNodeSerializer
import no.nav.helse.sykmelding.domain.SykmeldingMessage
import no.nav.helse.søknad.domain.Sykepengesøknad
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.StringSerializer
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.util.Properties
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
    fun `behandler ikke søknad om utlandsopphold`() {
        testServer(config = mapOf(
            "KAFKA_BOOTSTRAP_SERVERS" to embeddedEnvironment.brokersURL,
            "KAFKA_USERNAME" to username,
            "KAFKA_PASSWORD" to password,
            "DATABASE_JDBC_URL" to embeddedPostgres.getJdbcUrl("postgres", "postgres")
        )) {

            val søknadCounterBefore = getCounterValue("soknader_totals")
            val søknadIgnorertCounterBefore = getCounterValue("soknader_ignorert_totals")

            val søknad = objectMapper.readTree("/søknad_om_utlandsopphold.json".readResource())
            produceOneMessage("syfo-soknad-v2", søknad["id"].asText(), søknad)

            await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted {
                    val søknadCounterAfter = getCounterValue("soknader_totals")
                    val søknadIgnorertCounterAfter = getCounterValue("soknader_ignorert_totals")

                    assertEquals(0, søknadCounterAfter - søknadCounterBefore)
                    assertEquals(1, søknadIgnorertCounterAfter - søknadIgnorertCounterBefore)
                }
        }
    }

    @Test
    fun `behandler ikke søknad med status != SENDT`() {
        testServer(config = mapOf(
            "KAFKA_BOOTSTRAP_SERVERS" to embeddedEnvironment.brokersURL,
            "KAFKA_USERNAME" to username,
            "KAFKA_PASSWORD" to password,
            "DATABASE_JDBC_URL" to embeddedPostgres.getJdbcUrl("postgres", "postgres")
        )) {

            val søknadCounterBefore = getCounterValue("soknader_totals")
            val søknadIgnorertCounterBefore = getCounterValue("soknader_ignorert_totals")

            val søknad = objectMapper.readTree("/søknad_frilanser_ny.json".readResource())
            produceOneMessage("syfo-soknad-v2", søknad["id"].asText(), søknad)

            await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted {
                    val søknadCounterAfter = getCounterValue("soknader_totals")
                    val søknadIgnorertCounterAfter = getCounterValue("soknader_ignorert_totals")

                    assertEquals(0, søknadCounterAfter - søknadCounterBefore)
                    assertEquals(1, søknadIgnorertCounterAfter - søknadIgnorertCounterBefore)
                }
        }
    }

    @Test
    fun `kobler søknad til eksisterende sakskompleks`() {
        val sakskompleksDao = SakskompleksDao(embeddedPostgres.postgresDatabase)
        val sakskompleksService = SakskompleksService(sakskompleksDao)

        testServer(config = mapOf(
            "KAFKA_BOOTSTRAP_SERVERS" to embeddedEnvironment.brokersURL,
            "KAFKA_USERNAME" to username,
            "KAFKA_PASSWORD" to password,
            "DATABASE_JDBC_URL" to embeddedPostgres.getJdbcUrl("postgres", "postgres")
        )) {

            val sykmeldingCounterBefore = getCounterValue("sykmeldinger_totals")
            val søknadCounterBefore = getCounterValue("soknader_totals")

            val sykmelding = objectMapper.readTree("/sykmelding.json".readResource())
            produceOneMessage("privat-syfo-sm2013-automatiskBehandling", sykmelding["sykmelding"]["id"].asText(), sykmelding)

            await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted {
                    val sykmeldingCounterAfter = getCounterValue("sykmeldinger_totals")

                    assertEquals(1, sykmeldingCounterAfter - sykmeldingCounterBefore)
                    assertNotNull(sakskompleksService.finnSak(SykmeldingMessage(sykmelding).sykmelding))
                }

            val søknad = objectMapper.readTree("/søknad_arbeidstaker_sendt_nav.json".readResource())
            produceOneMessage("syfo-soknad-v2", søknad["id"].asText(), søknad)

            await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted {
                    val søknadCounterAfter = getCounterValue("soknader_totals")

                    assertEquals(1, søknadCounterAfter - søknadCounterBefore)

                    val sak = sakskompleksService.finnSak(Sykepengesøknad(søknad))
                    assertEquals(listOf(SykmeldingMessage(sykmelding).sykmelding), sak?.sykmeldinger)
                    assertEquals(listOf(Sykepengesøknad(søknad)), sak?.søknader)
                }
        }
    }

    @Test
    fun `søknad uten tilhørende sykmelding ignoreres`() {
        val sakskompleksDao = SakskompleksDao(embeddedPostgres.postgresDatabase)
        val sakskompleksService = SakskompleksService(sakskompleksDao)

        testServer(config = mapOf(
            "KAFKA_BOOTSTRAP_SERVERS" to embeddedEnvironment.brokersURL,
            "KAFKA_USERNAME" to username,
            "KAFKA_PASSWORD" to password,
            "DATABASE_JDBC_URL" to embeddedPostgres.getJdbcUrl("postgres", "postgres")
        )) {

            val søknadCounterBefore = getCounterValue("soknader_totals")
            val søknad = objectMapper.readTree("/søknad_arbeidstaker_sendt_nav.json".readResource())
            produceOneMessage("syfo-soknad-v2", søknad["id"].asText(), søknad)

            await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted {
                    val søknadCounterAfter = getCounterValue("soknader_totals")

                    assertEquals(1, søknadCounterAfter - søknadCounterBefore)

                    val sak = sakskompleksService.finnSak(Sykepengesøknad(søknad))
                    assertNull(sak)
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
