package no.nav.helse.component

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import no.nav.common.KafkaEnvironment
import no.nav.helse.Topics
import no.nav.helse.behov.Behov
import no.nav.helse.behov.Behovstype
import no.nav.helse.hendelser.*
import no.nav.helse.løsBehov
import no.nav.helse.person.Person
import no.nav.helse.person.TilstandType
import no.nav.helse.spleis.HendelseMediator
import no.nav.helse.spleis.KafkaRapid
import no.nav.helse.spleis.db.PersonRepository
import no.nav.helse.toJsonNode
import no.nav.inntektsmeldingkontrakt.Arbeidsgivertype
import no.nav.inntektsmeldingkontrakt.Refusjon
import no.nav.inntektsmeldingkontrakt.Status
import no.nav.syfo.kafka.sykepengesoknad.dto.*
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig.LINGER_MS_CONFIG
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.StreamsConfig
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicBoolean
import no.nav.inntektsmeldingkontrakt.Inntektsmelding as Inntektsmeldingkontrakt

internal class HendelseMediatorTest {

    @Test
    internal fun `leser søknader`() {
        sendNySøknad()
        ventTilTrue(lestNySøknad)

        sendSøknad()
        ventTilTrue(lestSendtSøknad)
    }

    @Test
    internal fun `leser inntektsmeldinger`() {
        sendInnteksmelding()
        ventTilTrue(lestInntektsmelding)
    }

    @Test
    internal fun `leser påminnelser`() {
        sendNyPåminnelse()
        ventTilTrue(lestPåminnelse)
    }

    @Test
    internal fun `leser behov`() {
        sendVilkårsgrunnlag()
        ventTilTrue(lestVilkårsgrunnlag)

        sendYtelser()
        ventTilTrue(lestYtelser)

        sendManuellSaksbehandling()
        ventTilTrue(lestManuellSaksbehandling)
    }

    private fun ventTilTrue(atomicBoolean: AtomicBoolean) {
        await().atMost(10, SECONDS).untilTrue(atomicBoolean)
    }

    @BeforeEach
    internal fun reset() {
        lestNySøknad.set(false)
        lestSendtSøknad.set(false)
        lestInntektsmelding.set(false)
        lestPåminnelse.set(false)
        lestYtelser.set(false)
        lestVilkårsgrunnlag.set(false)
        lestManuellSaksbehandling.set(false)
    }

    private companion object : PersonRepository {
        private val defaultAktørId = UUID.randomUUID().toString()
        private val defaultFødselsnummer = UUID.randomUUID().toString()
        private val defaultOrganisasjonsnummer = UUID.randomUUID().toString()

        private val lestNySøknad = AtomicBoolean(false)
        private val lestSendtSøknad = AtomicBoolean(false)
        private val lestInntektsmelding = AtomicBoolean(false)
        private val lestPåminnelse = AtomicBoolean(false)
        private val lestYtelser = AtomicBoolean(false)
        private val lestVilkårsgrunnlag = AtomicBoolean(false)
        private val lestManuellSaksbehandling = AtomicBoolean(false)

        private val hendelseStream = KafkaRapid()

        override fun hentPerson(aktørId: String): Person? {
            return mockk<Person>(relaxed = true) {

                every {
                    håndter(any<Sykmelding>())
                } answers {
                    lestNySøknad.set(true)
                }

                every {
                    håndter(any<Søknad>())
                } answers {
                    lestSendtSøknad.set(true)
                }

                every {
                    håndter(any<Inntektsmelding>())
                } answers {
                    lestInntektsmelding.set(true)
                }

                every {
                    håndter(any<Ytelser>())
                } answers {
                    lestYtelser.set(true)
                }

                every {
                    håndter(any<Påminnelse>())
                } answers {
                    lestPåminnelse.set(true)
                }

                every {
                    håndter(any<Vilkårsgrunnlag>())
                } answers {
                    lestVilkårsgrunnlag.set(true)
                }

                every {
                    håndter(any<ManuellSaksbehandling>())
                } answers {
                    lestManuellSaksbehandling.set(true)
                }
            }
        }

        init {
            HendelseMediator(
                rapid = hendelseStream,
                personRepository = this,
                lagrePersonDao = mockk(relaxed = true),
                lagreUtbetalingDao = mockk(relaxed = true),
                vedtaksperiodeProbe = mockk(relaxed = true),
                producer = mockk(relaxed = true),
                hendelseProbe = mockk(relaxed = true),
                hendelseRecorder = mockk(relaxed = true)
            )
        }
        private val topicInfos = listOf(Topics.søknadTopic, Topics.rapidTopic).map { KafkaEnvironment.TopicInfo(it, partitions = 1) }

        private val embeddedKafkaEnvironment = KafkaEnvironment(
            autoStart = false,
            noOfBrokers = 1,
            topicInfos = topicInfos,
            withSchemaRegistry = false,
            withSecurity = false
        )

        private lateinit var producer: KafkaProducer<String, String>

        private fun generiskBehov(
            aktørId: String,
            fødselsnummer: String,
            organisasjonsnummer: String,
            behov: List<Behovstype> = listOf()
        ) = Behov.nyttBehov(
            behov = behov,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = UUID.randomUUID(),
            additionalParams = mapOf()
        )

        @BeforeAll
        @JvmStatic
        internal fun `start embedded environment`() {
            embeddedKafkaEnvironment.start()
            hendelseStream.start(streamsConfig())

            producer = KafkaProducer(producerConfig(), StringSerializer(), StringSerializer())
        }

        private fun streamsConfig() = kafkaBaseConfig().apply {
            put(StreamsConfig.APPLICATION_ID_CONFIG, "hendelsebuilder-test")
        }

        private fun producerConfig() = kafkaBaseConfig().apply {
            put(LINGER_MS_CONFIG, "0")
        }

        private fun kafkaBaseConfig() = Properties().apply {
            put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaEnvironment.brokersURL)
        }

        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        private fun sendNyPåminnelse(
            aktørId: String = defaultAktørId,
            fødselsnummer: String = defaultFødselsnummer,
            organisasjonsnummer: String = defaultOrganisasjonsnummer
        ) {
            objectMapper.writeValueAsString(
                mapOf(
                    "@event_name" to "påminnelse",
                    "aktørId" to aktørId,
                    "fødselsnummer" to fødselsnummer,
                    "organisasjonsnummer" to organisasjonsnummer,
                    "vedtaksperiodeId" to UUID.randomUUID().toString(),
                    "tilstand" to TilstandType.START.name,
                    "antallGangerPåminnet" to 0,
                    "tilstandsendringstidspunkt" to LocalDateTime.now().toString(),
                    "påminnelsestidspunkt" to LocalDateTime.now().toString(),
                    "nestePåminnelsestidspunkt" to LocalDateTime.now().toString()
                )
            ).also { sendKafkaMessage(aktørId, it) }
        }

        private fun sendManuellSaksbehandling(
            aktørId: String = defaultAktørId,
            fødselsnummer: String = defaultFødselsnummer,
            organisasjonsnummer: String = defaultOrganisasjonsnummer
        ) {
            val behov = Behov.nyttBehov(
                behov = listOf(Behovstype.Godkjenning),
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = UUID.randomUUID(),
                additionalParams = mapOf(
                    "saksbehandlerIdent" to "en_saksbehandler"
                )
            )
            sendBehov(
                behov.løsBehov(
                    mapOf(
                        "Godkjenning" to mapOf(
                            "godkjent" to true
                        )
                    )
                )
            )
        }

        private fun sendYtelser(
            aktørId: String = defaultAktørId,
            fødselsnummer: String = defaultFødselsnummer,
            organisasjonsnummer: String = defaultOrganisasjonsnummer
        ) {
            val behov = generiskBehov(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                behov = listOf(Behovstype.Sykepengehistorikk, Behovstype.Foreldrepenger)
            )
            sendBehov(
                behov.løsBehov(
                    mapOf(
                        "Sykepengehistorikk" to emptyList<Any>(),
                        "Foreldrepenger" to emptyMap<String, String>()
                    )
                )
            )
        }

        private fun sendVilkårsgrunnlag(
            aktørId: String = defaultAktørId,
            fødselsnummer: String = defaultFødselsnummer,
            organisasjonsnummer: String = defaultOrganisasjonsnummer,
            egenAnsatt: Boolean = false
        ) {
            val behov = generiskBehov(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                behov = listOf(Behovstype.Inntektsberegning, Behovstype.EgenAnsatt, Behovstype.Opptjening)
            )

            sendBehov(
                behov.løsBehov(
                    mapOf(
                        "EgenAnsatt" to egenAnsatt,
                        "Inntektsberegning" to emptyMap<String, String>(),
                        "Opptjening" to emptyList<Any>()
                    )
                )
            )
        }

        private fun sendInnteksmelding(
            aktørId: String = defaultAktørId,
            fødselsnummer: String = defaultFødselsnummer,
            organisasjonsnummer: String = defaultOrganisasjonsnummer
        ): Inntektsmeldingkontrakt {
            val inntektsmelding = Inntektsmeldingkontrakt(
                inntektsmeldingId = UUID.randomUUID().toString(),
                arbeidstakerFnr = fødselsnummer,
                arbeidstakerAktorId = aktørId,
                virksomhetsnummer = organisasjonsnummer,
                arbeidsgiverFnr = null,
                arbeidsgiverAktorId = null,
                arbeidsgivertype = Arbeidsgivertype.VIRKSOMHET,
                arbeidsforholdId = null,
                beregnetInntekt = BigDecimal.ONE,
                refusjon = Refusjon(BigDecimal.ONE, LocalDate.now()),
                endringIRefusjoner = emptyList(),
                opphoerAvNaturalytelser = emptyList(),
                gjenopptakelseNaturalytelser = emptyList(),
                arbeidsgiverperioder = emptyList(),
                status = Status.GYLDIG,
                arkivreferanse = "",
                ferieperioder = emptyList(),
                foersteFravaersdag = LocalDate.now(),
                mottattDato = LocalDateTime.now()
            )
            sendKafkaMessage(
                inntektsmelding.inntektsmeldingId,
                inntektsmelding.toJsonNode().toString()
            )
            return inntektsmelding
        }

        private fun sendSøknad(
            id: UUID = UUID.randomUUID(),
            aktørId: String = defaultAktørId,
            fødselsnummer: String = defaultFødselsnummer,
            organisasjonsnummer: String = defaultOrganisasjonsnummer
        ) {
            val sendtSøknad = SykepengesoknadDTO(
                status = SoknadsstatusDTO.SENDT,
                id = id.toString(),
                aktorId = aktørId,
                fnr = fødselsnummer,
                arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer),
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                type = SoknadstypeDTO.ARBEIDSTAKERE,
                startSyketilfelle = LocalDate.now(),
                sendtNav = LocalDateTime.now(),
                egenmeldinger = emptyList(),
                fravar = emptyList(),
                soknadsperioder = listOf(SoknadsperiodeDTO(LocalDate.now(), LocalDate.now(), 100)),
                opprettet = LocalDateTime.now()
            )
            sendKafkaMessage(id.toString(), sendtSøknad.toJsonNode().toString())
        }

        private fun sendNySøknad(
            id: UUID = UUID.randomUUID(),
            aktørId: String = defaultAktørId,
            fødselsnummer: String = defaultFødselsnummer,
            organisasjonsnummer: String = defaultOrganisasjonsnummer
        ): SykepengesoknadDTO {
            val nySøknad = SykepengesoknadDTO(
                status = SoknadsstatusDTO.NY,
                id = id.toString(),
                sykmeldingId = UUID.randomUUID().toString(),
                aktorId = aktørId,
                fnr = fødselsnummer,
                arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer),
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                type = SoknadstypeDTO.ARBEIDSTAKERE,
                startSyketilfelle = LocalDate.now(),
                sendtNav = LocalDateTime.now(),
                egenmeldinger = emptyList(),
                fravar = emptyList(),
                soknadsperioder = listOf(
                    SoknadsperiodeDTO(
                        fom = LocalDate.now(),
                        tom = LocalDate.now(),
                        sykmeldingsgrad = 100
                    )
                ),
                opprettet = LocalDateTime.now()
            )
            sendKafkaMessage(id.toString(), nySøknad.toJsonNode().toString())
            return nySøknad
        }

        private fun sendBehov(behov: String) {
            sendKafkaMessage(UUID.randomUUID().toString(), behov)
        }

        private fun sendKafkaMessage(key: String, message: String) =
            producer.send(ProducerRecord(Topics.rapidTopic, key, message))
    }
}
