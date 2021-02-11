package no.nav.helse.spleis

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.person.*
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.db.LagrePersonDao
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class PersonMediator(private val person: Person, private val message: HendelseMessage, private val hendelse: PersonHendelse) {
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    private val meldinger = mutableListOf<Pair<String, String>>()
    private var vedtak = false

    init {
        person.addObserver(Observatør())
    }

    fun finalize(rapidsConnection: RapidsConnection, lagrePersonDao: LagrePersonDao) {
        lagrePersonDao.lagrePerson(message, person, hendelse, vedtak)
        if (meldinger.isEmpty()) return
        sikkerLogg.info("som følge av ${message.navn} id=${message.id} sendes ${meldinger.size} meldinger på rapid for fnr=${hendelse.fødselsnummer()}")
        meldinger.forEach { (fødselsnummer, melding) ->
            rapidsConnection.publish(fødselsnummer, melding.also { sikkerLogg.info("sender $it") })
        }
    }

    private fun queueMessage(fødselsnummer: String, message: String) {
        meldinger.add(fødselsnummer to message)
    }

    private inner class Observatør: PersonObserver {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        override fun vedtaksperiodePåminnet(vedtaksperiodeId: UUID, påminnelse: Påminnelse) {
            queueMessage("vedtaksperiode_påminnet", JsonMessage.newMessage(påminnelse.toMap()))
        }

        override fun vedtaksperiodeIkkePåminnet(påminnelse: Påminnelse, vedtaksperiodeId: UUID, nåværendeTilstand: TilstandType) {
            queueMessage(
                "vedtaksperiode_ikke_påminnet", JsonMessage.newMessage(
                    mapOf(
                        "vedtaksperiodeId" to vedtaksperiodeId,
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

        override fun utbetalingEndret(event: PersonObserver.UtbetalingEndretEvent) {
            queueMessage(
                "utbetaling_endret", JsonMessage.newMessage(
                    mapOf(
                        "utbetalingId" to event.utbetalingId,
                        "type" to event.type,
                        "forrigeStatus" to event.forrigeStatus,
                        "gjeldendeStatus" to event.gjeldendeStatus,
                        "arbeidsgiverOppdrag" to event.arbeidsgiverOppdrag,
                        "personOppdrag" to event.personOppdrag
                    )
                )
            )
        }

        override fun utbetalingUtbetalt(event: PersonObserver.UtbetalingUtbetaltEvent) {
            queueMessage(
                "utbetaling_utbetalt", JsonMessage.newMessage(
                    mapOf(
                        "utbetalingId" to event.utbetalingId,
                        "type" to event.type,
                        "fom" to event.fom,
                        "tom" to event.tom,
                        "maksdato" to event.maksdato,
                        "forbrukteSykedager" to event.forbrukteSykedager,
                        "gjenståendeSykedager" to event.gjenståendeSykedager,
                        "ident" to event.ident,
                        "epost" to event.epost,
                        "tidspunkt" to event.tidspunkt,
                        "automatiskBehandling" to event.automatiskBehandling,
                        "arbeidsgiverOppdrag" to event.arbeidsgiverOppdrag,
                        "personOppdrag" to event.personOppdrag
                    )
                )
            )
        }

        override fun vedtaksperiodeReberegnet(vedtaksperiodeId: UUID) {
            queueMessage(
                "vedtaksperiode_reberegnet", JsonMessage.newMessage(
                    mapOf(
                        "vedtaksperiodeId" to vedtaksperiodeId
                    )
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

        override fun vedtakFattet(
            vedtaksperiodeId: UUID,
            periode: Periode,
            hendelseIder: List<UUID>,
            skjæringstidspunkt: LocalDate,
            sykepengegrunnlag: Double,
            inntekt: Double,
            utbetalingId: UUID?
        ) {
            vedtak = true
            queueMessage("vedtak_fattet", JsonMessage.newMessage(
                mutableMapOf(
                    "vedtaksperiodeId" to vedtaksperiodeId,
                    "fom" to periode.start,
                    "tom" to periode.endInclusive,
                    "hendelser" to hendelseIder,
                    "skjæringstidspunkt" to skjæringstidspunkt,
                    "sykepengegrunnlag" to sykepengegrunnlag,
                    "inntekt" to inntekt
                ).apply {
                    if (utbetalingId != null) this["utbetalingId"] = utbetalingId
                }
            ))
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

        override fun avstemt(result: Map<String, Any>) {
            queueMessage("person_avstemt", JsonMessage.newMessage(result))
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

        override fun inntektsmeldingLagtPåKjøl(event: PersonObserver.InntektsmeldingLagtPåKjølEvent) {
            queueMessage(
                "inntektsmelding_lagt_på_kjøl", JsonMessage.newMessage(
                    mapOf(
                        "hendelseId" to event.hendelseId,
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
