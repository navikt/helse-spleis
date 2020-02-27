package no.nav.helse.spleis

import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.person.*
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.db.HendelseRecorder
import no.nav.helse.spleis.db.PersonRepository
import no.nav.helse.spleis.hendelser.MessageProcessor
import no.nav.helse.spleis.hendelser.Parser
import no.nav.helse.spleis.hendelser.model.*
import org.slf4j.LoggerFactory

// Understands how to communicate messages to other objects
// Acts like a GoF Mediator to forward messages to observers
// Uses GoF Observer pattern to notify events
internal class HendelseMediator(
    private val rapidsConnection: RapidsConnection,
    private val personRepository: PersonRepository,
    private val lagrePersonDao: PersonObserver,
    private val lagreUtbetalingDao: PersonObserver,
    private val vedtaksperiodeProbe: VedtaksperiodeProbe = VedtaksperiodeProbe,
    private val hendelseProbe: HendelseProbe,
    private val hendelseRecorder: HendelseRecorder
) : Parser.ParserDirector {
    private val sikkerLogg = LoggerFactory.getLogger("sikkerLogg")
    private val parser = Parser(this, rapidsConnection)

    private val personObserver = PersonMediator(rapidsConnection)

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
        sikkerLogg.debug("gjenkjente melding {} som {}", message.id, message::class.simpleName)
        val aktivitetslogger = Aktivitetslogger()
        val aktivitetslogg = Aktivitetslogg()
        val messageProcessor = Processor(BehovMediator(rapidsConnection, sikkerLogg))
        try {
            message.accept(hendelseRecorder)
            message.accept(messageProcessor)

            if (aktivitetslogger.hasErrorsOld() || aktivitetslogg.hasErrors()) {
                sikkerLogg.error("aktivitetslogger inneholder errors: ${aktivitetslogger.toReport()}")
            } else if (aktivitetslogger.hasMessagesOld() || aktivitetslogg.hasMessages()) {
                sikkerLogg.info("aktivitetslogger inneholder meldinger: ${aktivitetslogger.toReport()}")
            }
        } catch (err: Aktivitetslogger.AktivitetException) {
            sikkerLogg.error("feil på melding: $err")
        } catch (err: Aktivitetslogg.AktivitetException) {
            sikkerLogg.error("feil på melding: $err")
        } catch (err: UtenforOmfangException) {
            sikkerLogg.error("melding er utenfor omfang: ${err.message}", err)
        } finally {
            AktivitetsloggerProbe.inspiser(aktivitetslogger)
        }
    }

    override fun onMessageException(exception: MessageProblems.MessageException) {
        sikkerLogg.error("feil på melding: {}", exception)
    }

    override fun onUnrecognizedMessage(problems: List<Pair<String, MessageProblems>>) {
        sikkerLogg.debug("ukjent melding:\n${problems.joinToString(separator = "\n") { "${it.first}:\n${it.second}" }}")
    }

    private inner class Processor(private val behovMediator: BehovMediator) : MessageProcessor {
        override fun process(message: NySøknadMessage) {
            val sykmelding = message.asSykmelding()
            hendelseProbe.onSykmelding(sykmelding)
            person(sykmelding).håndter(sykmelding)
            behovMediator.finalize(sykmelding)
        }

        override fun process(message: SendtSøknadMessage) {
            val søknad = message.asSøknad()
            hendelseProbe.onSøknad(søknad)
            person(søknad).håndter(søknad)
            behovMediator.finalize(søknad)
        }

        override fun process(message: InntektsmeldingMessage) {
            val inntektsmelding = message.asInntektsmelding()
            hendelseProbe.onInntektsmelding(inntektsmelding)
            person(inntektsmelding).håndter(inntektsmelding)
            behovMediator.finalize(inntektsmelding)
        }

        override fun process(message: YtelserMessage) {
            val ytelser = message.asYtelser()
            hendelseProbe.onYtelser(ytelser)
            person(ytelser).håndter(ytelser)
            behovMediator.finalize(ytelser)
        }

        override fun process(message: VilkårsgrunnlagMessage) {
            val vilkårsgrunnlag = message.asVilkårsgrunnlag()
            hendelseProbe.onVilkårsgrunnlag(vilkårsgrunnlag)
            person(vilkårsgrunnlag).håndter(vilkårsgrunnlag)
            behovMediator.finalize(vilkårsgrunnlag)
        }

        override fun process(message: ManuellSaksbehandlingMessage) {
            val manuellSaksbehandling = message.asManuellSaksbehandling()
            hendelseProbe.onManuellSaksbehandling(manuellSaksbehandling)
            person(manuellSaksbehandling).håndter(manuellSaksbehandling)
            behovMediator.finalize(manuellSaksbehandling)
        }

        override fun process(message: UtbetalingMessage) {
            val utbetaling = message.asUtbetaling()
            hendelseProbe.onUtbetaling(utbetaling)
            person(utbetaling).håndter(utbetaling)
            behovMediator.finalize(utbetaling)
        }

        override fun process(message: PåminnelseMessage) {
            val påminnelse = message.asPåminnelse()
            hendelseProbe.onPåminnelse(påminnelse)
            person(påminnelse).håndter(påminnelse)
            behovMediator.finalize(påminnelse)
        }

        private fun person(arbeidstakerHendelse: ArbeidstakerHendelse): Person {
            arbeidstakerHendelse.addObserver(behovMediator)
            arbeidstakerHendelse.addObserver(vedtaksperiodeProbe)
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
