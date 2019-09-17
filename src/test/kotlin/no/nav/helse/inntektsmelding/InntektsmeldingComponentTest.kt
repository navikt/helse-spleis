package no.nav.helse.inntektsmelding

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.util.KtorExperimentalAPI
import io.prometheus.client.CollectorRegistry
import no.nav.common.JAASCredential
import no.nav.common.KafkaEnvironment
import no.nav.helse.inntektsmelding.InntektsmeldingProbe.Companion.innteksmeldingKobletTilSakCounterName
import no.nav.helse.inntektsmelding.InntektsmeldingProbe.Companion.innteksmeldingerMottattCounterName
import no.nav.helse.inntektsmelding.InntektsmeldingProbe.Companion.manglendeSakskompleksForInntektsmeldingCounterName
import no.nav.helse.inntektsmelding.domain.sisteDagIArbeidsgiverPeriode
import no.nav.helse.inntektsmelding.serde.inntektsmeldingObjectMapper
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
import no.nav.inntektsmeldingkontrakt.Arbeidsgivertype.VIRKSOMHET
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.inntektsmeldingkontrakt.Refusjon
import no.nav.inntektsmeldingkontrakt.Status.GYLDIG
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.StringSerializer
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal
import java.sql.Connection
import java.util.*
import java.util.concurrent.TimeUnit

@KtorExperimentalAPI
class InntektsmeldingComponentTest {

    companion object {

        private const val username = "srvkafkaclient"
        private const val password = "kafkaclient"

        // TODO kan disse hentes fra App.kt?
        const val sykemeldingTopic = "privat-syfo-sm2013-automatiskBehandling"
        const val søknadTopic = "syfo-soknad-v2"
        const val inntektsmeldingTopic = "privat-sykepenger-inntektsmelding"

        private val embeddedEnvironment = KafkaEnvironment(
                users = listOf(JAASCredential(username, password)),
                autoStart = false,
                withSchemaRegistry = false,
                withSecurity = true,
                topicNames = listOf(sykemeldingTopic, inntektsmeldingTopic, søknadTopic)
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

    private val enInntektsmelding: Inntektsmelding
        get() {
            val inntektsmelding = Inntektsmelding(
                inntektsmeldingId = "1",
                arbeidstakerFnr = "12345678910",
                arbeidstakerAktorId = enSykmelding.aktørId,
                virksomhetsnummer = "123456789",
                arbeidsgiverFnr = "10987654321",
                arbeidsgiverAktorId = "1110987654321",
                arbeidsgivertype = VIRKSOMHET,
                arbeidsforholdId = "42",
                beregnetInntekt = BigDecimal("10000.01"),
                refusjon = Refusjon(),
                endringIRefusjoner = emptyList(),
                opphoerAvNaturalytelser = emptyList(),
                gjenopptakelseNaturalytelser = emptyList(),
                arbeidsgiverperioder = listOf(
                    Periode(
                        fom = enSykmelding.gjelderFra().minusDays(30),
                        tom = enSykmelding.gjelderFra()
                    )
                ),
                status = GYLDIG,
                arkivreferanse = "ENARKIVREFERANSE"
            )
            return inntektsmelding
        }

    @Test
    fun `Testdataene stemmer overens`() {
        assertEquals(enSykmelding.aktørId, enInntektsmelding.arbeidstakerAktorId)
        assertEquals(enSøknad.aktørId, enInntektsmelding.arbeidstakerAktorId)
        // TODO sjekk at det er samme arbeidsgiver på sykmeldingen og inntektsmeldingen - eller?

        val sykmeldingPeriode = enSykmelding.gjelderFra().rangeTo(enSykmelding.gjelderTil())
        assertTrue(sykmeldingPeriode.contains(enInntektsmelding.sisteDagIArbeidsgiverPeriode()!!))
    }

    @Test
    fun `Inntektsmelding blir lagt til sakskompleks med kun sykmelding`() {
        testServer(config = mapOf(
            "KAFKA_BOOTSTRAP_SERVERS" to embeddedEnvironment.brokersURL,
            "KAFKA_USERNAME" to username,
            "KAFKA_PASSWORD" to password,
            "sykmelding-kafka-topic" to sykemeldingTopic,
            "soknad-kafka-topic" to søknadTopic,
            "inntektsmelding-kafka-topic" to inntektsmeldingTopic,
            "DATABASE_JDBC_URL" to embeddedPostgres.getJdbcUrl("postgres", "postgres")
        )) {
            val sakskompleksCounterBefore = getCounterValue(sakskompleksTotalsCounterName)
            val inntektsmeldingMotattCounterBefore = getCounterValue(innteksmeldingerMottattCounterName)
            val inntektsmeldingKobletTilSakCounterBefore = getCounterValue(innteksmeldingKobletTilSakCounterName)

            produceOneMessage(sykemeldingTopic, enSykmelding.id, enSykmeldingSomJson, sykmeldingObjectMapper)

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

    // TODO Inntektsmelding blir lagt til sakskompleks med både sykmelding og søknad, og det resulterer i at sakskomplekset sendes til behandling
    @Test
    fun `Inntektsmelding blir lagt til sakskompleks med både sykmelding og søknad`() {
        testServer(config = mapOf(
            "KAFKA_BOOTSTRAP_SERVERS" to embeddedEnvironment.brokersURL,
            "KAFKA_USERNAME" to username,
            "KAFKA_PASSWORD" to password,
            "sykmelding-kafka-topic" to sykemeldingTopic,
            "soknad-kafka-topic" to søknadTopic,
            "inntektsmelding-kafka-topic" to inntektsmeldingTopic,
            "DATABASE_JDBC_URL" to embeddedPostgres.getJdbcUrl("postgres", "postgres")
        )) {
            val sakskompleksCounterBefore = getCounterValue(sakskompleksTotalsCounterName)
            val søknadCounterBefore = getCounterValue(søknadCounterName)
            val inntektsmeldingMottattCounterBefore = getCounterValue(innteksmeldingerMottattCounterName)
            val inntektsmeldingKobletTilSakCounterBefore = getCounterValue(innteksmeldingKobletTilSakCounterName)

            produceOneMessage(sykemeldingTopic, enSykmelding.id, enSykmeldingSomJson, sykmeldingObjectMapper)

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

                    assertEquals(1, inntektsmeldingMottattCounterAfter - inntektsmeldingMottattCounterBefore)
                    assertEquals(1, inntektsmeldingKobletTilSakCounterAfter - inntektsmeldingKobletTilSakCounterBefore)
                }
        }
    }

    //TODO inntektsmelding som kommer først, resulterer i manuell oppgave
    @Test
    fun `inntektsmelding som kommer først, blir ignorert`() {
        testServer(config = mapOf(
            "KAFKA_BOOTSTRAP_SERVERS" to embeddedEnvironment.brokersURL,
            "KAFKA_USERNAME" to username,
            "KAFKA_PASSWORD" to password,
            "sykmelding-kafka-topic" to sykemeldingTopic,
            "soknad-kafka-topic" to søknadTopic,
            "inntektsmelding-kafka-topic" to inntektsmeldingTopic,
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

    // TODO Inntektsmelding som ikke kommer i tide, fører til manuell oppgave

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
