package no.nav.helse.spleis

import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Person
import no.nav.helse.person.PersonObserver
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.db.HendelseRecorder
import no.nav.helse.spleis.db.PersonRepository
import no.nav.helse.spleis.hendelser.MessageProcessor
import no.nav.helse.spleis.hendelser.Parser
import no.nav.helse.spleis.hendelser.model.*
import org.slf4j.LoggerFactory
import org.slf4j.MDC

// Understands how to communicate messages to other objects
// Acts like a GoF Mediator to forward messages to observers
// Uses GoF Observer pattern to notify events
internal class HendelseMediator(
    rapidsConnection: RapidsConnection,
    private val personRepository: PersonRepository,
    private val lagrePersonDao: PersonObserver,
    private val lagreUtbetalingDao: PersonObserver,
    private val vedtaksperiodeProbe: VedtaksperiodeProbe = VedtaksperiodeProbe,
    private val hendelseProbe: HendelseProbe,
    private val hendelseRecorder: HendelseRecorder
) : Parser.ParserDirector {
    private val sikkerLogg = LoggerFactory.getLogger("sikkerLogg")
    private val parser = Parser(this, rapidsConnection)
    private val messageProcessor = Processor(PersonMediator(rapidsConnection), BehovMediator(rapidsConnection, sikkerLogg))

    init {
        parser.register(NySøknadMessage.Factory)
        parser.register(SendtSøknadMessage.Factory)
        parser.register(InntektsmeldingMessage.Factory)
        parser.register(YtelserMessage.Factory)
        parser.register(VilkårsgrunnlagMessage.Factory)
        parser.register(ManuellSaksbehandlingMessage.Factory)
        parser.register(UtbetalingMessage.Factory)
        parser.register(PåminnelseMessage.Factory)
    }

    override fun onRecognizedMessage(message: HendelseMessage, context: RapidsConnection.MessageContext) {
        sikkerLogg.info("gjenkjente melding id={} for fnr={} som {}", message.id, message.fødselsnummer, message::class.simpleName)
        try {
            message.accept(hendelseRecorder)
            message.accept(messageProcessor)
        } catch (err: Aktivitetslogg.AktivitetException) {
            val contextMap = MDC.getCopyOfContextMap() ?: emptyMap()
            try {
                MDC.setContextMap(contextMap + err.kontekst())
                sikkerLogg.error("alvorlig feil i aktivitetslogg: ${err.message}", err)
            } finally {
                MDC.setContextMap(contextMap)
            }
        } catch (err: Exception) {
            MDC.put("melding_id", message.id.toString())
            MDC.put("melding_type", message::class.simpleName)
            throw err
        }
    }

    override fun onMessageException(exception: MessageProblems.MessageException) {
        sikkerLogg.error("feil ved parsing av melding: {}", exception)
    }

    override fun onUnrecognizedMessage(message: String, problems: List<Pair<String, MessageProblems>>) {
        sikkerLogg.debug("ukjent melding:\n\t$message\n\nProblemer:\n${problems.joinToString(separator = "\n") { "${it.first}:\n${it.second}" }}")
    }

    private inner class Processor(private val personMediator: PersonMediator, private val behovMediator: BehovMediator) : MessageProcessor {
        override fun process(message: NySøknadMessage) {
            person(message.asSykmelding()) { person, sykmelding ->
                hendelseProbe.onSykmelding()
                person.håndter(sykmelding)
            }
        }

        override fun process(message: SendtSøknadMessage) {
            person(message.asSøknad()) { person, søknad ->
                hendelseProbe.onSøknad()
                person.håndter(søknad)
            }
        }

        override fun process(message: InntektsmeldingMessage) {
            person(message.asInntektsmelding()) { person, inntektsmelding ->
                hendelseProbe.onInntektsmelding()
                person.håndter(inntektsmelding)
            }
        }

        override fun process(message: YtelserMessage) {
            person(message.asYtelser()) { person, ytelser ->
                hendelseProbe.onYtelser()
                person.håndter(ytelser)
            }
        }

        override fun process(message: VilkårsgrunnlagMessage) {
            person(message.asVilkårsgrunnlag()) { person, vilkårsgrunnlag ->
                hendelseProbe.onVilkårsgrunnlag()
                person.håndter(vilkårsgrunnlag)
            }
        }

        override fun process(message: ManuellSaksbehandlingMessage) {
            person(message.asManuellSaksbehandling()) { person, manuellSaksbehandling ->
                hendelseProbe.onManuellSaksbehandling()
                person.håndter(manuellSaksbehandling)
            }
        }

        override fun process(message: UtbetalingMessage) {
            person(message.asUtbetaling()) { person, utbetaling ->
                hendelseProbe.onUtbetaling()
                person.håndter(utbetaling)
            }
        }

        override fun process(message: PåminnelseMessage) {
            person(message.asPåminnelse()) { person, påminnelse ->
                hendelseProbe.onPåminnelse(påminnelse)
                person.håndter(påminnelse)
            }
        }

        private fun <Hendelse: ArbeidstakerHendelse> person(hendelse: Hendelse, handler: (Person, Hendelse) -> Unit) {
            handler(person(hendelse), hendelse)
            finalize(hendelse)
        }

        private fun person(arbeidstakerHendelse: ArbeidstakerHendelse): Person {
            return (personRepository.hentPerson(arbeidstakerHendelse.fødselsnummer()) ?: Person(
                aktørId = arbeidstakerHendelse.aktørId(),
                fødselsnummer = arbeidstakerHendelse.fødselsnummer()
            )).also {
                it.addObserver(personMediator)
                it.addObserver(lagrePersonDao)
                it.addObserver(lagreUtbetalingDao)
                it.addObserver(vedtaksperiodeProbe)
            }
        }

        private fun finalize(hendelse: ArbeidstakerHendelse) {
            if (!hendelse.hasMessages()) return
            if (hendelse.hasErrors()) return sikkerLogg.info("aktivitetslogg inneholder errors: ${hendelse.toLogString()}")
            sikkerLogg.info("aktivitetslogg inneholder meldinger: ${hendelse.toLogString()}")

            behovMediator.håndter(hendelse)
        }
    }

    private class PersonMediator(private val rapidsConnection: RapidsConnection) : PersonObserver {

        override fun personEndret(personEndretEvent: PersonObserver.PersonEndretEvent) {}

        override fun vedtaksperiodePåminnet(påminnelse: Påminnelse) {
            rapidsConnection.publish(påminnelse.fødselsnummer(), påminnelse.toJson())
        }

        override fun vedtaksperiodeEndret(event: PersonObserver.VedtaksperiodeEndretTilstandEvent) {
            rapidsConnection.publish(event.fødselsnummer, event.toJson())
        }

        override fun vedtaksperiodeUtbetalt(event: PersonObserver.UtbetaltEvent) {
            rapidsConnection.publish(event.fødselsnummer, event.toJson())
        }

        override fun vedtaksperiodeIkkeFunnet(vedtaksperiodeEvent: PersonObserver.VedtaksperiodeIkkeFunnetEvent) {
            rapidsConnection.publish(vedtaksperiodeEvent.fødselsnummer, vedtaksperiodeEvent.toJson())
        }
    }

}
