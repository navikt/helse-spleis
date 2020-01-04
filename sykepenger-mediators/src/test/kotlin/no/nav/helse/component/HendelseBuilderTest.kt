package no.nav.helse.component

import no.nav.common.KafkaEnvironment
import no.nav.helse.TestConstants
import no.nav.helse.Topics
import no.nav.helse.behov.Behov
import no.nav.helse.behov.Behovtype
import no.nav.helse.hendelser.*
import no.nav.helse.løsBehov
import no.nav.helse.sak.ArbeidstakerHendelse
import no.nav.helse.sak.TilstandType
import no.nav.helse.spleis.HendelseBuilder
import no.nav.helse.spleis.HendelseListener
import no.nav.helse.toJsonNode
import no.nav.inntektsmeldingkontrakt.Arbeidsgivertype
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import no.nav.inntektsmeldingkontrakt.Refusjon
import no.nav.inntektsmeldingkontrakt.Status
import no.nav.syfo.kafka.sykepengesoknad.dto.ArbeidsgiverDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsstatusDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.SykepengesoknadDTO
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicBoolean

internal class HendelseBuilderTest : HendelseListener {

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

    private companion object : HendelseListener {
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

        private val hendelseBuilder = HendelseBuilder().also {
            it.addListener(object : HendelseListener {
                override fun onPåminnelse(påminnelse: Påminnelse) {
                    lestPåminnelse.set(true)
                }

                override fun onYtelser(ytelser: Ytelser) {
                    lestYtelser.set(true)
                }

                override fun onVilkårsgrunnlag(vilkårsgrunnlag: Vilkårsgrunnlag) {
                    lestVilkårsgrunnlag.set(true)
                }

                override fun onManuellSaksbehandling(manuellSaksbehandling: ManuellSaksbehandling) {
                    lestManuellSaksbehandling.set(true)
                }

                override fun onInntektsmelding(inntektsmelding: no.nav.helse.hendelser.Inntektsmelding) {
                    lestInntektsmelding.set(true)
                }

                override fun onNySøknad(søknad: NySøknad) {
                    lestNySøknad.set(true)
                }

                override fun onSendtSøknad(søknad: SendtSøknad) {
                    lestSendtSøknad.set(true)
                }
            })
        }

        private val topics = listOf(
            Topics.søknadTopic,
            Topics.inntektsmeldingTopic,
            Topics.behovTopic,
            Topics.påminnelseTopic
        )
        private val topicInfos = topics.map { KafkaEnvironment.TopicInfo(it, partitions = 1) }

        private val embeddedKafkaEnvironment = KafkaEnvironment(
            autoStart = false,
            noOfBrokers = 1,
            topicInfos = topicInfos,
            withSchemaRegistry = false,
            withSecurity = false,
            topicNames = topics
        )

        private lateinit var producer: KafkaProducer<String, String>

        @BeforeAll
        @JvmStatic
        internal fun `start embedded environment`() {
            embeddedKafkaEnvironment.start()
            hendelseBuilder.start(streamsConfig())

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

        private fun sendNyPåminnelse(aktørId: String = defaultAktørId, fødselsnummer: String = defaultFødselsnummer, organisasjonsnummer: String = defaultOrganisasjonsnummer): Påminnelse {
            return TestConstants.påminnelseHendelse(
                vedtaksperiodeId = UUID.randomUUID(),
                tilstand = TilstandType.START,
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer,
                fødselsnummer = fødselsnummer
            ).also {
                sendKafkaMessage(Topics.påminnelseTopic, aktørId, it.toJson())
            }
        }

        private fun sendManuellSaksbehandling(aktørId: String = defaultAktørId, fødselsnummer: String = defaultFødselsnummer, organisasjonsnummer: String = defaultOrganisasjonsnummer) {
            val behov = Behov.nyttBehov(
                hendelsetype = ArbeidstakerHendelse.Hendelsetype.ManuellSaksbehandling,
                behov = listOf(Behovtype.GodkjenningFraSaksbehandler),
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = UUID.randomUUID(),
                additionalParams = emptyMap()
            )
            sendBehov(behov.løsBehov(mapOf(
                "GodkjenningFraSaksbehandler" to true
            )))
        }

        private fun sendYtelser(aktørId: String = defaultAktørId, fødselsnummer: String = defaultFødselsnummer, organisasjonsnummer: String = defaultOrganisasjonsnummer) {
            val behov = Ytelser.lagBehov(UUID.randomUUID(), aktørId, fødselsnummer, organisasjonsnummer, LocalDate.now())
            sendBehov(behov.løsBehov(mapOf(
                "Sykepengehistorikk" to emptyList<Any>()
            )))
        }

        private fun sendVilkårsgrunnlag(aktørId: String = defaultAktørId, fødselsnummer: String = defaultFødselsnummer, organisasjonsnummer: String = defaultOrganisasjonsnummer, egenAnsatt: Boolean = false) {
            val behov = Vilkårsgrunnlag.lagBehov(UUID.randomUUID(), aktørId, fødselsnummer, organisasjonsnummer)

            sendBehov(behov.løsBehov(mapOf(
                "EgenAnsatt" to egenAnsatt
            )))
        }

        private fun sendInnteksmelding(aktørId: String = defaultAktørId, fødselsnummer: String = defaultFødselsnummer, organisasjonsnummer: String = defaultOrganisasjonsnummer): Inntektsmelding {
            val inntektsmelding = Inntektsmelding(
                inntektsmeldingId = UUID.randomUUID().toString(),
                arbeidstakerFnr = fødselsnummer,
                arbeidstakerAktorId = aktørId,
                virksomhetsnummer = organisasjonsnummer,
                arbeidsgiverFnr = null,
                arbeidsgiverAktorId = null,
                arbeidsgivertype = Arbeidsgivertype.VIRKSOMHET,
                arbeidsforholdId = null,
                beregnetInntekt = null,
                refusjon = Refusjon(null, null),
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
                Topics.inntektsmeldingTopic,
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
                arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer)
            )
            sendKafkaMessage(Topics.søknadTopic, id.toString(), sendtSøknad.toJsonNode().toString())
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
                aktorId = aktørId,
                fnr = fødselsnummer,
                arbeidsgiver = ArbeidsgiverDTO(orgnummer = organisasjonsnummer)
            )
            sendKafkaMessage(Topics.søknadTopic, id.toString(), nySøknad.toJsonNode().toString())
            return nySøknad
        }

        private fun sendBehov(behov: Behov) {
            sendKafkaMessage(Topics.behovTopic, behov.id().toString(), behov.toJson())
        }

        private fun sendKafkaMessage(topic: String, key: String, message: String) =
            producer.send(ProducerRecord(topic, key, message))
    }
}
