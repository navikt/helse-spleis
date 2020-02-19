package no.nav.helse.spleis

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.Topics
import no.nav.helse.behov.Behov
import no.nav.helse.behov.BehovType
import no.nav.helse.hendelser.HendelseObserver
import no.nav.helse.person.*
import no.nav.helse.spleis.db.HendelseRecorder
import no.nav.helse.spleis.db.PersonRepository
import no.nav.helse.spleis.hendelser.JsonMessage
import no.nav.helse.spleis.hendelser.MessageProcessor
import no.nav.helse.spleis.hendelser.Parser
import no.nav.helse.spleis.hendelser.model.*
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.Logger
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
    private val parser = Parser(this)

    private val personObserver = PersonMediator(producer, sikkerLogg)
    private val behovMediator = BehovMediator(producer)

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

    override fun onRecognizedMessage(
        message: JsonMessage,
        aktivitetslogger: Aktivitetslogger,
        aktivitetslogg: Aktivitetslogg
    ) {
        val messageProcessor = Processor(behovMediator)
        try {
            message.accept(hendelseRecorder)
            message.accept(messageProcessor)

            behovMediator.finalize()

            if (aktivitetslogger.hasErrorsOld()) {
                sikkerLogg.error("aktivitetslogger inneholder errors: ${aktivitetslogger.toReport()}")
            } else if (aktivitetslogger.hasMessagesOld()) {
                sikkerLogg.info("aktivitetslogger inneholder meldinger: ${aktivitetslogger.toReport()}")
            }
        } catch (err: Aktivitetslogger.AktivitetException) {
            sikkerLogg.error("feil på melding: $err")
        } catch (err: UtenforOmfangException) {
            sikkerLogg.error("melding er utenfor omfang: ${err.message}", err)
        } finally {
            AktivitetsloggerProbe.inspiser(aktivitetslogger)
        }
    }

    override fun onMessageError(aktivitetException: Aktivitetslogger.AktivitetException) {
        sikkerLogg.error("feil på melding: ${aktivitetException.message}", aktivitetException)
        AktivitetsloggerProbe.inspiser(aktivitetException)
    }

    override fun onMessageError(aktivitetException: Aktivitetslogg.AktivitetException) {
        sikkerLogg.error("feil på melding: ${aktivitetException.message}", aktivitetException)
        // TODO: pull message counts from Aktivitetslogg for statistics
//        AktivitetsloggerProbe.inspiser(aktivitetException)
    }

    override fun onUnrecognizedMessage(aktivitetslogger: Aktivitetslogger, aktivitetslogg: Aktivitetslogg) {
        sikkerLogg.debug("ukjent melding: ${aktivitetslogger.toReport()}")
    }

    private inner class Processor(private val behovMediator: BehovMediator) : MessageProcessor {
        override fun process(message: NySøknadMessage, aktivitetslogger: Aktivitetslogger) {
            val sykmelding = message.asSykmelding()

            hendelseProbe.onSykmelding(sykmelding)
            person(sykmelding).håndter(sykmelding)
        }

        override fun process(message: SendtSøknadMessage, aktivitetslogger: Aktivitetslogger) {
            val modelSendtSøknad = message.asSendtSøknad()
            hendelseProbe.onSendtSøknad(modelSendtSøknad)
            person(modelSendtSøknad).håndter(modelSendtSøknad)
        }

        override fun process(message: InntektsmeldingMessage, aktivitetslogger: Aktivitetslogger) {
            val inntektsmelding = message.asInntektsmelding()
            hendelseProbe.onInntektsmelding(inntektsmelding)
            person(inntektsmelding).håndter(inntektsmelding)
        }

        override fun process(message: YtelserMessage, aktivitetslogger: Aktivitetslogger) {
            val ytelser = message.asYtelser()

            hendelseProbe.onYtelser(ytelser)
            person(ytelser).håndter(ytelser)
        }

        override fun process(message: VilkårsgrunnlagMessage, aktivitetslogger: Aktivitetslogger) {
            val vilkårsgrunnlag = message.asVilkårsgrunnlag()
            hendelseProbe.onVilkårsgrunnlag(vilkårsgrunnlag)
            person(vilkårsgrunnlag).håndter(vilkårsgrunnlag)
        }

        override fun process(message: ManuellSaksbehandlingMessage, aktivitetslogger: Aktivitetslogger) {
            val manuellSaksbehandling = message.asManuellSaksbehandling()
            hendelseProbe.onManuellSaksbehandling(manuellSaksbehandling)
            person(manuellSaksbehandling).håndter(manuellSaksbehandling)
        }

        override fun process(message: PåminnelseMessage, aktivitetslogger: Aktivitetslogger) {
            val påminnelse = message.asPåminnelse()
            hendelseProbe.onPåminnelse(påminnelse)
            person(påminnelse).håndter(påminnelse)
        }

        private fun ArbeidstakerHendelse.kontrollerNeeds() {
            //Send needs
        }

        private fun person(arbeidstakerHendelse: ArbeidstakerHendelse): Person {
            arbeidstakerHendelse.addObserver(behovMediator)
            return (personRepository.hentPerson(arbeidstakerHendelse.aktørId()) ?: Person(
                aktørId = arbeidstakerHendelse.aktørId(),
                fødselsnummer = arbeidstakerHendelse.fødselsnummer()
            )).also {
                it.addObserver(personObserver)
                it.addObserver(lagrePersonDao)
                it.addObserver(lagreUtbetalingDao)
                it.addObserver(vedtaksperiodeProbe)
            }
        }
    }

    private class PersonMediator(
        private val producer: KafkaProducer<String, String>,
        private val sikkerLogg: Logger
    ) : PersonObserver {
        private val log = LoggerFactory.getLogger(this::class.java)

        override fun personEndret(personEndretEvent: PersonObserver.PersonEndretEvent) {}

        override fun vedtaksperiodeTrengerLøsning(behov: Behov) {
            producer.send(behov.producerRecord()).get().also {
                log.info(
                    "produserte behov=$behov, {}, {}, {}",
                    keyValue("vedtaksperiodeId", behov.vedtaksperiodeId()),
                    keyValue("partisjon", it.partition()),
                    keyValue("offset", it.offset())
                )
            }
        }

        override fun vedtaksperiodeEndret(event: PersonObserver.VedtaksperiodeEndretTilstandEvent) {
            producer.send(event.producerRecord()).get()
        }

        override fun vedtaksperiodeTilUtbetaling(event: PersonObserver.UtbetalingEvent) {
            producer.send(event.producerRecord()).get().also {
                log.info(
                    "legger vedtatt vedtak: {} på topic med {} og {}",
                    keyValue("vedtaksperiodeId", event.vedtaksperiodeId),
                    keyValue("partisjon", it.partition()),
                    keyValue("offset", it.offset())
                )
            }
        }

        override fun vedtaksperiodeIkkeFunnet(vedtaksperiodeEvent: PersonObserver.VedtaksperiodeIkkeFunnetEvent) {
            producer.send(vedtaksperiodeEvent.producerRecord())
        }

        private fun Behov.producerRecord() =
            ProducerRecord<String, String>(Topics.rapidTopic, fødselsnummer(), toJson()).also {
                sikkerLogg.info(
                    "produserte behov {} med for {}",
                    keyValue("id", this.id()),
                    keyValue("behovtype", this.behovType()),
                    keyValue("vedtaksperiodeId", this.vedtaksperiodeId())
                )
            }

    }

    private class BehovMediator(producer: KafkaProducer<String, String>) : HendelseObserver {
        private val behov = mutableListOf<BehovType>()
        override fun onBehov(behov: BehovType) {
            this.behov.add(behov)
        }

        fun finalize() {

        }
    }
}
