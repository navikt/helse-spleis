package no.nav.helse

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.util.KtorExperimentalAPI
import io.prometheus.client.CollectorRegistry
import no.nav.common.KafkaEnvironment
import no.nav.helse.TestConstants.søknad
import no.nav.helse.Topics.inntektsmeldingTopic
import no.nav.helse.Topics.sykmeldingTopic
import no.nav.helse.Topics.søknadTopic
import no.nav.helse.sakskompleks.SakskompleksDao
import no.nav.helse.serde.JsonNodeSerializer
import no.nav.helse.søknad.SøknadConsumer.Companion.søknadObjectMapper
import no.nav.helse.søknad.SøknadProbe.Companion.søknadCounterName
import no.nav.helse.søknad.SøknadProbe.Companion.søknaderIgnorertCounterName
import no.nav.helse.søknad.domain.Sykepengesøknad
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsstatusDTO
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.StringSerializer
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.util.*
import java.util.concurrent.TimeUnit

@Disabled("Denne logikken er ikke implementert uten sykmeldinger")
@KtorExperimentalAPI
class AppComponentTest {

    companion object {

        private val embeddedEnvironment = KafkaEnvironment(
                autoStart = false,
                withSchemaRegistry = false,
                withSecurity = false,
                topicNames = listOf(sykmeldingTopic, søknadTopic, inntektsmeldingTopic)
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

    @Disabled("Denne logikken er ikke implementert uten sykmeldinger")
    @Test
    fun `behandler ikke søknad om utlandsopphold`() {
        testServer(config = mapOf(
            "KAFKA_BOOTSTRAP_SERVERS" to embeddedEnvironment.brokersURL,
            "DATABASE_JDBC_URL" to embeddedPostgres.getJdbcUrl("postgres", "postgres")
        )) {

            val søknadCounterBefore = getCounterValue(søknadCounterName)
            val søknadIgnorertCounterBefore = getCounterValue(søknaderIgnorertCounterName)

            val søknad = søknadObjectMapper.readTree("/søknad_om_utlandsopphold.json".readResource())
            produceOneMessage(søknadTopic, søknad["id"].asText(), søknad)

            await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted {
                    val søknadCounterAfter = getCounterValue(søknadCounterName)
                    val søknadIgnorertCounterAfter = getCounterValue(søknaderIgnorertCounterName)

                    assertEquals(0, søknadCounterAfter - søknadCounterBefore)
                    assertEquals(1, søknadIgnorertCounterAfter - søknadIgnorertCounterBefore)
                }
        }
    }

    @Disabled("Denne logikken er ikke implementert uten sykmeldinger")
    @Test
    fun `behandler ikke søknad med status != SENDT`() {
        testServer(config = mapOf(
            "KAFKA_BOOTSTRAP_SERVERS" to embeddedEnvironment.brokersURL,
            "DATABASE_JDBC_URL" to embeddedPostgres.getJdbcUrl("postgres", "postgres")
        )) {

            val søknadCounterBefore = getCounterValue(søknadCounterName)
            val søknadIgnorertCounterBefore = getCounterValue(søknaderIgnorertCounterName)

            val søknad = søknadObjectMapper.readTree("/søknad_frilanser_ny.json".readResource())
            produceOneMessage(søknadTopic, søknad["id"].asText(), søknad)

            await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted {
                    val søknadCounterAfter = getCounterValue(søknadCounterName)
                    val søknadIgnorertCounterAfter = getCounterValue(søknaderIgnorertCounterName)

                    assertEquals(0, søknadCounterAfter - søknadCounterBefore)
                    assertEquals(1, søknadIgnorertCounterAfter - søknadIgnorertCounterBefore)
                }
        }
    }

    @Disabled("Denne logikken er ikke implementert uten sykmeldinger")
    @Test
    fun `kobler sendt søknad til eksisterende sakskompleks`() {
        val sakskompleksDao = SakskompleksDao(embeddedPostgres.postgresDatabase)

        testServer(config = mapOf(
            "KAFKA_BOOTSTRAP_SERVERS" to embeddedEnvironment.brokersURL,
            "DATABASE_JDBC_URL" to embeddedPostgres.getJdbcUrl("postgres", "postgres")
        )) {

            val nySøknadCounterBefore = getCounterValue(søknadCounterName, listOf("NY"))
            val sendtSøknadCounterBefore = getCounterValue(søknadCounterName, listOf("SENDT"))

            val nySøknad = søknad(status = SoknadsstatusDTO.NY)
            produceOneMessage(søknadTopic, nySøknad.id, søknadObjectMapper.valueToTree(nySøknad))

            await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted {
                    val nySøknadCounterAfter = getCounterValue(søknadCounterName, listOf("NY"))

                    assertEquals(1, nySøknadCounterAfter - nySøknadCounterBefore)

                    val sakerForBruker = sakskompleksDao.finnSaker(nySøknad.aktørId)
                    assertEquals(1, sakerForBruker.size)
                }

            val sendtSøknad = søknad(status = SoknadsstatusDTO.SENDT)
            produceOneMessage(søknadTopic, sendtSøknad.id, søknadObjectMapper.valueToTree(sendtSøknad))

            await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted {
                    val sendtSøknadCounterAfter = getCounterValue(søknadCounterName)

                    assertEquals(1, sendtSøknadCounterAfter - sendtSøknadCounterBefore)

                    val sakerForBruker = sakskompleksDao.finnSaker(søknad.aktørId)
                    assertEquals(1, sakerForBruker.size)

                    /*assertTrue(sakerForBruker[0].har(SykmeldingMessage(sykmelding).sykmelding))
                    assertTrue(sakerForBruker[0].har(Sykepengesøknad(søknad)))*/
                }
        }
    }

    @Disabled("Denne logikken er ikke implementert uten sykmeldinger")
    @Test
    fun `søknad uten tilhørende sykmelding ignoreres`() {
        val sakskompleksDao = SakskompleksDao(embeddedPostgres.postgresDatabase)

        testServer(config = mapOf(
            "KAFKA_BOOTSTRAP_SERVERS" to embeddedEnvironment.brokersURL,
            "DATABASE_JDBC_URL" to embeddedPostgres.getJdbcUrl("postgres", "postgres")
        )) {

            val søknadCounterBefore = getCounterValue(søknadCounterName)
            val søknad = søknadObjectMapper.readTree("/søknad_arbeidstaker_sendt_nav.json".readResource())
            produceOneMessage(søknadTopic, søknad["id"].asText(), søknad)

            await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted {
                    val søknadCounterAfter = getCounterValue(søknadCounterName)

                    assertEquals(1, søknadCounterAfter - søknadCounterBefore)

                    val sakerForBruker = sakskompleksDao.finnSaker(Sykepengesøknad(søknad).aktørId)
                    assertEquals(0, sakerForBruker.size)
                }
        }
    }

    private fun produceOneMessage(topic: String, key: String, message: JsonNode, objectMapper: ObjectMapper = søknadObjectMapper) {
        val producer = KafkaProducer<String, JsonNode>(producerProperties(), StringSerializer(), JsonNodeSerializer(objectMapper))
        producer.send(ProducerRecord(topic, key, message))
                .get(1, TimeUnit.SECONDS)
    }

    private fun producerProperties() =
            Properties().apply {
                put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, embeddedEnvironment.brokersURL)
                put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT")
                put(SaslConfigs.SASL_MECHANISM, "PLAIN")
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
