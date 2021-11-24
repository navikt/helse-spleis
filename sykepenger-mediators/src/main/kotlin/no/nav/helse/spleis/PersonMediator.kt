package no.nav.helse.spleis

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.Fødselsnummer
import no.nav.helse.hendelser.Hendelseskontekst
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.person.Person
import no.nav.helse.person.PersonHendelse
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.TilstandType
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asOptionalLocalDate
import no.nav.helse.spleis.db.HendelseRepository
import no.nav.helse.spleis.db.LagrePersonDao
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class PersonMediator(
    private val person: Person,
    private val message: HendelseMessage,
    private val hendelse: PersonHendelse,
    private val hendelseRepository: HendelseRepository
) : PersonObserver {
    private val meldinger = mutableListOf<Pakke>()
    private val replays = mutableListOf<String>()
    private var vedtak = false

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
        private val logg = LoggerFactory.getLogger(PersonMediator::class.java)
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }


    init {
        person.addObserver(this)
    }

    fun finalize(rapidsConnection: RapidsConnection, lagrePersonDao: LagrePersonDao) {
        lagrePersonDao.lagrePerson(message, person, hendelse, vedtak)
        sendUtgåendeMeldinger(rapidsConnection)
        sendReplays(rapidsConnection)
    }

    private fun sendUtgåendeMeldinger(rapidsConnection: RapidsConnection) {
        if (meldinger.isEmpty()) return
        message.logOutgoingMessages(sikkerLogg, meldinger.size)
        meldinger.forEach { pakke -> pakke.publish(rapidsConnection) }
    }

    private fun sendReplays(rapidsConnection: RapidsConnection) {
        if (replays.isEmpty()) return
        message.logReplays(sikkerLogg, replays.size)
        replays.forEach { melding ->
            rapidsConnection.queueReplayMessage(hendelse.fødselsnummer(), melding)
        }
    }

    private fun queueMessage(fødselsnummer: String, eventName: String, message: String) {
        meldinger.add(Pakke(fødselsnummer, eventName, message))
    }

    override fun inntektsmeldingReplay(fødselsnummer: Fødselsnummer, vedtaksperiodeId: UUID) {
        hendelseRepository.finnInntektsmeldinger(fødselsnummer).forEach { inntektsmelding ->
            createReplayMessage(inntektsmelding, mapOf(
                "@event_name" to "inntektsmelding_replay",
                "vedtaksperiodeId" to vedtaksperiodeId
            ))
        }
    }

    private fun createReplayMessage(message: JsonNode, extraFields: Map<String, Any>) {
        message as ObjectNode
        message.put("@replay", true)
        extraFields.forEach { (key, value), -> message.replace(key, objectMapper.convertValue<JsonNode>(value)) }
        replays.add(message.toString())
    }

    override fun vedtaksperiodePåminnet(hendelseskontekst: Hendelseskontekst, påminnelse: Påminnelse) {
        queueMessage(hendelseskontekst, "vedtaksperiode_påminnet", JsonMessage.newMessage(påminnelse.toOutgoingMessage()))
    }

    override fun vedtaksperiodeIkkePåminnet(hendelseskontekst: Hendelseskontekst, nåværendeTilstand: TilstandType) {
        queueMessage(
            hendelseskontekst,
            "vedtaksperiode_ikke_påminnet", JsonMessage.newMessage(
                mapOf(
                    "tilstand" to nåværendeTilstand
                )
            )
        )
    }

    override fun annullering(hendelseskontekst: Hendelseskontekst, event: PersonObserver.UtbetalingAnnullertEvent) {
        queueMessage(
            hendelseskontekst,
            "utbetaling_annullert", JsonMessage.newMessage(
                mutableMapOf(
                    "utbetalingId" to event.utbetalingId,
                    "korrelasjonsId" to event.korrelasjonsId,
                    "fom" to event.fom,
                    "tom" to event.tom,
                    "annullertAvSaksbehandler" to event.annullertAvSaksbehandler,
                    "tidspunkt" to event.annullertAvSaksbehandler,
                    "saksbehandlerEpost" to event.saksbehandlerEpost,
                    "epost" to event.saksbehandlerEpost,
                    "saksbehandlerIdent" to event.saksbehandlerIdent,
                    "ident" to event.saksbehandlerIdent,
                    "utbetalingslinjer" to event.utbetalingslinjer.map { mapOf(
                        "fom" to it.fom,
                        "tom" to it.tom,
                        "grad" to it.grad,
                        "beløp" to it.beløp
                    )}
                ).apply {
                    event.arbeidsgiverFagsystemId?.also {
                        this["fagsystemId"] = it
                        this["arbeidsgiverFagsystemId"] = it
                    }
                    event.personFagsystemId?.also {
                        this["personFagsystemId"] = it
                    }
                }
            )
        )
    }

    override fun utbetalingEndret(hendelseskontekst: Hendelseskontekst, event: PersonObserver.UtbetalingEndretEvent) {
        queueMessage(
            hendelseskontekst,
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

    override fun utbetalingUtbetalt(hendelseskontekst: Hendelseskontekst, event: PersonObserver.UtbetalingUtbetaltEvent) {
        queueMessage(hendelseskontekst, "utbetaling_utbetalt", utbetalingAvsluttet(event))
    }

    override fun utbetalingUtenUtbetaling(hendelseskontekst: Hendelseskontekst, event: PersonObserver.UtbetalingUtbetaltEvent) {
        queueMessage(hendelseskontekst, "utbetaling_uten_utbetaling", utbetalingAvsluttet(event))
    }

    private fun utbetalingAvsluttet(event: PersonObserver.UtbetalingUtbetaltEvent) =
        JsonMessage.newMessage(mapOf(
            "utbetalingId" to event.utbetalingId,
            "korrelasjonsId" to event.korrelasjonsId,
            "type" to event.type,
            "fom" to event.fom,
            "tom" to event.tom,
            "maksdato" to event.maksdato,
            "forbrukteSykedager" to event.forbrukteSykedager,
            "gjenståendeSykedager" to event.gjenståendeSykedager,
            "stønadsdager" to event.stønadsdager,
            "ident" to event.ident,
            "epost" to event.epost,
            "tidspunkt" to event.tidspunkt,
            "automatiskBehandling" to event.automatiskBehandling,
            "arbeidsgiverOppdrag" to event.arbeidsgiverOppdrag,
            "personOppdrag" to event.personOppdrag,
            "utbetalingsdager" to event.utbetalingsdager,
            "vedtaksperiodeIder" to event.vedtaksperiodeIder
        ))

    override fun feriepengerUtbetalt(hendelseskontekst: Hendelseskontekst, event: PersonObserver.FeriepengerUtbetaltEvent) =
        queueMessage(
            hendelseskontekst,
            "feriepenger_utbetalt", JsonMessage.newMessage(
                mapOf(
                    "arbeidsgiverOppdrag" to event.arbeidsgiverOppdrag,
                    "personOppdrag" to event.personOppdrag,
                )
            )
        )

    override fun vedtaksperiodeReberegnet(hendelseskontekst: Hendelseskontekst) {
        queueMessage(
            hendelseskontekst,
            "vedtaksperiode_reberegnet", JsonMessage.newMessage()
        )
    }

    override fun vedtaksperiodeEndret(hendelseskontekst: Hendelseskontekst, event: PersonObserver.VedtaksperiodeEndretEvent) {
        queueMessage(
            hendelseskontekst,
            "vedtaksperiode_endret", JsonMessage.newMessage(
                mapOf(
                    "gjeldendeTilstand" to event.gjeldendeTilstand,
                    "forrigeTilstand" to event.forrigeTilstand,
                    "aktivitetslogg" to event.aktivitetslogg.toMap(),
                    "harVedtaksperiodeWarnings" to event.harVedtaksperiodeWarnings,
                    "hendelser" to event.hendelser,
                    "makstid" to event.makstid
                )
            )
        )
    }

    override fun vedtaksperiodeAvbrutt(hendelseskontekst: Hendelseskontekst, event: PersonObserver.VedtaksperiodeAvbruttEvent) {
        queueMessage(
            hendelseskontekst,
            "vedtaksperiode_forkastet", JsonMessage.newMessage(
                mapOf(
                    "tilstand" to event.gjeldendeTilstand
                )
            )
        )
    }

    override fun vedtakFattet(
        hendelseskontekst: Hendelseskontekst,
        event: PersonObserver.VedtakFattetEvent
    ) {
        vedtak = true
        queueMessage(
            hendelseskontekst,
            "vedtak_fattet", JsonMessage.newMessage(
            mutableMapOf(
                "fom" to event.periode.start,
                "tom" to event.periode.endInclusive,
                "hendelser" to event.hendelseIder,
                "skjæringstidspunkt" to event.skjæringstidspunkt,
                "sykepengegrunnlag" to event.sykepengegrunnlag,
                "grunnlagForSykepengegrunnlag" to event.grunnlagForSykepengegrunnlag,
                "grunnlagForSykepengegrunnlagPerArbeidsgiver" to event.grunnlagForSykepengegrunnlagPerArbeidsgiver,
                "begrensning" to event.sykepengegrunnlagsbegrensning,
                "inntekt" to event.inntekt,
                "vedtakFattetTidspunkt" to event.vedtakFattetTidspunkt
            ).apply {
                event.utbetalingId?.let { this["utbetalingId"] = it }
            }
        ))
    }

    override fun revurderingAvvist(hendelseskontekst: Hendelseskontekst, event: PersonObserver.RevurderingAvvistEvent) {
        queueMessage(hendelseskontekst,
           "revurdering_avvist",
            JsonMessage.newMessage(event.toJsonMap())
        )
    }

    override fun vedtaksperiodeUtbetalt(hendelseskontekst: Hendelseskontekst, event: PersonObserver.UtbetaltEvent) {
        queueMessage(
            hendelseskontekst,
            "utbetalt", JsonMessage.newMessage(
                mapOf(
                    "utbetalingId" to event.utbetalingId,
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
                                    "sats" to linje.sats,
                                    "dagsats" to linje.sats,
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

    override fun avstemt(hendelseskontekst: Hendelseskontekst, result: Map<String, Any>) {
        queueMessage(hendelseskontekst, "person_avstemt", JsonMessage.newMessage(result))
    }

    override fun vedtaksperiodeIkkeFunnet(hendelseskontekst: Hendelseskontekst, vedtaksperiodeEvent: PersonObserver.VedtaksperiodeIkkeFunnetEvent) {
        queueMessage(
            hendelseskontekst,
            "vedtaksperiode_ikke_funnet", JsonMessage.newMessage(
                mapOf(
                    "vedtaksperiodeId" to vedtaksperiodeEvent.vedtaksperiodeId
                )
            )
        )
    }

    override fun manglerInntektsmelding(hendelseskontekst: Hendelseskontekst, event: PersonObserver.ManglendeInntektsmeldingEvent) {
        if (skalIkkeMasePåArbeidsgiver(hendelse.fødselsnummer(), event.fom, event.tom)) {
            hendelse.info("Hindrer publisering av trenger_inntektsmelding - har allerede fått inntektsmelding, men perioden er mistolket som gap")
            return
        }

        queueMessage(
            hendelseskontekst,
            "trenger_inntektsmelding", JsonMessage.newMessage(
                mapOf(
                    "fom" to event.fom,
                    "tom" to event.tom
                )
            )
        )
    }

    override fun trengerIkkeInntektsmelding(hendelseskontekst: Hendelseskontekst, event: PersonObserver.TrengerIkkeInntektsmeldingEvent) {
        queueMessage(
            hendelseskontekst,
            "trenger_ikke_inntektsmelding", JsonMessage.newMessage(
                mapOf(
                    "fom" to event.fom,
                    "tom" to event.tom
                )
            )
        )
    }

    override fun hendelseIkkeHåndtert(hendelseskontekst: Hendelseskontekst, event: PersonObserver.HendelseIkkeHåndtertEvent) {
        queueMessage(hendelseskontekst, "hendelse_ikke_håndtert", JsonMessage.newMessage(
            mapOf(
                "hendelseId" to event.hendelseId,
                "årsaker" to event.årsaker
            )
        ))
    }

    private fun leggPåStandardfelter(hendelseskontekst: Hendelseskontekst, eventName: String, outgoingMessage: JsonMessage) = outgoingMessage.apply {
        this["@event_name"] = eventName
        this["@id"] = UUID.randomUUID()
        this["@opprettet"] = LocalDateTime.now()
        this["@forårsaket_av"] = message.tracinginfo()
        hendelseskontekst.appendTo(this::set)
    }

    private fun queueMessage(hendelseskontekst: Hendelseskontekst, eventName: String, outgoingMessage: JsonMessage) {
        loggHvisTomHendelseliste(outgoingMessage)
        queueMessage(hendelse.fødselsnummer(), eventName, leggPåStandardfelter(hendelseskontekst, eventName, outgoingMessage).toJson())
    }

    private fun loggHvisTomHendelseliste(outgoingMessage: JsonMessage) {
        objectMapper.readTree(outgoingMessage.toJson()).let {
            if (it.path("hendelser").isArray && it["hendelser"].isEmpty) {
                logg.warn("Det blir sendt ut et event med tom hendelseliste - ooouups")
            }
        }
    }

    private fun skalIkkeMasePåArbeidsgiver(fnr: String, fom: LocalDate, tom: LocalDate): Boolean {
        val gjeldendePeriode = hendelseRepository.finnSøknader(Fødselsnummer.tilFødselsnummer(fnr))
            .flatMap { søknad -> Periode(søknad["fom"].asLocalDate(), søknad["tom"].asLocalDate()) }
            .grupperSammenhengendePerioder()
            .firstOrNull { it.contains(fom) || it.contains(tom) }

        return hendelseRepository
            .finnInntektsmeldinger(Fødselsnummer.tilFødselsnummer(fnr))
            .map { inntektsmelding ->
                inntektsmelding["foersteFravaersdag"].asOptionalLocalDate() to
                    if (inntektsmelding["arbeidsgiverperioder"].isEmpty) Periode(LocalDate.MIN, LocalDate.MIN)
                    else Periode(
                        inntektsmelding["arbeidsgiverperioder"].minOf { it["fom"].asLocalDate() },
                        inntektsmelding["arbeidsgiverperioder"].maxOf { it["tom"].asLocalDate() }
                    )
            }
            .any { (førsteFraværsdag, sammenslåttArbeidsgiverperiode) -> gjeldendePeriode?.fårInntektFra(førsteFraværsdag, sammenslåttArbeidsgiverperiode) ?: false }
    }

    private fun Periode.fårInntektFra(førsteFraværsdag: LocalDate?, arbeidsgiverperiode: Periode): Boolean {
        if (førsteFraværsdag.erEtterPeriode(arbeidsgiverperiode)) return førsteFraværsdag in this
        return this.overlapperMed(arbeidsgiverperiode)
    }

    private fun LocalDate?.erEtterPeriode (periode: Periode) = this?.let { periode.endInclusive < this } ?: false

    private data class Pakke (
        private val fødselsnummer: String,
        private val eventName: String,
        private val blob: String,
    ) {
        fun publish(rapidsConnection: RapidsConnection) {
            rapidsConnection.publish(fødselsnummer, blob.also { sikkerLogg.info("sender $eventName: $it") })
        }
    }

}
