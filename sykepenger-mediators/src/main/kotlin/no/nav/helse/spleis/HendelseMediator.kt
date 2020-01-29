package no.nav.helse.spleis

import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.Topics
import no.nav.helse.behov.Behov
import no.nav.helse.person.*
import no.nav.helse.spleis.db.HendelseRecorder
import no.nav.helse.spleis.db.PersonRepository
import no.nav.helse.spleis.hendelser.JsonMessage
import no.nav.helse.spleis.hendelser.MessageProcessor
import no.nav.helse.spleis.hendelser.Parser
import no.nav.helse.spleis.hendelser.model.*
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

// Understands how to communicate messages to other objects
// Acts like a GoF Mediator to forward messages to observers
// Uses GoF Observer pattern to notify events
internal class HendelseMediator(
    rapid: KafkaRapid,
    private val personRepository: PersonRepository,
    private val lagrePersonDao: PersonObserver,
    private val lagreUtbetalingDao: PersonObserver,
    private val vedtaksperiodeProbe: VedtaksperiodeProbe = VedtaksperiodeProbe,
    producer: KafkaProducer<String, String>,
    private val hendelseProbe: HendelseProbe,
    private val hendelseRecorder: HendelseRecorder
) : Parser.ParserDirector {
    private val sikkerLogg = LoggerFactory.getLogger("sikkerLogg")
    private val messageProcessor = Processor()
    private val parser = Parser(this)

    private val personObserver = PersonMediator(producer)

    init {
        rapid.addListener(parser)

        parser.register(NySøknadMessage.Factory)
        parser.register(SendtSøknadMessage.Factory)
        parser.register(InntektsmeldingMessage.Factory)
        parser.register(YtelserMessage.Factory)
        parser.register(VilkårsgrunnlagMessage.Factory)
        parser.register(ManuellSaksbehandlingMessage.Factory)
        parser.register(PåminnelseMessage.Factory)
    }

    override fun onRecognizedMessage(message: JsonMessage, aktivitetslogger: Aktivitetslogger) {
        try {
            hendelseRecorder.lagreMelding(message)
            message.accept(messageProcessor)

            if (aktivitetslogger.hasMessages()) {
                sikkerLogg.info("meldinger om melding: ${aktivitetslogger.toReport()}")
            }
        } catch (err: Aktivitetslogger.AktivitetException) {
            sikkerLogg.info("feil på melding: $err")
        } catch (err: UtenforOmfangException) {
            sikkerLogg.info("melding er utenfor omfang: ${err.message}", err)
        } catch (err: PersonskjemaForGammelt) {
            sikkerLogg.info("person har gammelt skjema: ${err.message}", err)
        }
    }

    override fun onMessageError(aktivitetException: Aktivitetslogger.AktivitetException) {
        sikkerLogg.info("feil på melding: ${aktivitetException.message}", aktivitetException)
    }

    override fun onUnrecognizedMessage(aktivitetslogger: Aktivitetslogger) {
        sikkerLogg.info("ukjent melding: ${aktivitetslogger.toReport()}")
    }

    private inner class Processor : MessageProcessor {
        override fun process(message: NySøknadMessage, aktivitetslogger: Aktivitetslogger) {
            val modelNySøknad = message.asModelNySøknad()

            hendelseProbe.onNySøknad(modelNySøknad)
            person(modelNySøknad).håndter(modelNySøknad)
        }

        override fun process(message: SendtSøknadMessage, aktivitetslogger: Aktivitetslogger) {
            val modelSendtSøknad = message.asModelSendtSøknad()
            hendelseProbe.onSendtSøknad(modelSendtSøknad)
            person(modelSendtSøknad).håndter(modelSendtSøknad)
        }

        override fun process(message: InntektsmeldingMessage, aktivitetslogger: Aktivitetslogger) {
            val inntektsmelding = message.asModelInntektsmelding()
            hendelseProbe.onInntektsmelding(inntektsmelding)
            person(inntektsmelding).håndter(inntektsmelding)
        }

        override fun process(message: YtelserMessage, aktivitetslogger: Aktivitetslogger) {
            val ytelser = message.asModelYtelser()

            hendelseProbe.onYtelser(ytelser)
            person(ytelser).håndter(ytelser)
        }

        override fun process(message: VilkårsgrunnlagMessage, aktivitetslogger: Aktivitetslogger) {
            val vilkårsgrunnlag = message.asModelVilkårsgrunnlag()
            hendelseProbe.onVilkårsgrunnlag(vilkårsgrunnlag)
            person(vilkårsgrunnlag).håndter(vilkårsgrunnlag)
        }

        override fun process(message: ManuellSaksbehandlingMessage, aktivitetslogger: Aktivitetslogger) {
            val manuellSaksbehandling = message.asModelManuellSaksbehandling()
            hendelseProbe.onManuellSaksbehandling(manuellSaksbehandling)
            person(manuellSaksbehandling).håndter(manuellSaksbehandling)
        }

        override fun process(message: PåminnelseMessage, aktivitetslogger: Aktivitetslogger) {
            val påminnelse = message.asModelPåminnelse()
                hendelseProbe.onPåminnelse(påminnelse)
                person(påminnelse).håndter(påminnelse)
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
            ProducerRecord<String, String>(Topics.rapidTopic, fødselsnummer(), toJson())

    }
}
