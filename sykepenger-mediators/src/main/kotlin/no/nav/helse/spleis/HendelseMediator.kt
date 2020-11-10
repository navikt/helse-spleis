package no.nav.helse.spleis

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.hendelser.*
import no.nav.helse.person.*
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.db.HendelseRepository
import no.nav.helse.spleis.db.LagrePersonDao
import no.nav.helse.spleis.db.PersonRepository
import no.nav.helse.spleis.db.VedtaksperiodeIdTilstand
import no.nav.helse.spleis.meldinger.model.*
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

// Understands how to communicate messages to other objects
// Acts like a GoF Mediator to forward messages to observers
// Uses GoF Observer pattern to notify events
internal class HendelseMediator(
    private val rapidsConnection: RapidsConnection,
    private val personRepository: PersonRepository,
    hendelseRepository: HendelseRepository,
    private val lagrePersonDao: LagrePersonDao
) : IHendelseMediator {
    private val log = LoggerFactory.getLogger(HendelseMediator::class.java)
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    private val personMediator = PersonMediator(rapidsConnection, personRepository)
    private val behovMediator = BehovMediator(rapidsConnection, sikkerLogg)
    private val replayMediator = ReplayMediator(this, hendelseRepository)

    override fun behandle(message: HendelseMessage) {
        try {
            message.behandle(this)
        } catch (err: Aktivitetslogg.AktivitetException) {
            withMDC(err.kontekst()) {
                sikkerLogg.error("alvorlig feil i aktivitetslogg: ${err.message}\n\t${message.toJson()}", err)
            }
        }
    }

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

    override fun behandle(message: UtbetalingshistorikkMessage, utbetalingshistorikk: Utbetalingshistorikk) {
        håndter(message, utbetalingshistorikk) { person ->
            HendelseProbe.onUtbetalingshistorikk()
            person.håndter(utbetalingshistorikk)
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

    override fun behandle(message: UtbetalingsgodkjenningMessage, utbetalingsgodkjenning: Utbetalingsgodkjenning) {
        håndter(message, utbetalingsgodkjenning) { person ->
            HendelseProbe.onUtbetalingsgodkjenning()
            person.håndter(utbetalingsgodkjenning)
        }
    }

    override fun behandle(message: UtbetalingOverførtMessage, utbetaling: UtbetalingOverført) {
        håndter(message, utbetaling) { person ->
            person.håndter(utbetaling)
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

    override fun behandle(message: AnnulleringMessage, annullerUtbetaling: AnnullerUtbetaling) {
        håndter(message, annullerUtbetaling) { person ->
            HendelseProbe.onAnnullerUtbetaling()
            person.håndter(annullerUtbetaling)
        }
    }

    override fun behandle(message: RollbackMessage, rollback: Rollback) {
        val nyestePersonId = requireNotNull(personRepository.hentNyestePersonId(rollback.fødselsnummer()))
        val vedtaksperiodeIderFørRollback = personRepository.hentVedtaksperiodeIderMedTilstand(nyestePersonId)
        val vedtaksperiodeIderEtterRollback =
            personRepository.hentVedtaksperiodeIderMedTilstand(rollback.personVersjon())

        val vedtaksperioderEndretTilstand = vedtaksperiodeIderFørRollback - vedtaksperiodeIderEtterRollback
        val vedtaksperioderSlettet =
            vedtaksperiodeIderFørRollback.map(VedtaksperiodeIdTilstand::id) - vedtaksperiodeIderEtterRollback.map(
                VedtaksperiodeIdTilstand::id
            )

        sjekkForVedtaksperioderTilUtbetaling(vedtaksperioderEndretTilstand)

        val rollbackPerson = personForId(rollback.personVersjon(), message, rollback)
        rollbackPerson.håndter(rollback)
        val tilbakerulledePerioder =
            personRepository.markerSomTilbakerullet(rollback.fødselsnummer(), rollback.personVersjon())
        sikkerLogg.info(
            "Rullet tilbake {} person for {}, gikk fra {} til {}",
            keyValue("antallPersonIder", tilbakerulledePerioder),
            keyValue("fødselsnummer", rollback.fødselsnummer()),
            keyValue("forigePersonId", nyestePersonId),
            keyValue("rullesTilbakeTilPersonId", rollback.personVersjon())
        )
        finalize(rollbackPerson, message, rollback)

        publiserPersonRulletTilbake(rollback, message, vedtaksperioderSlettet)
    }

    override fun behandle(message: RollbackDeleteMessage, rollback: RollbackDelete) {
        val nyestePersonId = requireNotNull(personRepository.hentNyestePersonId(rollback.fødselsnummer()))
        val vedtaksperioderFørRollback = personRepository.hentVedtaksperiodeIderMedTilstand(nyestePersonId)

        sjekkForVedtaksperioderTilUtbetaling(vedtaksperioderFørRollback)
        val person = Person(rollback.aktørId(), rollback.fødselsnummer())
        person.håndter(rollback)
        val tilbakerulledePerioder = personRepository.markerSomTilbakerullet(rollback.fødselsnummer(), 0)
        sikkerLogg.info(
            "Resetter person {} perioder for {}, original {}",
            keyValue("antallPersonIder", tilbakerulledePerioder),
            keyValue("fødselsnummer", rollback.fødselsnummer()),
            keyValue("forigePersonId", nyestePersonId)
        )
        finalize(person, message, rollback)
        publiserPersonRulletTilbake(rollback, message, vedtaksperioderFørRollback.map { it.id })
    }

    override fun behandle(message: OverstyrTidslinjeMessage, overstyrTidslinje: OverstyrTidslinje) {
        håndter(message, overstyrTidslinje) { person ->
            HendelseProbe.onOverstyrTidslinje()
            person.håndter(overstyrTidslinje)
        }
    }

    override fun behandle(message: EtterbetalingMessage, grunnbeløpsregulering: Grunnbeløpsregulering) {
        håndter(message, grunnbeløpsregulering) { person ->
            person.håndter(grunnbeløpsregulering)
        }
    }

    private fun publiserPersonRulletTilbake(
        rollback: PersonHendelse,
        message: HendelseMessage,
        vedtaksperioderSlettet: List<UUID>
    ) {
        rapidsConnection.publish(
            rollback.fødselsnummer(), JsonMessage.newMessage(
                mapOf(
                    "@event_name" to "person_rullet_tilbake",
                    "hendelseId" to message.id,
                    "fødselsnummer" to rollback.fødselsnummer(),
                    "vedtaksperioderSlettet" to vedtaksperioderSlettet
                )
            ).toJson()
        )
    }

    private fun sjekkForVedtaksperioderTilUtbetaling(vedtaksperioderEndretTilstand: List<VedtaksperiodeIdTilstand>) {
        require(vedtaksperioderEndretTilstand.none { it.tilstand in listOf("AVSLUTTET", "TIL_UTBETALING") }) {
            "Prøvde å rulle tilbake en person med en utbetalt periode"
        }
    }

    private fun <Hendelse : ArbeidstakerHendelse> håndter(
        message: HendelseMessage,
        hendelse: Hendelse,
        handler: (Person) -> Unit
    ) {
        person(message, hendelse).also {
            handler(it)
            finalize(it, message, hendelse)
        }
    }

    private fun person(message: HendelseMessage, hendelse: ArbeidstakerHendelse): Person {
        return initPerson(
            personRepository.hentPerson(hendelse.fødselsnummer()) ?: Person(
                aktørId = hendelse.aktørId(),
                fødselsnummer = hendelse.fødselsnummer()
            ), message, hendelse
        )
    }

    private fun personForId(id: Long, message: HendelseMessage, hendelse: PersonHendelse): Person {
        return initPerson(personRepository.hentPerson(id), message, hendelse)
    }

    private fun initPerson(person: Person, message: HendelseMessage, hendelse: PersonHendelse): Person {
        personMediator.observer(person, message, hendelse)
        person.addObserver(replayMediator)
        person.addObserver(VedtaksperiodeProbe)
        return person
    }

    private fun finalize(person: Person, message: HendelseMessage, hendelse: PersonHendelse) {
        lagrePersonDao.lagrePerson(message, person, hendelse)
        personMediator.finalize(message, hendelse)
        replayMediator.finalize()
        if (!hendelse.hasActivities()) return
        if (hendelse.hasErrorsOrWorse()) sikkerLogg.info("aktivitetslogg inneholder errors:\n${hendelse.toLogString()}")
        else sikkerLogg.info("aktivitetslogg inneholder meldinger:\n${hendelse.toLogString()}")
        behovMediator.håndter(message, hendelse)
    }

    private class PersonMediator(
        private val rapidsConnection: RapidsConnection,
        private val personRepository: PersonRepository
    ) {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
        private val meldinger = mutableListOf<Pair<String, String>>()

        fun observer(person: Person, message: HendelseMessage, hendelse: PersonHendelse) {
            person.addObserver(Observatør(message, hendelse))
        }

        fun finalize(message: HendelseMessage, hendelse: PersonHendelse) {
            if (meldinger.isEmpty()) return
            sikkerLogg.info("som følge av ${message.navn} id=${message.id} sendes ${meldinger.size} meldinger på rapid for fnr=${hendelse.fødselsnummer()}")
            meldinger.forEach { (fødselsnummer, melding) ->
                rapidsConnection.publish(fødselsnummer, melding.also { sikkerLogg.info("sender $it") })
            }
            meldinger.clear()
        }

        private fun queueMessage(fødselsnummer: String, message: String) {
            meldinger.add(fødselsnummer to message)
        }

        private inner class Observatør(
            private val message: HendelseMessage,
            private val hendelse: PersonHendelse
        ) : PersonObserver {
            private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

            override fun vedtaksperiodePåminnet(påminnelse: Påminnelse) {
                queueMessage(
                    "vedtaksperiode_påminnet", JsonMessage.newMessage(
                        mapOf(
                            "vedtaksperiodeId" to påminnelse.vedtaksperiodeId,
                            "tilstand" to påminnelse.tilstand(),
                            "antallGangerPåminnet" to påminnelse.antallGangerPåminnet(),
                            "tilstandsendringstidspunkt" to påminnelse.tilstandsendringstidspunkt(),
                            "påminnelsestidspunkt" to påminnelse.påminnelsestidspunkt(),
                            "nestePåminnelsestidspunkt" to påminnelse.nestePåminnelsestidspunkt()
                        )
                    )
                )
            }

            override fun vedtaksperiodeIkkePåminnet(påminnelse: Påminnelse, nåværendeTilstand: TilstandType) {
                queueMessage(
                    "vedtaksperiode_ikke_påminnet", JsonMessage.newMessage(
                        mapOf(
                            "vedtaksperiodeId" to påminnelse.vedtaksperiodeId,
                            "tilstand" to nåværendeTilstand
                        )
                    )
                )
            }

            override fun annullering(event: PersonObserver.UtbetalingAnnullertEvent) {
                queueMessage(
                    "utbetaling_annullert", JsonMessage.newMessage(
                        objectMapper.convertValue(event)
                    )
                )
            }

            override fun vedtaksperiodeEndret(event: PersonObserver.VedtaksperiodeEndretTilstandEvent) {
                queueMessage(
                    "vedtaksperiode_endret", JsonMessage.newMessage(
                        mapOf(
                            "vedtaksperiodeId" to event.vedtaksperiodeId,
                            "organisasjonsnummer" to event.organisasjonsnummer,
                            "gjeldendeTilstand" to event.gjeldendeTilstand,
                            "forrigeTilstand" to event.forrigeTilstand,
                            "aktivitetslogg" to event.aktivitetslogg.toMap(),
                            "vedtaksperiode_aktivitetslogg" to event.vedtaksperiodeaktivitetslogg.toMap(),
                            "hendelser" to event.hendelser,
                            "makstid" to event.makstid
                        )
                    )
                )
            }

            override fun vedtaksperiodeAvbrutt(event: PersonObserver.VedtaksperiodeAvbruttEvent) {
                queueMessage(
                    "vedtaksperiode_forkastet", JsonMessage.newMessage(
                        mapOf(
                            "vedtaksperiodeId" to event.vedtaksperiodeId,
                            "tilstand" to event.gjeldendeTilstand
                        )
                    )
                )
            }

            override fun vedtaksperiodeUtbetalt(event: PersonObserver.UtbetaltEvent) {
                queueMessage(
                    "utbetalt", JsonMessage.newMessage(
                        mapOf(
                            "aktørId" to event.aktørId,
                            "fødselsnummer" to event.fødselsnummer,
                            "organisasjonsnummer" to event.organisasjonsnummer,
                            "hendelser" to event.hendelser,
                            "utbetalt" to event.oppdrag.map { utbetalt ->
                                mapOf(
                                    "mottaker" to utbetalt.mottaker,
                                    "fagområde" to utbetalt.fagområde,
                                    "fagsystemId" to utbetalt.fagsystemId,
                                    "totalbeløp" to utbetalt.totalbeløp,
                                    "utbetalingslinjer" to utbetalt.utbetalingslinjer.map { linje ->
                                        mapOf(
                                            "fom" to linje.fom,
                                            "tom" to linje.tom,
                                            "dagsats" to linje.dagsats,
                                            "beløp" to linje.beløp,
                                            "grad" to linje.grad,
                                            "sykedager" to linje.sykedager
                                        )
                                    }
                                )
                            },
                            "ikkeUtbetalteDager" to event.ikkeUtbetalteDager.map {
                                mapOf(
                                    "dato" to it.dato,
                                    "type" to it.type.name
                                )
                            },
                            "fom" to event.fom,
                            "tom" to event.tom,
                            "forbrukteSykedager" to event.forbrukteSykedager,
                            "gjenståendeSykedager" to event.gjenståendeSykedager,
                            "godkjentAv" to event.godkjentAv,
                            "automatiskBehandling" to event.automatiskBehandling,
                            "opprettet" to event.opprettet,
                            "sykepengegrunnlag" to event.sykepengegrunnlag,
                            "månedsinntekt" to event.månedsinntekt,
                            "maksdato" to event.maksdato
                        )
                    )
                )
            }

            override fun vedtaksperiodeIkkeFunnet(vedtaksperiodeEvent: PersonObserver.VedtaksperiodeIkkeFunnetEvent) {
                queueMessage(
                    "vedtaksperiode_ikke_funnet", JsonMessage.newMessage(
                        mapOf(
                            "vedtaksperiodeId" to vedtaksperiodeEvent.vedtaksperiodeId
                        )
                    )
                )
            }

            override fun manglerInntektsmelding(event: PersonObserver.ManglendeInntektsmeldingEvent) {
                queueMessage(
                    "trenger_inntektsmelding", JsonMessage.newMessage(
                        mapOf(
                            "vedtaksperiodeId" to event.vedtaksperiodeId,
                            "fom" to event.fom,
                            "tom" to event.tom
                        )
                    )
                )
            }

            override fun trengerIkkeInntektsmelding(event: PersonObserver.TrengerIkkeInntektsmeldingEvent) {
                queueMessage(
                    "trenger_ikke_inntektsmelding", JsonMessage.newMessage(
                        mapOf(
                            "vedtaksperiodeId" to event.vedtaksperiodeId,
                            "fom" to event.fom,
                            "tom" to event.tom
                        )
                    )
                )
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
                if (hendelse is ArbeidstakerHendelse) {
                    this["organisasjonsnummer"] = hendelse.organisasjonsnummer()
                }
            }

            private fun queueMessage(event: String, outgoingMessage: JsonMessage) {
                queueMessage(hendelse.fødselsnummer(), leggPåStandardfelter(event, outgoingMessage).toJson())
            }
        }
    }
}

internal interface IHendelseMediator {
    fun behandle(message: HendelseMessage)
    fun behandle(message: NySøknadMessage, sykmelding: Sykmelding)
    fun behandle(message: SendtSøknadArbeidsgiverMessage, søknad: SøknadArbeidsgiver)
    fun behandle(message: SendtSøknadNavMessage, søknad: Søknad)
    fun behandle(message: InntektsmeldingMessage, inntektsmelding: Inntektsmelding)
    fun behandle(message: PåminnelseMessage, påminnelse: Påminnelse)
    fun behandle(message: UtbetalingshistorikkMessage, utbetalingshistorikk: Utbetalingshistorikk)
    fun behandle(message: YtelserMessage, ytelser: Ytelser)
    fun behandle(message: VilkårsgrunnlagMessage, vilkårsgrunnlag: Vilkårsgrunnlag)
    fun behandle(message: UtbetalingsgodkjenningMessage, utbetalingsgodkjenning: Utbetalingsgodkjenning)
    fun behandle(message: UtbetalingOverførtMessage, utbetaling: UtbetalingOverført)
    fun behandle(message: UtbetalingMessage, utbetaling: UtbetalingHendelse)
    fun behandle(message: SimuleringMessage, simulering: Simulering)
    fun behandle(message: AnnulleringMessage, annullerUtbetaling: AnnullerUtbetaling)
    fun behandle(message: RollbackMessage, rollback: Rollback)
    fun behandle(message: RollbackDeleteMessage, rollback: RollbackDelete)
    fun behandle(message: OverstyrTidslinjeMessage, overstyrTidslinje: OverstyrTidslinje)
    fun behandle(message: EtterbetalingMessage, grunnbeløpsregulering: Grunnbeløpsregulering)
}
