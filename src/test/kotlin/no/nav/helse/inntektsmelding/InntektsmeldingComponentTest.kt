package no.nav.helse.inntektsmelding

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.util.KtorExperimentalAPI
import io.prometheus.client.CollectorRegistry
import no.nav.common.JAASCredential
import no.nav.common.KafkaEnvironment
import no.nav.helse.inntektsmelding.InntektsmeldingProbe.Companion.innteksmeldingerTotalsCounterName
import no.nav.helse.serde.JsonNodeSerializer
import no.nav.helse.testServer
import no.nav.inntektsmelding.kontrakt.serde.JacksonJsonConfig
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
import org.junit.jupiter.api.Assertions.assertEquals
import java.math.BigDecimal
import java.sql.Connection
import java.time.LocalDate
import java.time.Month
import java.util.*
import java.util.concurrent.TimeUnit

@KtorExperimentalAPI
class InntektsmeldingComponentTest {

    companion object {

        private const val username = "srvkafkaclient"
        private const val password = "kafkaclient"

        // TODO kan disse hentes fra App.kt?
        const val sykemeldingTopic = "privat-syfo-sm2013-automatiskBehandling"
        const val soknadTopic = "syfo-soknad-v2"
        const val inntektsmeldingTopic = "privat-sykepenger-inntektsmelding"

        private val embeddedEnvironment = KafkaEnvironment(
                users = listOf(JAASCredential(username, password)),
                autoStart = false,
                withSchemaRegistry = false,
                withSecurity = true,
                topicNames = listOf(sykemeldingTopic, inntektsmeldingTopic, soknadTopic)
        )

        private val inntektsmeldingObjectMapper = JacksonJsonConfig.opprettObjectMapper()

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
    fun `behandler ikke søknad med status != SENDT`() {
        testServer(config = mapOf(
            "KAFKA_BOOTSTRAP_SERVERS" to embeddedEnvironment.brokersURL,
            "KAFKA_USERNAME" to username,
            "KAFKA_PASSWORD" to password,
            "sykmelding-kafka-topic" to sykemeldingTopic,
            "soknad-kafka-topic" to soknadTopic,
            "inntektsmelding-kafka-topic" to inntektsmeldingTopic,
            "DATABASE_JDBC_URL" to embeddedPostgres.getJdbcUrl("postgres", "postgres")
        )) {

            val inntektsmeldingCounterBefore = getCounterValue(innteksmeldingerTotalsCounterName)

            val inntektsmelding = Inntektsmelding(
                inntektsmeldingId = "1",
                arbeidstakerFnr= "12345678910",
                arbeidstakerAktorId= "1234567891011",
                virksomhetsnummer= "123456789",
                arbeidsgiverFnr= "10987654321",
                arbeidsgiverAktorId= "1110987654321",
                arbeidsgivertype= VIRKSOMHET,
                arbeidsforholdId= "42",
                beregnetInntekt= BigDecimal(10000.00),
                refusjon= Refusjon(),
                endringIRefusjoner= emptyList(),
                opphoerAvNaturalytelser= emptyList(),
                gjenopptakelseNaturalytelser= emptyList(),
                arbeidsgiverperioder= listOf(
                    Periode(
                        fom = LocalDate.of(2019, Month.APRIL, 1),
                        tom = LocalDate.of(2019, Month.APRIL, 16)
                    )
                ),
                status = GYLDIG,
                arkivreferanse = "ENARKIVREFERANSE"
            )

            produceOneMessage(inntektsmeldingTopic, inntektsmelding.inntektsmeldingId, inntektsmelding)

            await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted {
                    val inntektsmeldingCounterAfter = getCounterValue(innteksmeldingerTotalsCounterName)

                    assertEquals(1, inntektsmeldingCounterAfter - inntektsmeldingCounterBefore)
                }
        }
    }

    private fun produceOneMessage(topic: String, key: String, message: Inntektsmelding) {
        val producer = KafkaProducer<String, JsonNode>(producerProperties(), StringSerializer(), JsonNodeSerializer(inntektsmeldingObjectMapper))
        producer.send(ProducerRecord(topic, key, inntektsmeldingObjectMapper.valueToTree(message)))
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
