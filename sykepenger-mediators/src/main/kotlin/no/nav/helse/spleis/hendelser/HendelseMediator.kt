package no.nav.helse.spleis.hendelser

import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.Topics
import no.nav.helse.behov.Behov
import no.nav.helse.person.*
import no.nav.helse.spleis.*
import no.nav.helse.spleis.hendelser.model.*
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

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
        } catch (err: Aktivitetslogger.AktivitetException) {
            sikkerLogg.info("feil på melding: $err")
        } catch (err: UtenforOmfangException) {
            sikkerLogg.info("melding er utenfor omfang: ${err.message}", err)
        } catch (err: PersonskjemaForGammelt) {
            sikkerLogg.info("person har gammelt skjema: ${err.message}", err)
        }
    }

    override fun onUnrecognizedMessage(aktivitetException: Aktivitetslogger.AktivitetException) {
        sikkerLogg.info("feil på melding: $aktivitetException", aktivitetException)
    }

    override fun onUnrecognizedMessage(aktivitetslogger: Aktivitetslogger) {
        sikkerLogg.info("ukjent melding: $aktivitetslogger")
    }

    private inner class Processor : MessageProcessor {
        override fun process(message: NySøknadMessage, aktivitetslogger: Aktivitetslogger) {
            val modelNySøknad = message.asModelNySøknad()

            hendelseProbe.onNySøknad(modelNySøknad, aktivitetslogger)
            hendelseRecorder.onNySøknad(modelNySøknad, aktivitetslogger)
            person(modelNySøknad).håndter(modelNySøknad)

            if (aktivitetslogger.hasMessages()) {
                sikkerLogg.info("meldinger om ny søknad: $aktivitetslogger")
            }
        }

        override fun process(message: SendtSøknadMessage, aktivitetslogger: Aktivitetslogger) {
            val modelSendtSøknad = message.asModelSendtSøknad()
            hendelseProbe.onSendtSøknad(modelSendtSøknad)
            hendelseRecorder.onSendtSøknad(modelSendtSøknad)
            person(modelSendtSøknad).håndter(modelSendtSøknad)

            if (aktivitetslogger.hasMessages()) {
                sikkerLogg.info("meldinger om sendt søknad: $aktivitetslogger")
            }
        }

        override fun process(message: InntektsmeldingMessage, aktivitetslogger: Aktivitetslogger) {
            val inntektsmelding = message.asModelInntektsmelding()
            hendelseProbe.onInntektsmelding(inntektsmelding)
            hendelseRecorder.onInntektsmelding(inntektsmelding)
            person(inntektsmelding).håndter(inntektsmelding)
        }

        override fun process(message: YtelserMessage, aktivitetslogger: Aktivitetslogger) {
            val ytelser = message.asModelYtelser()

            hendelseProbe.onYtelser(ytelser)
            hendelseRecorder.onYtelser(ytelser)
            person(ytelser).håndter(ytelser)

            if (aktivitetslogger.hasMessages()) {
                sikkerLogg.info("meldinger om ytelser: $aktivitetslogger")
            }
        }

        override fun process(message: VilkårsgrunnlagMessage, aktivitetslogger: Aktivitetslogger) {
            val vilkårsgrunnlag = message.asModelVilkårsgrunnlag()
            hendelseProbe.onVilkårsgrunnlag(vilkårsgrunnlag)
            hendelseRecorder.onVilkårsgrunnlag(vilkårsgrunnlag)
            person(vilkårsgrunnlag).håndter(vilkårsgrunnlag)

            if (aktivitetslogger.hasMessages()) {
                sikkerLogg.info("meldinger om vilkårsgrunnlag: $aktivitetslogger")
            }
        }

        override fun process(message: ManuellSaksbehandlingMessage, aktivitetslogger: Aktivitetslogger) {
            val manuellSaksbehandling = message.asModelManuellSaksbehandling()
            hendelseProbe.onManuellSaksbehandling(manuellSaksbehandling)
            hendelseRecorder.onManuellSaksbehandling(manuellSaksbehandling)
            person(manuellSaksbehandling).håndter(manuellSaksbehandling)

            if (aktivitetslogger.hasMessages()) {
                sikkerLogg.info("meldinger om manuell saksbehandling: $aktivitetslogger")
            }
        }

        override fun process(message: PåminnelseMessage, aktivitetslogger: Aktivitetslogger) {
            val påminnelse = message.asModelPåminnelse()
                hendelseProbe.onPåminnelse(påminnelse)
                hendelseRecorder.onPåminnelse(påminnelse)
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
