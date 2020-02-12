package no.nav.helse.e2e

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import no.nav.common.KafkaEnvironment
import no.nav.helse.*
import no.nav.helse.TestConstants.inntektsmeldingDTO
import no.nav.helse.TestConstants.søknadDTO
import no.nav.helse.Topics.inntektsmeldingTopic
import no.nav.helse.Topics.rapidTopic
import no.nav.helse.Topics.søknadTopic
import no.nav.helse.behov.Behovstype
import no.nav.helse.behov.Behovstype.*
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.TilstandType
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import no.nav.inntektsmeldingkontrakt.Refusjon
import no.nav.syfo.kafka.sykepengesoknad.dto.ArbeidsgiverDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsstatusDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.SykepengesoknadDTO
import org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig.*
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.time.Duration.ofMillis
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*
import java.util.concurrent.TimeUnit.SECONDS

internal class EndToEndTest {

    private companion object {

        private val timeoutSecondsPerStep = 5L

        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        private const val username = "srvkafkaclient"
        private const val password = "kafkaclient"
        private const val kafkaApplicationId = "spleis-v1"

        private val topics = Topics.hendelseKildeTopics + listOf(
            rapidTopic
        )
        // Use one partition per topic to make message sending more predictable
        private val topicInfos = topics.map { KafkaEnvironment.TopicInfo(it, partitions = 1) }

        private val embeddedKafkaEnvironment = KafkaEnvironment(
            autoStart = false,
            noOfBrokers = 1,
            topicInfos = topicInfos,
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

        private fun consumerProperties(): MutableMap<String, Any>? {
            return HashMap<String, Any>().apply {
                put(BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaEnvironment.brokersURL)
                put(SECURITY_PROTOCOL_CONFIG, "PLAINTEXT")
                put(SASL_MECHANISM, "PLAIN")
                put(GROUP_ID_CONFIG, "end-to-end-test")
                put(AUTO_OFFSET_RESET_CONFIG, "earliest")
            }
        }

        var apiport: Int = -1

        @BeforeAll
        @JvmStatic
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
            jwtStub =
                JwtStub(
                    "Microsoft Azure AD",
                    wireMockServer
                )
            stubFor(jwtStub.stubbedJwkProvider())
            stubFor(jwtStub.stubbedConfigProvider())

            val port = randomPort()
            apiport = port
            appBaseUrl = "http://localhost:$port"
            app = ApplicationBuilder(
                applicationConfig(
                    wireMockServer.baseUrl(),
                    port
                )
            ).apply {
                start()
            }
        }

        @AfterAll
        @JvmStatic
        internal fun `stop embedded environment`() {
            app.stop()
            wireMockServer.stop()
            TestConsumer.close()
            adminClient.close()
            embeddedKafkaEnvironment.tearDown()

            postgresConnection.close()
            embeddedPostgres.close()
        }

    }

    @BeforeEach
    fun `create test consumer`() {
        TestConsumer.reset()
    }

    @Test
    fun `happy path av hele modellen`() {
        val aktørID = "87654321962"
        val fødselsnummer = "01017000000"
        val virksomhetsnummer = "123456789"

        val søknad = sendNySøknad(aktørID, fødselsnummer, virksomhetsnummer)
        assertVedtaksperiodeEndretEvent(
            aktørId = aktørID,
            fødselsnummer = fødselsnummer,
            virksomhetsnummer = virksomhetsnummer,
            previousState = TilstandType.START,
            currentState = TilstandType.MOTTATT_NY_SØKNAD
        )
        sendSøknad(aktørID, fødselsnummer, virksomhetsnummer)
        assertVedtaksperiodeEndretEvent(
            aktørId = aktørID,
            fødselsnummer = fødselsnummer,
            virksomhetsnummer = virksomhetsnummer,
            previousState = TilstandType.MOTTATT_NY_SØKNAD,
            currentState = TilstandType.UNDERSØKER_HISTORIKK
        )
        sendInnteksmelding(aktørID, fødselsnummer, virksomhetsnummer)
        assertVedtaksperiodeEndretEvent(
            aktørId = aktørID,
            fødselsnummer = fødselsnummer,
            virksomhetsnummer = virksomhetsnummer,
            previousState = TilstandType.UNDERSØKER_HISTORIKK,
            currentState = TilstandType.AVVENTER_VILKÅRSPRØVING
        )

        sendVilkårsgrunnlagsløsning(aktørID, fødselsnummer)

        assertVedtaksperiodeEndretEvent(
            aktørId = aktørID,
            fødselsnummer = fødselsnummer,
            virksomhetsnummer = virksomhetsnummer,
            previousState = TilstandType.AVVENTER_VILKÅRSPRØVING,
            currentState = TilstandType.AVVENTER_HISTORIKK
        )

        val sykehistorikk = listOf(
            SpolePeriode(
                fom = søknad.fom!!.minusMonths(8),
                tom = søknad.fom!!.minusMonths(7),
                grad = "100"
            )
        )
        sendSykepengehistorikkløsning(aktørID, fødselsnummer, sykehistorikk)

        assertVedtaksperiodeEndretEvent(
            aktørId = aktørID,
            fødselsnummer = fødselsnummer,
            virksomhetsnummer = virksomhetsnummer,
            previousState = TilstandType.AVVENTER_HISTORIKK,
            currentState = TilstandType.AVVENTER_GODKJENNING
        )
        assertBehov(
            aktørId = aktørID,
            fødselsnummer = fødselsnummer,
            virksomhetsnummer = virksomhetsnummer,
            typer = listOf(GodkjenningFraSaksbehandler.name)
        )

        aktørID.hentPerson {
            assertTrue(this.contains("maksdato"))
            assertTrue(this.contains("utbetalingslinjer"))
            assertTrue(this.contains("dagsats"))
        }
    }

    @Test
    fun `gitt en periode til utbetaling, skal vi kunne hente opp personen via utbetalingsreferanse`() {
        val aktørID = "87659123421962"
        val fødselsnummer = "01018000000"
        val virksomhetsnummer = "123456789"

        val søknad = sendNySøknad(aktørID, fødselsnummer, virksomhetsnummer)
        sendSøknad(aktørID, fødselsnummer, virksomhetsnummer)
        sendInnteksmelding(aktørID, fødselsnummer, virksomhetsnummer)

        sendVilkårsgrunnlagsløsning(aktørID, fødselsnummer)

        val sykehistorikk = listOf(
            SpolePeriode(
                fom = søknad.fom!!.minusMonths(8),
                tom = søknad.fom!!.minusMonths(7),
                grad = "100"
            )
        )
        sendSykepengehistorikkløsning(aktørID, fødselsnummer, sykehistorikk)
        sendGodkjenningFraSaksbehandlerløsning(aktørID, fødselsnummer, true, "en_saksbehandler_ident")

        assertVedtaksperiodeEndretEvent(
            aktørId = aktørID,
            fødselsnummer = fødselsnummer,
            virksomhetsnummer = virksomhetsnummer,
            previousState = TilstandType.AVVENTER_GODKJENNING,
            currentState = TilstandType.TIL_UTBETALING
        )

        val utbetalingsbehov = ventPåBehov(aktørId = aktørID, fødselsnummer = fødselsnummer, behovType = Utbetaling)
        val utbetalingsreferanse: String = utbetalingsbehov["utbetalingsreferanse"].asText()

        utbetalingsreferanse.hentUtbetaling {
            assertTrue(this.contains(aktørID))
        }
    }

    @Test
    fun `gitt en ny søknad, så skal den kunne hentes ut på personen`() {
        val enAktørId = "1211109876233"
        val fødselsnummer = "01019000123"
        val virksomhetsnummer = "123456789"

        sendNySøknad(enAktørId, fødselsnummer, virksomhetsnummer)
        sendSøknad(enAktørId, fødselsnummer, virksomhetsnummer)
        sendInnteksmelding(enAktørId, fødselsnummer, virksomhetsnummer)
        sendVilkårsgrunnlagsløsning(enAktørId, fødselsnummer)
        sendSykepengehistorikkløsning(enAktørId, fødselsnummer, emptyList())

        assertVedtaksperiodeEndretEvent(fødselsnummer, virksomhetsnummer, enAktørId, TilstandType.AVVENTER_HISTORIKK, TilstandType.AVVENTER_GODKJENNING)

        enAktørId.hentPerson {
            assertEquals(3, objectMapper.readTree(this)["hendelser"].size())
        }
    }

    @Test
    fun `påminnelse for vedtaksperiode som ikke finnes`() {
        val enAktørId = "1211108676544"
        val fødselsnummer = "01019000000"
        val organisasjonsnummer = "123456789"

        val påminnelse = sendNyPåminnelse(enAktørId, fødselsnummer, organisasjonsnummer)

        await("Venter på beskjed om at vedtaksperiode ikke finnes")
            .atMost(30L, SECONDS)
            .untilAsserted {
                assertNotNull(
                    TestConsumer.records(rapidTopic)
                        .map { objectMapper.readTree(it.value()) }
                        .filter { it["@event_name"]?.asText() == "vedtaksperiode_ikke_funnet" }
                        .filter { enAktørId == it["aktørId"].textValue() }
                        .filter { fødselsnummer == it["fødselsnummer"].textValue() }
                        .filter { organisasjonsnummer == it["organisasjonsnummer"].textValue() }
                        .firstOrNull { påminnelse.vedtaksperiodeId() == it["vedtaksperiodeId"].textValue() }
                )
            }
    }

    private fun sendNyPåminnelse(aktørId: String, fødselsnummer: String, organisasjonsnummer: String): Påminnelse {
        val vedtaksperiodeId = UUID.randomUUID().toString()
        objectMapper.writeValueAsString(
            mapOf(
                "@event_name" to "påminnelse",
                "aktørId" to aktørId,
                "fødselsnummer" to fødselsnummer,
                "organisasjonsnummer" to organisasjonsnummer,
                "vedtaksperiodeId" to vedtaksperiodeId,
                "tilstand" to TilstandType.START.name,
                "antallGangerPåminnet" to 0,
                "tilstandsendringstidspunkt" to LocalDateTime.now().toString(),
                "påminnelsestidspunkt" to LocalDateTime.now().toString(),
                "nestePåminnelsestidspunkt" to LocalDateTime.now().toString()
            )
        ).also { sendKafkaMessage(rapidTopic, fødselsnummer, it) }
        return Påminnelse(
            hendelseId = UUID.randomUUID(),
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            antallGangerPåminnet = 1,
            tilstand = TilstandType.START,
            tilstandsendringstidspunkt = LocalDateTime.now(),
            påminnelsestidspunkt = LocalDateTime.now(),
            nestePåminnelsestidspunkt = LocalDateTime.now(),
            aktivitetslogger = Aktivitetslogger()
        )
    }

    private fun String.httpGet(testBlock: String.() -> Unit) {
        val token = jwtStub.createTokenFor(
            subject = "en_saksbehandler_ident",
            groups = listOf("sykepenger-saksbehandler-gruppe"),
            audience = "spleis_azure_ad_app_id"
        )

        val connection = appBaseUrl.handleRequest(HttpMethod.Get, this,
            builder = {
                setRequestProperty(Authorization, "Bearer $token")
            })

        assertEquals(HttpStatusCode.OK.value, connection.responseCode)

        connection.responseBody.testBlock()
    }

    private fun String.hentPerson(testBlock: String.() -> Unit) {
        ("/api/person/$this").httpGet(testBlock)
    }

    private fun String.hentUtbetaling(testBlock: String.() -> Unit) {
        ("/api/utbetaling/$this").httpGet(testBlock)
    }

    private fun sendSykepengehistorikkløsning(aktørId: String, fødselsnummer: String, perioder: List<SpolePeriode>) {
        val behov = ventPåBehov(aktørId, fødselsnummer, Sykepengehistorikk)

        assertNotNull(behov["utgangspunktForBeregningAvYtelse"])

        sendBehov(
            behov.løsBehov(
                mapOf(
                    "Sykepengehistorikk" to perioder,
                    "Foreldrepenger" to emptyMap<String, String>()
                )
            )
        )
    }

    private fun sendVilkårsgrunnlagsløsning(aktørId: String, fødselsnummer: String, egenAnsatt: Boolean = false) {
        val behov = ventPåBehov(aktørId, fødselsnummer, EgenAnsatt)

        sendBehov(
            behov.løsBehov(
                mapOf(
                    "EgenAnsatt" to egenAnsatt,
                    "Inntektsberegning" to (1.rangeTo(12)).map {
                        mapOf(
                            "årMåned" to YearMonth.of(2018, it).toString(),
                            "inntektsliste" to listOf(
                                mapOf(
                                    "beløp" to 31000.0
                                )
                            )
                        )
                    },
                    "Opptjening" to listOf(
                        mapOf(
                            "orgnummer" to "123456789",
                            "ansattSiden" to LocalDate.of(2010, 1, 1),
                            "ansattTil" to null
                        )
                    )
                )
            )
        )
    }

    private fun sendGodkjenningFraSaksbehandlerløsning(
        aktørId: String,
        fødselsnummer: String,
        utbetalingGodkjent: Boolean,
        saksbehandler: String
    ) {
        val behov = ventPåBehov(aktørId, fødselsnummer, GodkjenningFraSaksbehandler)

        val løstBehov = behov
            .apply { put("saksbehandlerIdent", saksbehandler) }
            .løsBehov(mapOf(GodkjenningFraSaksbehandler.name to mapOf("godkjent" to utbetalingGodkjent)))
        sendBehov(løstBehov)
    }

    private fun sendInnteksmelding(
        aktorID: String,
        fødselsnummer: String,
        virksomhetsnummer: String,
        inntektsMelding: Inntektsmelding = inntektsmeldingDTO(
            aktørId = aktorID,
            fødselsnummer = fødselsnummer,
            virksomhetsnummer = virksomhetsnummer,
            refusjon = Refusjon(
                beloepPrMnd = 31000.toBigDecimal(),
                opphoersdato = LocalDate.now()
            )
        )
    ) {
        synchronousSendKafkaMessage(
            inntektsmeldingTopic,
            inntektsMelding.inntektsmeldingId,
            inntektsMelding.toJsonNode().toString()
        )
    }

    private fun sendSøknad(
        aktorID: String,
        fødselsnummer: String,
        virksomhetsnummer: String,
        sendtSøknad: SykepengesoknadDTO = søknadDTO(
            aktørId = aktorID,
            fødselsnummer = fødselsnummer,
            arbeidsgiver = ArbeidsgiverDTO(orgnummer = virksomhetsnummer),
            status = SoknadsstatusDTO.SENDT
        )
    ) {
        synchronousSendKafkaMessage(søknadTopic, sendtSøknad.id!!, sendtSøknad.toJsonNode().toString())
    }

    private fun sendNySøknad(
        aktorID: String,
        fødselsnummer: String,
        virksomhetsnummer: String,
        nySøknad: SykepengesoknadDTO = søknadDTO(
            aktørId = aktorID,
            fødselsnummer = fødselsnummer,
            arbeidsgiver = ArbeidsgiverDTO(orgnummer = virksomhetsnummer),
            status = SoknadsstatusDTO.NY
        ).copy(sendtNav = null)
    ): SykepengesoknadDTO {
        synchronousSendKafkaMessage(søknadTopic, nySøknad.id!!, nySøknad.toJsonNode().toString())
        return nySøknad
    }

    private fun sendBehov(behov: String) {
        sendKafkaMessage(rapidTopic, UUID.randomUUID().toString(), behov)
    }

    private fun ventPåBehov(aktørId: String, fødselsnummer: String, behovType: Behovstype): ObjectNode {
        var behov: ObjectNode? = null
        await()
            .atMost(timeoutSecondsPerStep, SECONDS)
            .until {
                behov = TestConsumer.records(rapidTopic)
                    .map { objectMapper.readValue<ObjectNode>(it.value()) }
                    .filter { it.hasNonNull("@behov") }
                    .filter { it["@behov"].map(JsonNode::asText).contains(behovType.name) }
                    .firstOrNull { aktørId == it["aktørId"].asText() && fødselsnummer == it["fødselsnummer"].asText() }

                behov != null
            }

        return behov!!
    }

    private fun assertVedtaksperiodeEndretEvent(
        fødselsnummer: String,
        virksomhetsnummer: String,
        aktørId: String,
        previousState: TilstandType,
        currentState: TilstandType
    ) {
        await()
            .atMost(timeoutSecondsPerStep, SECONDS)
            .untilAsserted {
                val meldingerPåTopic = TestConsumer.records(rapidTopic)
                val vedtaksperiodeEndretHendelser = meldingerPåTopic
                    .map { objectMapper.readTree(it.value()) }
                    .filter { it["@event_name"]?.asText() == "vedtaksperiode_endret" }
                    .filter { aktørId == it["aktørId"].textValue() }
                    .filter { fødselsnummer == it["fødselsnummer"].textValue() }
                    .filter { virksomhetsnummer == it["organisasjonsnummer"].textValue() }
                    .filter {
                        previousState == TilstandType.valueOf(it["forrigeTilstand"].textValue())
                            && currentState == TilstandType.valueOf(it["gjeldendeTilstand"].textValue())
                    }

                assertEquals(1, vedtaksperiodeEndretHendelser.size)
            }
    }

    private fun assertBehov(fødselsnummer: String, virksomhetsnummer: String, aktørId: String, typer: List<String>) {
        await()
            .atMost(timeoutSecondsPerStep, SECONDS)
            .untilAsserted {
                val meldingerPåTopic =
                    TestConsumer.records(rapidTopic)
                val behov = meldingerPåTopic
                    .map { objectMapper.readTree(it.value()) }
                    .filter { it.hasNonNull("@behov") }
                    .filter { aktørId == it["aktørId"].asText() }
                    .filter { fødselsnummer == it["fødselsnummer"].asText() }
                    .filter { virksomhetsnummer == it["organisasjonsnummer"].asText() }
                    .filter { it["@behov"].map(JsonNode::asText).containsAll(typer) }
                    .distinct()

                assertNotNull(behov, "Fant ingen behov med type $typer")
            }
    }

    private fun sendKafkaMessage(topic: String, key: String, message: String) =
        kafkaProducer.send(ProducerRecord(topic, key, message))

    /**
     * Trick Kafka into behaving synchronously by sending the message, and then confirming that it is read by the
     * consumer group
     */
    private fun synchronousSendKafkaMessage(topic: String, key: String, message: String) {
        val metadata = sendKafkaMessage(topic, key, message)
        kafkaProducer.flush()
        metadata.get().assertMessageIsConsumed()
    }

    private fun JsonNode.løsBehov(løsning: Any) = also {
        it as ObjectNode
        it.set<ObjectNode>("@løsning", objectMapper.convertValue(løsning))
        it.put("@final", true)
        it.put("@besvart", LocalDateTime.now().toString())
    }.toString()

    /**
     * Check that the consumers has received this message, by comparing the position of the message with the reported
     * last read message of the consumer group
     */
    private fun RecordMetadata.assertMessageIsConsumed(recordMetadata: RecordMetadata = this) {
        await()
            .atMost(timeoutSecondsPerStep, SECONDS)
            .untilAsserted {
                val offsetAndMetadataMap =
                    adminClient.listConsumerGroupOffsets(
                        kafkaApplicationId
                    ).partitionsToOffsetAndMetadata().get()
                val topicPartition = TopicPartition(recordMetadata.topic(), recordMetadata.partition())
                val currentPositionOfSentMessage = recordMetadata.offset()
                val currentConsumerGroupPosition = offsetAndMetadataMap[topicPartition]?.offset()?.minus(1)
                    ?: fail() // This offset represents next position to read from, so we subtract 1 to get the last read offset
                assertEquals(currentConsumerGroupPosition, currentPositionOfSentMessage)
            }
    }

    private object TestConsumer {
        private val records = mutableListOf<ConsumerRecord<String, String>>()

        private val kafkaConsumer =
            KafkaConsumer(consumerProperties(), StringDeserializer(), StringDeserializer()).also {
                it.subscribe(topics)
            }

        fun reset() {
            records.clear()
        }

        fun records(topic: String) = records().filter { it.topic() == topic }

        fun records() =
            records.also {
                it.addAll(
                    kafkaConsumer.poll(ofMillis(0))
                )
            }

        fun close() {
            kafkaConsumer.unsubscribe()
            kafkaConsumer.close()
        }
    }
}
