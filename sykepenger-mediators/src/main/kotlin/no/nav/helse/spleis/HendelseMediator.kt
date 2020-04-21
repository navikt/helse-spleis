package no.nav.helse.spleis

import no.nav.helse.hendelser.*
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Person
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.toMap
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.db.LagrePersonDao
import no.nav.helse.spleis.db.PersonRepository
import no.nav.helse.spleis.meldinger.model.*
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

) : IHendelseMediator {
    private val log = LoggerFactory.getLogger(HendelseMediator::class.java)
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    private val personMediator = PersonMediator(rapidsConnection)
    private val behovMediator = BehovMediator(rapidsConnection, sikkerLogg)

    override fun behandle(message: NySøknadMessage, sykmelding: Sykmelding) {
        håndter(message, sykmelding) { person ->
            HendelseProbe.onSykmelding()
            person.håndter(sykmelding)
        }
    }

    override fun behandle(message: SendtSøknadArbeidsgiverMessage, søknad: SøknadArbeidsgiver) {
        håndter(message, søknad) { person ->
            HendelseProbe.onSøknadArbeidsgiver()
            person.håndter(søknad)
        }
    }

    override fun behandle(message: SendtSøknadNavMessage, søknad: Søknad) {
        håndter(message, søknad) { person ->
            HendelseProbe.onSøknadNav()
            person.håndter(søknad)
        }
    }

    override fun behandle(message: InntektsmeldingMessage, inntektsmelding: Inntektsmelding) {
        håndter(message, inntektsmelding) { person ->
            HendelseProbe.onInntektsmelding()
            person.håndter(inntektsmelding)
        }
    }

    override fun behandle(message: YtelserMessage, ytelser: Ytelser) {
        håndter(message, ytelser) { person ->
            HendelseProbe.onYtelser()
            person.håndter(ytelser)
        }
    }

    override fun behandle(message: VilkårsgrunnlagMessage, vilkårsgrunnlag: Vilkårsgrunnlag) {
        håndter(message, vilkårsgrunnlag) { person ->
            HendelseProbe.onVilkårsgrunnlag()
            person.håndter(vilkårsgrunnlag)
        }
    }

    override fun behandle(message: SimuleringMessage, simulering: Simulering) {
        håndter(message, simulering) { person ->
            HendelseProbe.onSimulering()
            person.håndter(simulering)
        }
    }

    override fun behandle(message: ManuellSaksbehandlingMessage, manuellSaksbehandling: ManuellSaksbehandling) {
        håndter(message, manuellSaksbehandling) { person ->
            HendelseProbe.onManuellSaksbehandling()
            person.håndter(manuellSaksbehandling)
        }
    }

    override fun behandle(message: UtbetalingMessage, utbetaling: UtbetalingHendelse) {
        håndter(message, utbetaling) { person ->
            HendelseProbe.onUtbetaling()
            person.håndter(utbetaling)
        }
    }

    override fun behandle(message: PåminnelseMessage, påminnelse: Påminnelse) {
        håndter(message, påminnelse) { person ->
            HendelseProbe.onPåminnelse(påminnelse)
            person.håndter(påminnelse)
        }
    }

    override fun behandle(message: KansellerUtbetalingMessage, kansellerUtbetaling: KansellerUtbetaling) {
        håndter(message, kansellerUtbetaling) { person ->
            HendelseProbe.onKansellerUtbetaling()
            person.håndter(kansellerUtbetaling)
        }
    }

    private fun <Hendelse: ArbeidstakerHendelse> håndter(message: HendelseMessage, hendelse: Hendelse, handler: (Person) -> Unit) {
        person(message, hendelse).also {
            handler(it)
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
        lagrePersonDao.lagrePerson(message, person, hendelse)

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

internal interface IHendelseMediator {
    fun behandle(message: NySøknadMessage, sykmelding: Sykmelding)
    fun behandle(message: SendtSøknadArbeidsgiverMessage, søknad: SøknadArbeidsgiver)
    fun behandle(message: SendtSøknadNavMessage, søknad: Søknad)
    fun behandle(message: InntektsmeldingMessage, inntektsmelding: Inntektsmelding)
    fun behandle(message: PåminnelseMessage, påminnelse: Påminnelse)
    fun behandle(message: YtelserMessage, ytelser: Ytelser)
    fun behandle(message: VilkårsgrunnlagMessage, vilkårsgrunnlag: Vilkårsgrunnlag)
    fun behandle(message: ManuellSaksbehandlingMessage, manuellSaksbehandling: ManuellSaksbehandling)
    fun behandle(message: UtbetalingMessage, utbetaling: UtbetalingHendelse)
    fun behandle(message: SimuleringMessage, simulering: Simulering)
    fun behandle(message: KansellerUtbetalingMessage, kansellerUtbetaling: KansellerUtbetaling)
}
