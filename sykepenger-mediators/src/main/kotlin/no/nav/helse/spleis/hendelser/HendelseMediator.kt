package no.nav.helse.spleis.hendelser

import com.fasterxml.jackson.databind.JsonNode
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.Topics
import no.nav.helse.behov.Behov
import no.nav.helse.hendelser.*
import no.nav.helse.person.*
import no.nav.helse.spleis.*
import no.nav.helse.spleis.hendelser.model.*
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

// Understands how to communicate messages to other objects
// Acts like a GoF Mediator to forward messages to observers
// Uses GoF Observer pattern to notify events
internal class HendelseMediator(
    rapid: HendelseStream,
    private val personRepository: PersonRepository,
    private val lagrePersonDao: PersonObserver,
    private val lagreUtbetalingDao: PersonObserver,
    private val vedtaksperiodeProbe: VedtaksperiodeProbe = VedtaksperiodeProbe,
    producer: KafkaProducer<String, String>,
    private val hendelseProbe: HendelseListener,
    private val hendelseRecorder: HendelseListener
) : Parser.ParserDirector {
    private val sikkerLogg = LoggerFactory.getLogger("sikkerLogg")
    private val messageProcessor = Processor()
    private val parser = Parser(this)

    private val personObserver = PersonMediator(producer)

    init {
        rapid.addListener(parser)

        parser.register(NySøknadMessage.Factory)
        parser.register(FremtidigSøknadMessage.Factory)
        parser.register(SendtSøknadMessage.Factory)
        parser.register(InntektsmeldingMessage.Factory)
        parser.register(YtelserMessage.Factory)
        parser.register(VilkårsgrunnlagMessage.Factory)
        parser.register(ManuellSaksbehandlingMessage.Factory)
        parser.register(PåminnelseMessage.Factory)
    }

    override fun onRecognizedMessage(message: JsonMessage, warnings: Aktivitetslogger) {
        try {
            message.accept(messageProcessor)

            if (warnings.hasMessages()) {
                sikkerLogg.info("meldinger om melding: $warnings")
            }
        } catch (err: Aktivitetslogger) {
            sikkerLogg.info("feil på melding: $err")
        } catch (err: UtenforOmfangException) {
            sikkerLogg.info("melding er utenfor omfang: ${err.message}", err)
        } catch (err: PersonskjemaForGammelt) {
            sikkerLogg.info("person har gammelt skjema: ${err.message}", err)
        }
    }

    override fun onUnrecognizedMessage(aktivitetslogger: Aktivitetslogger) {
        sikkerLogg.info("feil på melding: $aktivitetslogger")
    }

    private inner class Processor : MessageProcessor {
        override fun process(message: NySøknadMessage, aktivitetslogger: Aktivitetslogger) {
            val modelNySøknad = ModelNySøknad(
                hendelseId = UUID.randomUUID(),
                fnr = message["fnr"].asText(),
                aktørId = message["aktorId"].asText(),
                orgnummer = message["arbeidsgiver"].path("orgnummer").asText(),
                rapportertdato = message["opprettet"].asText().let { LocalDateTime.parse(it) },
                sykeperioder = message["soknadsperioder"].map {
                    Triple(
                        first = it.path("fom").asLocalDate(),
                        second = it.path("tom").asLocalDate(),
                        third = it.path("sykmeldingsgrad").asInt()
                    )
                },
                aktivitetslogger = aktivitetslogger,
                originalJson = message.toJson()
            )

            hendelseProbe.onNySøknad(modelNySøknad, aktivitetslogger)
            hendelseRecorder.onNySøknad(modelNySøknad, aktivitetslogger)
            person(modelNySøknad).håndter(modelNySøknad, aktivitetslogger)

            if (aktivitetslogger.hasMessages()) {
                sikkerLogg.info("meldinger om ny søknad: $aktivitetslogger")
            }
        }

        private fun JsonNode.asLocalDate() =
            asText().let { LocalDate.parse(it) }


        override fun process(message: FremtidigSøknadMessage, aktivitetslogger: Aktivitetslogger) {
            // TODO: map til ordentlig domenehendelse uten kobling til json
            NySøknad.Builder().build(message.toJson())?.apply {
                hendelseProbe.onNySøknad(this)
                hendelseRecorder.onNySøknad(this)
                person(this).håndter(this)
            } ?: aktivitetslogger.error("klarer ikke å mappe søknaden til domenetype")
        }

        override fun process(message: SendtSøknadMessage, aktivitetslogger: Aktivitetslogger) {
            // TODO: map til ordentlig domenehendelse uten kobling til json
            SendtSøknad.Builder().build(message.toJson())?.apply {
                hendelseProbe.onSendtSøknad(this)
                hendelseRecorder.onSendtSøknad(this)
                person(this).håndter(this)
            } ?: aktivitetslogger.error("klarer ikke å mappe søknaden til domenetype")
        }

        override fun process(message: InntektsmeldingMessage, aktivitetslogger: Aktivitetslogger) {
            // TODO: map til ordentlig domenehendelse uten kobling til json
            Inntektsmelding.Builder().build(message.toJson())?.apply {
                hendelseProbe.onInntektsmelding(this)
                hendelseRecorder.onInntektsmelding(this)
                person(this).håndter(this)
            } ?: aktivitetslogger.error("klarer ikke å mappe inntektsmelding til domenetype")
        }

        override fun process(message: YtelserMessage, aktivitetslogger: Aktivitetslogger) {
            val foreldrepenger = message["@løsning"].path("Foreldrepenger").let {
                ModelForeldrepenger(
                    foreldrepengeytelse = it.path("Foreldrepengeytelse").takeIf(JsonNode::isObject)?.let(::asPeriode),
                    svangerskapsytelse = it.path("Svangerskapsytelse").takeIf(JsonNode::isObject)?.let(::asPeriode),
                    aktivitetslogger = aktivitetslogger
                )
            }

            val sykepengehistorikk = ModelSykepengehistorikk(
                perioder = message["@løsning"].path("Sykepengehistorikk").map(::asPeriode),
                aktivitetslogger = aktivitetslogger
            )

            val ytelser = ModelYtelser(
                hendelseId = UUID.randomUUID(),
                aktørId = message["aktørId"].asText(),
                fødselsnummer = message["fødselsnummer"].asText(),
                organisasjonsnummer = message["organisasjonsnummer"].asText(),
                vedtaksperiodeId = message["vedtaksperiodeId"].asText(),
                sykepengehistorikk = sykepengehistorikk,
                foreldrepenger = foreldrepenger,
                rapportertdato = message["@besvart"].asLocalDateTime(),
                originalJson = message.toJson()
            )

            hendelseProbe.onYtelser(ytelser)
            hendelseRecorder.onYtelser(ytelser)
            person(ytelser).håndter(ytelser)

            if (aktivitetslogger.hasMessages()) {
                sikkerLogg.info("meldinger om ytelser: $aktivitetslogger")
            }
        }

        private fun person(arbeidstakerHendelse: ArbeidstakerHendelse) =
            (personRepository.hentPerson(arbeidstakerHendelse.aktørId()) ?: Person(
                aktørId = arbeidstakerHendelse.aktørId(),
                fødselsnummer = arbeidstakerHendelse.fødselsnummer()
            )).also {
                it.addObserver(personObserver)
                it.addObserver(lagrePersonDao)
                it.addObserver(lagreUtbetalingDao)
                it.addObserver(vedtaksperiodeProbe)
            }

        private fun JsonNode.asLocalDateTime() =
            asText().let { LocalDateTime.parse(it) }

        private fun asPeriode(jsonNode: JsonNode) =
            jsonNode.path("fom").asLocalDate() to jsonNode.path("tom").asLocalDate()

        override fun process(message: VilkårsgrunnlagMessage, aktivitetslogger: Aktivitetslogger) {
            // TODO: map til ordentlig domenehendelse uten kobling til json
            Vilkårsgrunnlag.Builder().build(message.toJson())?.apply {
                hendelseProbe.onVilkårsgrunnlag(this)
                hendelseRecorder.onVilkårsgrunnlag(this)
                person(this).håndter(this)
            } ?: aktivitetslogger.error("klarer ikke å mappe vilkårsgrunnlag til domenetype")
        }

        override fun process(message: ManuellSaksbehandlingMessage, aktivitetslogger: Aktivitetslogger) {
            // TODO: map til ordentlig domenehendelse uten kobling til json
            ManuellSaksbehandling.Builder().build(message.toJson())?.apply {
                hendelseProbe.onManuellSaksbehandling(this)
                hendelseRecorder.onManuellSaksbehandling(this)
                person(this).håndter(this)
            } ?: aktivitetslogger.error("klarer ikke å mappe manuellsaksbehandling til domenetype")
        }

        override fun process(message: PåminnelseMessage, aktivitetslogger: Aktivitetslogger) {
            // TODO: map til ordentlig domenehendelse uten kobling til json
            Påminnelse.Builder().build(message.toJson())?.apply {
                hendelseProbe.onPåminnelse(this)
                hendelseRecorder.onPåminnelse(this)
                person(this).håndter(this)
            } ?: aktivitetslogger.error("klarer ikke å mappe påminnelse til domenetype")
        }
    }

    private class PersonMediator(private val producer: KafkaProducer<String, String>) : PersonObserver {
        private val log = LoggerFactory.getLogger(this::class.java)

        override fun personEndret(personEndretEvent: PersonObserver.PersonEndretEvent) {}

        override fun vedtaksperiodeTrengerLøsning(event: Behov) {
            producer.send(event.producerRecord()).get().also {
                log.info(
                    "produserte behov=$event, {}, {}, {}",
                    StructuredArguments.keyValue("vedtaksperiodeId", event.vedtaksperiodeId()),
                    StructuredArguments.keyValue("partisjon", it.partition()),
                    StructuredArguments.keyValue("offset", it.offset())
                )
            }
        }

        override fun vedtaksperiodeEndret(event: VedtaksperiodeObserver.StateChangeEvent) {
            producer.send(event.producerRecord()).get()
        }

        override fun vedtaksperiodeTilUtbetaling(event: VedtaksperiodeObserver.UtbetalingEvent) {
            producer.send(event.producerRecord()).get().also {
                log.info(
                    "legger vedtatt vedtak: {} på topic med {} og {}",
                    StructuredArguments.keyValue("vedtaksperiodeId", event.vedtaksperiodeId),
                    StructuredArguments.keyValue("partisjon", it.partition()),
                    StructuredArguments.keyValue("offset", it.offset())
                )
            }
        }

        override fun vedtaksperiodeIkkeFunnet(vedtaksperiodeEvent: PersonObserver.VedtaksperiodeIkkeFunnetEvent) {
            producer.send(vedtaksperiodeEvent.producerRecord())
        }

        private fun Behov.producerRecord() =
            ProducerRecord<String, String>(Topics.behovTopic, id().toString(), toJson())

    }
}
