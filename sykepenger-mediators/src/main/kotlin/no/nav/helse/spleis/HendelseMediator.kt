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
    private val log = LoggerFactory.getLogger(HendelseMediator::class.java)
    private val sikkerLogg = LoggerFactory.getLogger("sikkerLogg")
    private val parser = Parser(this, rapidsConnection)

    private val personMediator = PersonMediator(rapidsConnection)
    private val behovMediator = BehovMediator(rapidsConnection, sikkerLogg)
    private val messageProcessor = Processor()

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
            withMDC(mapOf(
                "melding_id" to message.id.toString(),
                "melding_type" to (message::class.simpleName ?: "ukjent")
            )) {
                log.error("alvorlig feil i aktivitetslogg: ${err.message}", err)

                withMDC(mapOf("fødselsnummer" to message.fødselsnummer)) {
                    sikkerLogg.error("alvorlig feil i aktivitetslogg: ${err.message}", err)
                }
            }
        }
    }

    private fun withMDC(context: Map<String, String>, block: () -> Unit) {
        val contextMap = MDC.getCopyOfContextMap() ?: emptyMap()
        try {
            MDC.setContextMap(contextMap + context)
            block()
        } finally {
            MDC.setContextMap(contextMap)
        }
    }

    override fun onMessageException(exception: MessageProblems.MessageException) {
        sikkerLogg.error("feil ved parsing av melding: {}", exception)
    }

    override fun onUnrecognizedMessage(message: String, problems: List<Pair<String, MessageProblems>>) {
        sikkerLogg.debug("ukjent melding:\n\t$message\n\nProblemer:\n${problems.joinToString(separator = "\n") { "${it.first}:\n${it.second}" }}")
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

    private fun finalize(person: Person, hendelse: ArbeidstakerHendelse) {
        if (!hendelse.hasMessages()) return
        if (hendelse.hasErrors()) return sikkerLogg.info("aktivitetslogg inneholder errors: ${hendelse.toLogString()}")
        sikkerLogg.info("aktivitetslogg inneholder meldinger: ${hendelse.toLogString()}")

        behovMediator.håndter(hendelse)
    }

    private inner class Processor() : MessageProcessor {
        override fun process(message: NySøknadMessage) {
            håndter(message.asSykmelding()) { person, sykmelding ->
                hendelseProbe.onSykmelding()
                person.håndter(sykmelding)
            }
        }

        override fun process(message: SendtSøknadMessage) {
            håndter(message.asSøknad()) { person, søknad ->
                hendelseProbe.onSøknad()
                person.håndter(søknad)
            }
        }

        override fun process(message: InntektsmeldingMessage) {
            håndter(message.asInntektsmelding()) { person, inntektsmelding ->
                hendelseProbe.onInntektsmelding()
                person.håndter(inntektsmelding)
            }
        }

        override fun process(message: YtelserMessage) {
            håndter(message.asYtelser()) { person, ytelser ->
                hendelseProbe.onYtelser()
                person.håndter(ytelser)
            }
        }

        override fun process(message: VilkårsgrunnlagMessage) {
            håndter(message.asVilkårsgrunnlag()) { person, vilkårsgrunnlag ->
                hendelseProbe.onVilkårsgrunnlag()
                person.håndter(vilkårsgrunnlag)
            }
        }

        override fun process(message: ManuellSaksbehandlingMessage) {
            håndter(message.asManuellSaksbehandling()) { person, manuellSaksbehandling ->
                hendelseProbe.onManuellSaksbehandling()
                person.håndter(manuellSaksbehandling)
            }
        }

        override fun process(message: UtbetalingMessage) {
            håndter(message.asUtbetaling()) { person, utbetaling ->
                hendelseProbe.onUtbetaling()
                person.håndter(utbetaling)
            }
        }

        override fun process(message: PåminnelseMessage) {
            håndter(message.asPåminnelse()) { person, påminnelse ->
                hendelseProbe.onPåminnelse(påminnelse)
                person.håndter(påminnelse)
            }
        }

        private fun <Hendelse: ArbeidstakerHendelse> håndter(hendelse: Hendelse, handler: (Person, Hendelse) -> Unit) {
            person(hendelse).also {
                handler(it, hendelse)
                finalize(it, hendelse)
            }
        }
    }

    private class PersonMediator(private val rapidsConnection: RapidsConnection) : PersonObserver {

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
