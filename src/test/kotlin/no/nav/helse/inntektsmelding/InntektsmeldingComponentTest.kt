package no.nav.helse.inntektsmelding

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.util.KtorExperimentalAPI
import io.prometheus.client.CollectorRegistry
import no.nav.common.JAASCredential
import no.nav.common.KafkaEnvironment
import no.nav.helse.Topics.inntektsmeldingTopic
import no.nav.helse.Topics.sykmeldingTopic
import no.nav.helse.Topics.søknadTopic
import no.nav.helse.inntektsmelding.InntektsmeldingConsumer.Companion.inntektsmeldingObjectMapper
import no.nav.helse.inntektsmelding.InntektsmeldingProbe.Companion.innteksmeldingKobletTilSakCounterName
import no.nav.helse.inntektsmelding.InntektsmeldingProbe.Companion.innteksmeldingerMottattCounterName
import no.nav.helse.inntektsmelding.InntektsmeldingProbe.Companion.manglendeSakskompleksForInntektsmeldingCounterName
import no.nav.helse.inntektsmelding.domain.Inntektsmelding
import no.nav.helse.readResource
import no.nav.helse.sakskompleks.SakskompleksDao
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

        private const val username = "srvkafkaclient"
        private const val password = "kafkaclient"

        private val embeddedEnvironment = KafkaEnvironment(
                users = listOf(JAASCredential(username, password)),
                autoStart = false,
                withSchemaRegistry = false,
                withSecurity = true,
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
        testServer(config = mapOf(
            "KAFKA_BOOTSTRAP_SERVERS" to embeddedEnvironment.brokersURL,
            "KAFKA_USERNAME" to username,
            "KAFKA_PASSWORD" to password,
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

                    assertNotNull(sakskompleksService.finnSak(enSykmelding))
                    assertEquals(1, sakskompleksCounterAfter - sakskompleksCounterBefore)
                }

            produceOneMessage(inntektsmeldingTopic, enInntektsmelding.inntektsmeldingId, enInntektsmelding.toJsonNode())

            await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted {
                    val inntektsmeldingMottattCounterAfter = getCounterValue(innteksmeldingerMottattCounterName)
                    val inntektsmeldingKobletTilSakCounterAfter = getCounterValue(innteksmeldingKobletTilSakCounterName)

                    val lagretSak = sakskompleksService.finnSak(enInntektsmelding) ?: fail("Buhu - fant ikke sakskompleks :(")

                    assertEquals(sakskompleksService.finnSak(enSykmelding), lagretSak)

                    assertEquals(listOf(enInntektsmelding), lagretSak.inntektsmeldinger)
                    assertEquals(listOf(enSykmelding), lagretSak.sykmeldinger)
                    assertEquals(emptyList<Sykepengesøknad>(), lagretSak.søknader)

                    assertEquals(1, inntektsmeldingMottattCounterAfter - inntektsmeldingMotattCounterBefore)
                    assertEquals(1, inntektsmeldingKobletTilSakCounterAfter - inntektsmeldingKobletTilSakCounterBefore)
                }
        }
    }

    @Test
    fun `Inntektsmelding blir lagt til sakskompleks med både sykmelding og søknad`() {
        testServer(config = mapOf(
            "KAFKA_BOOTSTRAP_SERVERS" to embeddedEnvironment.brokersURL,
            "KAFKA_USERNAME" to username,
            "KAFKA_PASSWORD" to password,
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

                    assertNotNull(sakskompleksService.finnSak(enSykmelding))
                    assertEquals(1, sakskompleksCounterAfter - sakskompleksCounterBefore)
                }

            produceOneMessage(søknadTopic, enSøknadSomJson["id"].asText(), enSøknadSomJson, søknadObjectMapper)

            await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted {
                    val søknadCounterAfter = getCounterValue(søknadCounterName)

                    assertNotNull(sakskompleksService.finnSak(enSøknad))
                    assertEquals(1, søknadCounterAfter - søknadCounterBefore)
                }

            produceOneMessage(inntektsmeldingTopic, enInntektsmelding.inntektsmeldingId, enInntektsmelding.toJsonNode())

            await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted {
                    val inntektsmeldingMottattCounterAfter = getCounterValue(innteksmeldingerMottattCounterName)
                    val inntektsmeldingKobletTilSakCounterAfter = getCounterValue(innteksmeldingKobletTilSakCounterName)

                    val lagretSak = sakskompleksService.finnSak(enInntektsmelding) ?: fail("Buhu - fant ikke sakskompleks :(")
                    assertEquals(sakskompleksService.finnSak(enSykmelding), lagretSak)
                    assertEquals(sakskompleksService.finnSak(enSøknad), lagretSak)

                    assertEquals(listOf(enSykmelding), lagretSak.sykmeldinger)
                    assertEquals(listOf(enSøknad), lagretSak.søknader)
                    assertEquals(listOf(enInntektsmelding), lagretSak.inntektsmeldinger)
                    assertEquals(enInntektsmelding.jsonNode, lagretSak.inntektsmeldinger[0].jsonNode)

                    assertEquals(1, inntektsmeldingMottattCounterAfter - inntektsmeldingMottattCounterBefore)
                    assertEquals(1, inntektsmeldingKobletTilSakCounterAfter - inntektsmeldingKobletTilSakCounterBefore)
                }
        }
    }

    @Test
    fun `inntektsmelding som kommer først, blir ignorert`() {
        testServer(config = mapOf(
            "KAFKA_BOOTSTRAP_SERVERS" to embeddedEnvironment.brokersURL,
            "KAFKA_USERNAME" to username,
            "KAFKA_PASSWORD" to password,
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

                    assertEquals(1, inntektsmeldingMottattCounterAfter - inntektsmeldingMottattCounterBefore)
                    assertEquals(1, manglendeSakskompleksForInntektsmeldingCounterAfter - manglendeSakskompleksForInntektsmeldingCounterBefore)

                    assertNull(sakskompleksService.finnSak(enInntektsmelding))
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
