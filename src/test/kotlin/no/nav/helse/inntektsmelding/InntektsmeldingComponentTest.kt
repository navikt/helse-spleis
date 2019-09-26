package no.nav.helse.inntektsmelding

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.util.KtorExperimentalAPI
import io.prometheus.client.CollectorRegistry
import no.nav.common.KafkaEnvironment
import no.nav.helse.Topics.inntektsmeldingTopic
import no.nav.helse.Topics.sykmeldingTopic
import no.nav.helse.Topics.søknadTopic
import no.nav.helse.inntektsmelding.InntektsmeldingConsumer.Companion.inntektsmeldingObjectMapper
import no.nav.helse.inntektsmelding.InntektsmeldingProbe.Companion.innteksmeldingerMottattCounterName
import no.nav.helse.inntektsmelding.domain.Inntektsmelding
import no.nav.helse.readResource
import no.nav.helse.sakskompleks.SakskompleksDao
import no.nav.helse.sakskompleks.SakskompleksProbe.Companion.innteksmeldingKobletTilSakCounterName
import no.nav.helse.sakskompleks.SakskompleksProbe.Companion.manglendeSakskompleksForInntektsmeldingCounterName
import no.nav.helse.sakskompleks.SakskompleksProbe.Companion.sakskompleksTotalsCounterName
import no.nav.helse.sakskompleks.SakskompleksService
import no.nav.helse.serde.JsonNodeSerializer
import no.nav.helse.sykmelding.SykmeldingConsumer.Companion.sykmeldingObjectMapper
import no.nav.helse.sykmelding.domain.SykmeldingMessage
import no.nav.helse.sykmelding.domain.gjelderFra
import no.nav.helse.sykmelding.domain.gjelderTil
import no.nav.helse.søknad.SøknadConsumer.Companion.søknadObjectMapper
import no.nav.helse.søknad.SøknadProbe.Companion.søknadCounterName
import no.nav.helse.søknad.domain.Sykepengesøknad
import no.nav.helse.testServer
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.StringSerializer
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.sql.Connection
import java.util.*
import java.util.concurrent.TimeUnit

@KtorExperimentalAPI
class InntektsmeldingComponentTest {

    companion object {

        private val embeddedEnvironment = KafkaEnvironment(
                autoStart = false,
                withSchemaRegistry = false,
                withSecurity = false,
                topicNames = listOf(sykmeldingTopic, inntektsmeldingTopic, søknadTopic)
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

        sakskompleksService = SakskompleksService(SakskompleksDao(embeddedPostgres.postgresDatabase))
    }

    @AfterEach
    fun `stop postgres`() {
        postgresConnection.close()
        embeddedPostgres.close()
    }

    private lateinit var sakskompleksService: SakskompleksService

    private val enSykmeldingSomJson = sykmeldingObjectMapper.readTree("/sykmelding.json".readResource())
    private val enSykmelding = SykmeldingMessage(enSykmeldingSomJson).sykmelding

    private val enSøknadSomJson = søknadObjectMapper.readTree("/søknad_arbeidstaker_sendt_nav.json".readResource())
    private val enSøknad = Sykepengesøknad(enSøknadSomJson)

    private val enInntektsmeldingSomJson = inntektsmeldingObjectMapper.readTree("/inntektsmelding.json".readResource())
    private val enInntektsmelding = Inntektsmelding(enInntektsmeldingSomJson)

    @Test
    fun `Testdataene stemmer overens`() {
        assertEquals(enSykmelding.aktørId, enInntektsmelding.arbeidstakerAktorId)
        assertEquals(enSøknad.aktørId, enInntektsmelding.arbeidstakerAktorId)

        val sykmeldingPeriode = enSykmelding.gjelderFra().rangeTo(enSykmelding.gjelderTil())
        assertTrue(sykmeldingPeriode.contains(enInntektsmelding.sisteDagIArbeidsgiverPeriode!!))
    }

    @Test
    fun `Inntektsmelding blir lagt til sakskompleks med kun sykmelding`() {
        val sakskompleksDao = SakskompleksDao(embeddedPostgres.postgresDatabase)

        testServer(config = mapOf(
            "KAFKA_BOOTSTRAP_SERVERS" to embeddedEnvironment.brokersURL,
            "DATABASE_JDBC_URL" to embeddedPostgres.getJdbcUrl("postgres", "postgres")
        )) {
            val sakskompleksCounterBefore = getCounterValue(sakskompleksTotalsCounterName)
            val inntektsmeldingMotattCounterBefore = getCounterValue(innteksmeldingerMottattCounterName)
            val inntektsmeldingKobletTilSakCounterBefore = getCounterValue(innteksmeldingKobletTilSakCounterName)

            produceOneMessage(sykmeldingTopic, enSykmelding.id, enSykmeldingSomJson, sykmeldingObjectMapper)

            await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted {
                    val sakskompleksCounterAfter = getCounterValue(sakskompleksTotalsCounterName)

                    val sakerForBruker = sakskompleksDao.finnSaker(enSykmelding.aktørId)
                    assertEquals(1, sakerForBruker.size)

                    assertEquals(1, sakskompleksCounterAfter - sakskompleksCounterBefore)
                }

            produceOneMessage(inntektsmeldingTopic, enInntektsmelding.inntektsmeldingId, enInntektsmelding.toJsonNode())

            await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted {
                    val inntektsmeldingMottattCounterAfter = getCounterValue(innteksmeldingerMottattCounterName)
                    val inntektsmeldingKobletTilSakCounterAfter = getCounterValue(innteksmeldingKobletTilSakCounterName)

                    val sakerForBruker = sakskompleksDao.finnSaker(enSykmelding.aktørId)
                    assertEquals(1, sakerForBruker.size)

                    assertTrue(sakerForBruker[0].har(enInntektsmelding))
                    assertTrue(sakerForBruker[0].har(enSykmelding))

                    assertEquals(1, inntektsmeldingMottattCounterAfter - inntektsmeldingMotattCounterBefore)
                    assertEquals(1, inntektsmeldingKobletTilSakCounterAfter - inntektsmeldingKobletTilSakCounterBefore)
                }
        }
    }

    @Test
    fun `Inntektsmelding blir lagt til sakskompleks med både sykmelding og søknad`() {
        val sakskompleksDao = SakskompleksDao(embeddedPostgres.postgresDatabase)

        testServer(config = mapOf(
            "KAFKA_BOOTSTRAP_SERVERS" to embeddedEnvironment.brokersURL,
            "DATABASE_JDBC_URL" to embeddedPostgres.getJdbcUrl("postgres", "postgres")
        )) {
            val sakskompleksCounterBefore = getCounterValue(sakskompleksTotalsCounterName)
            val søknadCounterBefore = getCounterValue(søknadCounterName)
            val inntektsmeldingMottattCounterBefore = getCounterValue(innteksmeldingerMottattCounterName)
            val inntektsmeldingKobletTilSakCounterBefore = getCounterValue(innteksmeldingKobletTilSakCounterName)

            produceOneMessage(sykmeldingTopic, enSykmelding.id, enSykmeldingSomJson, sykmeldingObjectMapper)

            await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted {
                    val sakskompleksCounterAfter = getCounterValue(sakskompleksTotalsCounterName)

                    val sakerForBruker = sakskompleksDao.finnSaker(enSykmelding.aktørId)
                    assertEquals(1, sakerForBruker.size)
                    assertTrue(sakerForBruker[0].har(enSykmelding))
                    assertFalse(sakerForBruker[0].har(enSøknad))
                    assertFalse(sakerForBruker[0].har(enInntektsmelding))

                    assertEquals(1, sakskompleksCounterAfter - sakskompleksCounterBefore)
                }

            produceOneMessage(søknadTopic, enSøknadSomJson["id"].asText(), enSøknadSomJson, søknadObjectMapper)

            await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted {
                    val søknadCounterAfter = getCounterValue(søknadCounterName)

                    val sakerForBruker = sakskompleksDao.finnSaker(enSykmelding.aktørId)
                    assertEquals(1, sakerForBruker.size)
                    assertTrue(sakerForBruker[0].har(enSykmelding))
                    assertTrue(sakerForBruker[0].har(enSøknad))
                    assertFalse(sakerForBruker[0].har(enInntektsmelding))

                    assertEquals(1, søknadCounterAfter - søknadCounterBefore)
                }

            produceOneMessage(inntektsmeldingTopic, enInntektsmelding.inntektsmeldingId, enInntektsmelding.toJsonNode())

            await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted {
                    val inntektsmeldingMottattCounterAfter = getCounterValue(innteksmeldingerMottattCounterName)
                    val inntektsmeldingKobletTilSakCounterAfter = getCounterValue(innteksmeldingKobletTilSakCounterName)

                    val sakerForBruker = sakskompleksDao.finnSaker(enSykmelding.aktørId)
                    assertEquals(1, sakerForBruker.size)
                    assertTrue(sakerForBruker[0].har(enSykmelding))
                    assertTrue(sakerForBruker[0].har(enSøknad))
                    assertTrue(sakerForBruker[0].har(enInntektsmelding))

                    assertEquals(1, inntektsmeldingMottattCounterAfter - inntektsmeldingMottattCounterBefore)
                }
        }
    }

    @Test
    fun `inntektsmelding som kommer først, blir ignorert`() {
        val sakskompleksDao = SakskompleksDao(embeddedPostgres.postgresDatabase)

        testServer(config = mapOf(
            "KAFKA_BOOTSTRAP_SERVERS" to embeddedEnvironment.brokersURL,
            "DATABASE_JDBC_URL" to embeddedPostgres.getJdbcUrl("postgres", "postgres")
        )) {

            val inntektsmeldingMottattCounterBefore = getCounterValue(innteksmeldingerMottattCounterName)
            val manglendeSakskompleksForInntektsmeldingCounterBefore = getCounterValue(manglendeSakskompleksForInntektsmeldingCounterName)

            produceOneMessage(inntektsmeldingTopic, enInntektsmelding.inntektsmeldingId, enInntektsmelding.toJsonNode())

            await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted {
                    val inntektsmeldingMottattCounterAfter = getCounterValue(innteksmeldingerMottattCounterName)
                    val manglendeSakskompleksForInntektsmeldingCounterAfter = getCounterValue(manglendeSakskompleksForInntektsmeldingCounterName)

                    val sakerForBruker = sakskompleksDao.finnSaker(enSykmelding.aktørId)
                    assertEquals(0, sakerForBruker.size)

                    assertEquals(1, inntektsmeldingMottattCounterAfter - inntektsmeldingMottattCounterBefore)
                    assertEquals(1, manglendeSakskompleksForInntektsmeldingCounterAfter - manglendeSakskompleksForInntektsmeldingCounterBefore)
                }
        }
    }

    private fun Inntektsmelding.toJsonNode(): JsonNode = inntektsmeldingObjectMapper.valueToTree(this)

    private fun produceOneMessage(topic: String, key: String, message: JsonNode, objectMapper: ObjectMapper = inntektsmeldingObjectMapper) {
        val producer = KafkaProducer<String, JsonNode>(producerProperties(), StringSerializer(), JsonNodeSerializer(objectMapper))
        producer.send(ProducerRecord(topic, key, message))
        producer.flush()
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
