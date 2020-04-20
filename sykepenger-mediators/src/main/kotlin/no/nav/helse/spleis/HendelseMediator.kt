package no.nav.helse.spleis

import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Person
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.toMap
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.db.LagrePersonDao
import no.nav.helse.spleis.db.PersonRepository
import no.nav.helse.spleis.hendelser.MessageProcessor
import no.nav.helse.spleis.hendelser.model.*
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

// Understands how to communicate messages to other objects
// Acts like a GoF Mediator to forward messages to observers
// Uses GoF Observer pattern to notify events
internal class HendelseMediator(
    rapidsConnection: RapidsConnection,
    private val personRepository: PersonRepository,
    private val lagrePersonDao: LagrePersonDao

) : MessageProcessor {
    private val log = LoggerFactory.getLogger(HendelseMediator::class.java)
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    private val personMediator = PersonMediator(rapidsConnection)
    private val behovMediator = BehovMediator(rapidsConnection, sikkerLogg)

    override fun process(message: NySøknadMessage) {
        håndter(message, message.asSykmelding()) { person, sykmelding ->
            HendelseProbe.onSykmelding()
            person.håndter(sykmelding)
        }
    }

    override fun process(message: SendtSøknadArbeidsgiverMessage) {
        håndter(message, message.asSøknadArbeidsgiver()) { person, søknad ->
            HendelseProbe.onSøknadArbeidsgiver()
            person.håndter(søknad)
        }
    }

    override fun process(message: SendtSøknadNavMessage) {
        håndter(message, message.asSøknad()) { person, søknad ->
            HendelseProbe.onSøknadNav()
            person.håndter(søknad)
        }
    }

    override fun process(message: InntektsmeldingMessage) {
        håndter(message, message.asInntektsmelding()) { person, inntektsmelding ->
            HendelseProbe.onInntektsmelding()
            person.håndter(inntektsmelding)
        }
    }

    override fun process(message: YtelserMessage) {
        håndter(message, message.asYtelser()) { person, ytelser ->
            HendelseProbe.onYtelser()
            person.håndter(ytelser)
        }
    }

    override fun process(message: VilkårsgrunnlagMessage) {
        håndter(message, message.asVilkårsgrunnlag()) { person, vilkårsgrunnlag ->
            HendelseProbe.onVilkårsgrunnlag()
            person.håndter(vilkårsgrunnlag)
        }
    }

    override fun process(message: SimuleringMessage) {
        håndter(message, message.asSimulering()) { person, simulering ->
            HendelseProbe.onSimulering()
            person.håndter(simulering)
        }
    }

    override fun process(message: ManuellSaksbehandlingMessage) {
        håndter(message, message.asManuellSaksbehandling()) { person, manuellSaksbehandling ->
            HendelseProbe.onManuellSaksbehandling()
            person.håndter(manuellSaksbehandling)
        }
    }

    override fun process(message: UtbetalingMessage) {
        håndter(message, message.asUtbetaling()) { person, utbetaling ->
            HendelseProbe.onUtbetaling()
            person.håndter(utbetaling)
        }
    }

    override fun process(message: PåminnelseMessage) {
        håndter(message, message.asPåminnelse()) { person, påminnelse ->
            HendelseProbe.onPåminnelse(påminnelse)
            person.håndter(påminnelse)
        }
    }

    override fun process(message: KansellerUtbetalingMessage) {
        håndter(message, message.asKansellerUtbetaling()) { person, kansellerUtbetaling ->
            HendelseProbe.onKansellerUtbetaling()
            person.håndter(kansellerUtbetaling)
        }
    }

    private fun <Hendelse: ArbeidstakerHendelse> håndter(message: HendelseMessage, hendelse: Hendelse, handler: (Person, Hendelse) -> Unit) {
        person(message, hendelse).also {
            handler(it, hendelse)
            finalize(it, message, hendelse)
        }
    }

    private fun person(message: HendelseMessage, hendelse: ArbeidstakerHendelse): Person {
        return (personRepository.hentPerson(hendelse.fødselsnummer()) ?: Person(
            aktørId = hendelse.aktørId(),
            fødselsnummer = hendelse.fødselsnummer()
        )).also {
            personMediator.observer(it, message, hendelse)
            it.addObserver(VedtaksperiodeProbe)
        }
    }

    private fun finalize(person: Person, message: HendelseMessage, hendelse: ArbeidstakerHendelse) {
        lagrePersonDao.lagrePerson(person, hendelse)

        if (!hendelse.hasMessages()) return
        if (hendelse.hasErrors()) sikkerLogg.info("aktivitetslogg inneholder errors:\n${hendelse.toLogString()}")
        else sikkerLogg.info("aktivitetslogg inneholder meldinger:\n${hendelse.toLogString()}")
        behovMediator.håndter(message, hendelse)
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
                    "utbetalingslinjer" to event.utbetalingslinjer.map {
                        mapOf(
                            "fom" to it.fom,
                            "tom" to it.tom,
                            "dagsats" to it.dagsats,
                            "grad" to it.grad
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

