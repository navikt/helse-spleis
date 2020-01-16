package no.nav.helse.component

import no.nav.common.KafkaEnvironment
import no.nav.helse.TestConstants
import no.nav.helse.Topics
import no.nav.helse.behov.Behov
import no.nav.helse.behov.Behovstype
import no.nav.helse.hendelser.*
import no.nav.helse.løsBehov
import no.nav.helse.person.ArbeidstakerHendelse.Hendelsestype
import no.nav.helse.person.Problemer
import no.nav.helse.person.TilstandType
import no.nav.helse.spleis.HendelseListener
import no.nav.helse.spleis.HendelseStream
import no.nav.helse.spleis.hendelser.HendelseMediator
import no.nav.helse.toJsonNode
import no.nav.inntektsmeldingkontrakt.Arbeidsgivertype
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
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

internal class HendelseMediatorTest : HendelseListener {

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
        private const val dummyTopic = "unused"
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

        private val hendelseStream = HendelseStream(listOf(dummyTopic))

        init {
            HendelseMediator(hendelseStream).also {
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

                override fun onNySøknad(søknad: ModelNySøknad, problemer: Problemer) {
                    lestNySøknad.set(true)
                }

                    override fun onSendtSøknad(søknad: SendtSøknad) {
                        lestSendtSøknad.set(true)
                    }
                })
            }
        }
        private val topicInfos = listOf(dummyTopic, Topics.helseRapidTopic).map { KafkaEnvironment.TopicInfo(it, partitions = 1) }

        private val embeddedKafkaEnvironment = KafkaEnvironment(
            autoStart = false,
            noOfBrokers = 1,
            topicInfos = topicInfos,
            withSchemaRegistry = false,
            withSecurity = false
        )

        private lateinit var producer: KafkaProducer<String, String>

        private fun generiskBehov(
            hendelsetype: Hendelsestype,
            aktørId: String,
            fødselsnummer: String,
            organisasjonsnummer: String,
            behov: List<Behovstype> = listOf()
        ) = Behov.nyttBehov(
            hendelsestype = hendelsetype,
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

        private fun sendNyPåminnelse(
            aktørId: String = defaultAktørId,
            fødselsnummer: String = defaultFødselsnummer,
            organisasjonsnummer: String = defaultOrganisasjonsnummer
        ): Påminnelse {
            return TestConstants.påminnelseHendelse(
                vedtaksperiodeId = UUID.randomUUID(),
                tilstand = TilstandType.START,
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer,
                fødselsnummer = fødselsnummer
            ).also {
                sendKafkaMessage(aktørId, it.toJson())
            }
        }

        private fun sendManuellSaksbehandling(
            aktørId: String = defaultAktørId,
            fødselsnummer: String = defaultFødselsnummer,
            organisasjonsnummer: String = defaultOrganisasjonsnummer
        ) {
            val behov = Behov.nyttBehov(
                hendelsestype = Hendelsestype.ManuellSaksbehandling,
                behov = listOf(Behovstype.GodkjenningFraSaksbehandler),
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = UUID.randomUUID(),
                additionalParams = emptyMap()
            )
            sendBehov(
                behov.løsBehov(
                    mapOf(
                        "GodkjenningFraSaksbehandler" to true
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
                hendelsetype = Hendelsestype.Ytelser,
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                behov = listOf(Behovstype.Sykepengehistorikk, Behovstype.Foreldrepenger)
            )
            sendBehov(
                behov.løsBehov(
                    mapOf(
                        "Sykepengehistorikk" to emptyList<Any>()
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
                hendelsetype = Hendelsestype.Vilkårsgrunnlag,
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                behov = listOf(Behovstype.Inntektsberegning, Behovstype.EgenAnsatt)
            )

            sendBehov(
                behov.løsBehov(
                    mapOf(
                        "EgenAnsatt" to egenAnsatt
                    )
                )
            )
        }

        private fun sendInnteksmelding(
            aktørId: String = defaultAktørId,
            fødselsnummer: String = defaultFødselsnummer,
            organisasjonsnummer: String = defaultOrganisasjonsnummer
        ): Inntektsmelding {
            val inntektsmelding = Inntektsmelding(
                inntektsmeldingId = UUID.randomUUID().toString(),
                arbeidstakerFnr = fødselsnummer,
                arbeidstakerAktorId = aktørId,
                virksomhetsnummer = organisasjonsnummer,
                arbeidsgiverFnr = null,
                arbeidsgiverAktorId = null,
                arbeidsgivertype = Arbeidsgivertype.VIRKSOMHET,
                arbeidsforholdId = null,
                beregnetInntekt = BigDecimal.ONE,
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
                soknadsperioder = emptyList(),
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

        private fun sendBehov(behov: Behov) {
            sendKafkaMessage(behov.id().toString(), behov.toJson())
        }

        private fun sendKafkaMessage(key: String, message: String) =
            producer.send(ProducerRecord(Topics.helseRapidTopic, key, message))
    }
}
