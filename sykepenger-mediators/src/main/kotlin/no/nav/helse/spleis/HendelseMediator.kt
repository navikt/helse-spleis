package no.nav.helse.spleis

import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.person.*
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.db.HendelseRecorder
import no.nav.helse.spleis.db.LagrePersonDao
import no.nav.helse.spleis.db.PersonRepository
import no.nav.helse.spleis.hendelser.Parser
import no.nav.helse.spleis.hendelser.model.*
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.time.LocalDateTime
import java.util.*

// Understands how to communicate messages to other objects
// Acts like a GoF Mediator to forward messages to observers
// Uses GoF Observer pattern to notify events
internal class HendelseMediator(
    rapidsConnection: RapidsConnection,
    private val personRepository: PersonRepository,
    private val lagrePersonDao: LagrePersonDao,
    private val hendelseRecorder: HendelseRecorder
) : Parser.ParserDirector {
    private val log = LoggerFactory.getLogger(HendelseMediator::class.java)
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    private val parser = Parser(this, rapidsConnection)

    private val personMediator = PersonMediator(rapidsConnection)
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

    internal fun person(message: HendelseMessage, hendelse: ArbeidstakerHendelse): Person {
        return (personRepository.hentPerson(hendelse.fødselsnummer()) ?: Person(
            aktørId = hendelse.aktørId(),
            fødselsnummer = hendelse.fødselsnummer()
        )).also {
            personMediator.observer(it, message, hendelse)
            it.addObserver(VedtaksperiodeProbe)
        }
    }

    internal fun finalize(person: Person, message: HendelseMessage, hendelse: ArbeidstakerHendelse) {
        lagrePersonDao.lagrePerson(person, hendelse)

        if (!hendelse.hasMessages()) return
        if (hendelse.hasErrors()) sikkerLogg.info("aktivitetslogg inneholder errors:\n${hendelse.toLogString()}")
        else sikkerLogg.info("aktivitetslogg inneholder meldinger:\n${hendelse.toLogString()}")
        behovMediator.håndter(message, hendelse)
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

    private class PersonMediator(private val rapidsConnection: RapidsConnection) {

        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

        fun observer(person: Person, message: HendelseMessage, hendelse: ArbeidstakerHendelse) {
            person.addObserver(Observatør(message, hendelse))
        }

        private fun publish(fødselsnummer: String, message: String) {
            rapidsConnection.publish(fødselsnummer, message.also { sikkerLogg.info("sender $it") })
        }

        private inner class Observatør(
            private val message: HendelseMessage,
            private val hendelse: ArbeidstakerHendelse
        ): PersonObserver {
            override fun vedtaksperiodePåminnet(påminnelse: Påminnelse) {
                publish("vedtaksperiode_påminnet", JsonMessage.newMessage(mapOf(
                    "vedtaksperiodeId" to påminnelse.vedtaksperiodeId,
                    "tilstand" to påminnelse.tilstand(),
                    "antallGangerPåminnet" to påminnelse.antallGangerPåminnet(),
                    "tilstandsendringstidspunkt" to påminnelse.tilstandsendringstidspunkt(),
                    "påminnelsestidspunkt" to påminnelse.påminnelsestidspunkt(),
                    "nestePåminnelsestidspunkt" to påminnelse.nestePåminnelsestidspunkt()
                )))
            }

            override fun vedtaksperiodeEndret(event: PersonObserver.VedtaksperiodeEndretTilstandEvent) {
                publish("vedtaksperiode_endret", JsonMessage.newMessage(mapOf(
                    "vedtaksperiodeId" to event.vedtaksperiodeId,
                    "gjeldendeTilstand" to event.gjeldendeTilstand,
                    "forrigeTilstand" to event.forrigeTilstand,
                    "endringstidspunkt" to event.endringstidspunkt,
                    "aktivitetslogg" to event.aktivitetslogg.toMap(),
                    "timeout" to event.timeout.toSeconds(),
                    "hendelser" to event.hendelser
                )))
            }

            override fun vedtaksperiodeUtbetalt(event: PersonObserver.UtbetaltEvent) {
                publish("utbetalt", JsonMessage.newMessage(mapOf(
                    "førsteFraværsdag" to event.førsteFraværsdag,
                    "vedtaksperiodeId" to event.vedtaksperiodeId.toString(),
                    "utbetaling" to event.utbetalingslinjer.map {
                        mapOf(
                            "utbetalingsreferanse" to it.utbetalingsreferanse,
                            "utbetalingslinjer" to it.utbetalingslinjer.map {
                                mapOf(
                                    "fom" to it.fom,
                                    "tom" to it.tom,
                                    "dagsats" to it.dagsats,
                                    "grad" to it.grad
                                )
                            }
                        )
                    },
                    "forbrukteSykedager" to event.forbrukteSykedager,
                    "opprettet" to event.opprettet
                )))
            }

            override fun vedtaksperiodeIkkeFunnet(vedtaksperiodeEvent: PersonObserver.VedtaksperiodeIkkeFunnetEvent) {
                publish("vedtaksperiode_ikke_funnet", JsonMessage.newMessage(mapOf(
                    "vedtaksperiodeId" to vedtaksperiodeEvent.vedtaksperiodeId
                )))
            }

            override fun manglerInntektsmelding(event: PersonObserver.ManglendeInntektsmeldingEvent) {
                publish("trenger_inntektsmelding", JsonMessage.newMessage(mapOf(
                    "vedtaksperiodeId" to event.vedtaksperiodeId,
                    "opprettet" to event.opprettet,
                    "fom" to event.fom,
                    "tom" to event.tom
                )))
            }

            private fun leggPåStandardfelter(event: String, outgoingMessage: JsonMessage) = outgoingMessage.apply {
                this["@event_name"] = event
                this["@id"] = UUID.randomUUID()
                this["@opprettet"] = LocalDateTime.now()
                this["@forårsaket_av"] = mapOf(
                    "event_name" to message.navn,
                    "id" to message.id,
                    "opprettet" to message.opprettet
                )
                this["aktørId"] = hendelse.aktørId()
                this["fødselsnummer"] = hendelse.fødselsnummer()
                this["organisasjonsnummer"] = hendelse.organisasjonsnummer()
            }

            private fun publish(event: String, outgoingMessage: JsonMessage) {
                publish(hendelse.fødselsnummer(), leggPåStandardfelter(event, outgoingMessage).toJson())
            }
        }
    }
}

