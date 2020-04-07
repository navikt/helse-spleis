package no.nav.helse.spleis

import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Person
import no.nav.helse.person.PersonObserver
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.db.HendelseRecorder
import no.nav.helse.spleis.db.LagrePersonDao
import no.nav.helse.spleis.db.LagreUtbetalingDao
import no.nav.helse.spleis.db.PersonRepository
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
    private val lagrePersonDao: LagrePersonDao,
    lagreUtbetalingDao: LagreUtbetalingDao,
    private val hendelseRecorder: HendelseRecorder
) : Parser.ParserDirector {
    private val log = LoggerFactory.getLogger(HendelseMediator::class.java)
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    private val parser = Parser(this, rapidsConnection)

    private val personMediator = PersonMediator(rapidsConnection, lagreUtbetalingDao)
    private val behovMediator = BehovMediator(rapidsConnection, sikkerLogg)
    private val messageProcessor = HendelseProcessor(this)

    init {
        parser.register(NySøknadMessage.Factory)
        parser.register(SendtSøknadArbeidsgiverMessage.Factory)
        parser.register(SendtSøknadNavMessage.Factory)
        parser.register(InntektsmeldingMessage.Factory)
        parser.register(YtelserMessage.Factory)
        parser.register(VilkårsgrunnlagMessage.Factory)
        parser.register(ManuellSaksbehandlingMessage.Factory)
        parser.register(UtbetalingMessage.Factory)
        parser.register(PåminnelseMessage.Factory)
        parser.register(SimuleringMessage.Factory)
    }

    override fun onRecognizedMessage(message: HendelseMessage, context: RapidsConnection.MessageContext) {
        withMDC(mapOf(
            "melding_id" to message.id.toString(),
            "melding_type" to (message::class.simpleName ?: "ukjent")
        )) {
            sikkerLogg.info("gjenkjente melding id={} for fnr={} som {}", message.id, message.fødselsnummer, message::class.simpleName)
            try {
                message.accept(hendelseRecorder)
                message.accept(messageProcessor)
            } catch (err: Aktivitetslogg.AktivitetException) {
                withMDC(err.kontekst()) {
                    sikkerLogg.error("alvorlig feil i aktivitetslogg: ${err.message}", err)
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
    }

    override fun onMessageException(exception: MessageProblems.MessageException) {
        sikkerLogg.error("feil ved parsing av melding: {}", exception)
    }

    override fun onUnrecognizedMessage(message: String, problems: List<Pair<String, MessageProblems>>) {
        sikkerLogg.debug("ukjent melding:\n\t$message\n\nProblemer:\n${problems.joinToString(separator = "\n") { "${it.first}:\n${it.second}" }}")
    }

    internal fun person(arbeidstakerHendelse: ArbeidstakerHendelse): Person {
        return (personRepository.hentPerson(arbeidstakerHendelse.fødselsnummer()) ?: Person(
            aktørId = arbeidstakerHendelse.aktørId(),
            fødselsnummer = arbeidstakerHendelse.fødselsnummer()
        )).also {
            it.addObserver(personMediator)
            it.addObserver(VedtaksperiodeProbe)
        }
    }

    internal fun finalize(person: Person, hendelse: ArbeidstakerHendelse) {
        lagrePersonDao.lagrePerson(person, hendelse)

        if (!hendelse.hasMessages()) return
        if (hendelse.hasErrors()) sikkerLogg.info("aktivitetslogg inneholder errors:\n${hendelse.toLogString()}")
        else sikkerLogg.info("aktivitetslogg inneholder meldinger:\n${hendelse.toLogString()}")
        behovMediator.håndter(hendelse)
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

    private class PersonMediator(
        private val rapidsConnection: RapidsConnection,
        private val lagreUtbetalingDao: LagreUtbetalingDao
    ) : PersonObserver {

        override fun vedtaksperiodeTilUtbetaling(event: PersonObserver.UtbetalingEvent) {
            lagreUtbetalingDao.lagreUtbetaling(
                event.utbetalingsreferanse,
                event.aktørId,
                event.organisasjonsnummer,
                event.vedtaksperiodeId
            )
        }

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

        override fun manglerInntektsmelding(event: PersonObserver.ManglendeInntektsmeldingEvent) {
            rapidsConnection.publish(event.fødselsnummer, event.toJson())
        }
    }
}

