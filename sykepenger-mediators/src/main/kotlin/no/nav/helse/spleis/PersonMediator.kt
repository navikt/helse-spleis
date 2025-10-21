package no.nav.helse.spleis

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.OutgoingMessage
import java.util.UUID
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.PersonObserver.Arbeidsgiverperiode
import no.nav.helse.person.PersonObserver.Inntekt
import no.nav.helse.person.PersonObserver.Refusjon
import no.nav.helse.person.PersonObserver.Utbetalingsdag.Dagtype
import no.nav.helse.person.PersonObserver.Utbetalingsdag.EksternBegrunnelseDTO
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import org.slf4j.LoggerFactory

internal class PersonMediator(
    private val message: HendelseMessage
) : PersonObserver {
    private val meldinger = mutableListOf<Pakke>()

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    fun ferdigstill(context: MessageContext) {
        sendUtgåendeMeldinger(context)
    }

    private fun sendUtgåendeMeldinger(context: MessageContext) {
        if (meldinger.isEmpty()) return
        message.logOutgoingMessages(sikkerLogg, meldinger.size)
        val outgoingMessages = meldinger.map { it.tilUtgåendeMelding() }
        val (ok, failed) = context.publish(outgoingMessages)

        if (failed.isEmpty()) return
        val førsteFeil = failed.first().error
        val feilmelding = "Feil ved sending av ${failed.size} melding(er), ${ok.size} melding(er) gikk ok!\n" +
            "Disse meldingene feilet:\n" +
            failed.joinToString(separator = "\n") { "#${it.index}: ${it.error.message}\n\t${it.message}" }
        throw RuntimeException(feilmelding, førsteFeil)
    }

    private fun queueMessage(fødselsnummer: String, message: String) {
        val eventName = objectMapper.readTree(message).path("@event_name").asText()
        meldinger.add(Pakke(fødselsnummer, eventName, message))
    }

    private val Behandlingsporing.Yrkesaktivitet.somOrganisasjonsnummer
        get() = when (this) {
            Behandlingsporing.Yrkesaktivitet.Arbeidsledig -> "ARBEIDSLEDIG"
            is Behandlingsporing.Yrkesaktivitet.Arbeidstaker -> organisasjonsnummer
            Behandlingsporing.Yrkesaktivitet.Frilans -> "FRILANS"
            Behandlingsporing.Yrkesaktivitet.Selvstendig -> "SELVSTENDIG"
        }

    private val Behandlingsporing.Yrkesaktivitet.somYrkesaktivitetstype
        get() = when (this) {
            Behandlingsporing.Yrkesaktivitet.Arbeidsledig -> "ARBEIDSLEDIG"
            is Behandlingsporing.Yrkesaktivitet.Arbeidstaker -> "ARBEIDSTAKER"
            Behandlingsporing.Yrkesaktivitet.Frilans -> "FRILANS"
            Behandlingsporing.Yrkesaktivitet.Selvstendig -> "SELVSTENDIG"
        }

    override fun inntektsmeldingFørSøknad(event: PersonObserver.InntektsmeldingFørSøknadEvent) {
        queueMessage(
            JsonMessage.newMessage(
                "inntektsmelding_før_søknad",
                mapOf(
                    "inntektsmeldingId" to event.inntektsmeldingId,
                    "organisasjonsnummer" to event.yrkesaktivitetssporing.somOrganisasjonsnummer,
                    "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype
                )
            )
        )
    }

    override fun inntektsmeldingIkkeHåndtert(inntektsmeldingId: UUID, organisasjonsnummer: String, speilrelatert: Boolean) {
        queueMessage(
            JsonMessage.newMessage(
                "inntektsmelding_ikke_håndtert",
                mapOf(
                    "inntektsmeldingId" to inntektsmeldingId,
                    "organisasjonsnummer" to organisasjonsnummer,
                    "speilrelatert" to speilrelatert
                )
            )
        )
    }

    override fun inntektsmeldingHåndtert(inntektsmeldingId: UUID, vedtaksperiodeId: UUID, organisasjonsnummer: String) {
        queueMessage(
            JsonMessage.newMessage(
                "inntektsmelding_håndtert", mapOf(
                "inntektsmeldingId" to inntektsmeldingId,
                "organisasjonsnummer" to organisasjonsnummer,
                "vedtaksperiodeId" to vedtaksperiodeId
            )
            )
        )
    }

    override fun søknadHåndtert(søknadId: UUID, vedtaksperiodeId: UUID, organisasjonsnummer: String) {
        queueMessage(
            JsonMessage.newMessage(
                "søknad_håndtert", mapOf(
                "søknadId" to søknadId,
                "organisasjonsnummer" to organisasjonsnummer,
                "vedtaksperiodeId" to vedtaksperiodeId
            )
            )
        )
    }

    override fun vedtaksperiodeAnnullert(vedtaksperiodeAnnullertEvent: PersonObserver.VedtaksperiodeAnnullertEvent) {
        queueMessage(
            JsonMessage.newMessage(
                "vedtaksperiode_annullert",
                mapOf(
                    "fom" to vedtaksperiodeAnnullertEvent.fom,
                    "tom" to vedtaksperiodeAnnullertEvent.tom,
                    "vedtaksperiodeId" to vedtaksperiodeAnnullertEvent.vedtaksperiodeId,
                    "behandlingId" to vedtaksperiodeAnnullertEvent.behandlingId,
                    "organisasjonsnummer" to vedtaksperiodeAnnullertEvent.yrkesaktivitetssporing.somOrganisasjonsnummer,
                    "yrkesaktivitetstype" to vedtaksperiodeAnnullertEvent.yrkesaktivitetssporing.somYrkesaktivitetstype
                )
            )
        )
    }

    override fun overlappendeInfotrygdperioder(event: PersonObserver.OverlappendeInfotrygdperioder) {
        queueMessage(
            JsonMessage.newMessage(
                "overlappende_infotrygdperioder",
                mutableMapOf(
                    "infotrygdhistorikkHendelseId" to event.infotrygdhistorikkHendelseId,
                    "vedtaksperioder" to event.overlappendeInfotrygdperioder.map {
                        mapOf(
                            "organisasjonsnummer" to it.yrkesaktivitetssporing.somOrganisasjonsnummer,
                            "yrkesaktivitetstype" to it.yrkesaktivitetssporing.somYrkesaktivitetstype,
                            "vedtaksperiodeId" to it.vedtaksperiodeId,
                            "vedtaksperiodeFom" to it.vedtaksperiodeFom,
                            "vedtaksperiodeTom" to it.vedtaksperiodeTom,
                            "vedtaksperiodetilstand" to it.vedtaksperiodetilstand,
                            "kanForkastes" to it.kanForkastes,
                            "infotrygdperioder" to it.infotrygdperioder.map { infotrygdperiode ->
                                mapOf(
                                    "fom" to infotrygdperiode.fom,
                                    "tom" to infotrygdperiode.tom,
                                    "type" to infotrygdperiode.type,
                                    "organisasjonsnummer" to infotrygdperiode.orgnummer
                                )
                            }
                        )
                    },
                )
            )
        )
    }

    override fun vedtaksperiodePåminnet(vedtaksperiodeId: UUID, organisasjonsnummer: String, påminnelse: Påminnelse) {
        queueMessage(
            JsonMessage.newMessage(
                "vedtaksperiode_påminnet",
                mapOf(
                    "organisasjonsnummer" to påminnelse.behandlingsporing.somOrganisasjonsnummer,
                    "yrkesaktivitetstype" to påminnelse.behandlingsporing.somYrkesaktivitetstype,
                    "vedtaksperiodeId" to påminnelse.vedtaksperiodeId,
                    "tilstand" to påminnelse.tilstand,
                    "antallGangerPåminnet" to påminnelse.antallGangerPåminnet,
                    "tilstandsendringstidspunkt" to påminnelse.tilstandsendringstidspunkt,
                    "påminnelsestidspunkt" to påminnelse.påminnelsestidspunkt,
                    "nestePåminnelsestidspunkt" to påminnelse.nestePåminnelsestidspunkt
                )
            )
        )
    }

    override fun vedtaksperiodeIkkePåminnet(vedtaksperiodeId: UUID, organisasjonsnummer: String, nåværendeTilstand: TilstandType) {
        queueMessage(
            JsonMessage.newMessage(
                "vedtaksperiode_ikke_påminnet", mapOf(
                "organisasjonsnummer" to organisasjonsnummer,
                "vedtaksperiodeId" to vedtaksperiodeId,
                "tilstand" to nåværendeTilstand
            )
            )
        )
    }

    override fun annullering(event: PersonObserver.UtbetalingAnnullertEvent) {
        queueMessage(
            JsonMessage.newMessage(
                "utbetaling_annullert",
                mutableMapOf(
                    "organisasjonsnummer" to event.yrkesaktivitetssporing.somOrganisasjonsnummer,
                    "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
                    "utbetalingId" to event.utbetalingId,
                    "korrelasjonsId" to event.korrelasjonsId,
                    "fom" to event.fom,
                    "tom" to event.tom,
                    "tidspunkt" to event.annullertAvSaksbehandler,
                    "epost" to event.saksbehandlerEpost,
                    "ident" to event.saksbehandlerIdent,
                    "arbeidsgiverFagsystemId" to event.arbeidsgiverFagsystemId,
                    "personFagsystemId" to event.personFagsystemId
                )
            )
        )
    }

    override fun planlagtAnnullering(event: PersonObserver.PlanlagtAnnulleringEvent) {
        queueMessage(
            JsonMessage.newMessage(
                "planlagt_annullering",
                buildMap {
                    put("yrkesaktivitet", event.yrkesaktivitet) // TODO delete
                    put("yrkesaktivitetstype", event.yrkesaktivitetssporing.somYrkesaktivitetstype)
                    compute("organisasjonsnummer") { _, _ ->
                        (event.yrkesaktivitetssporing as? Behandlingsporing.Yrkesaktivitet.Arbeidstaker)?.organisasjonsnummer
                    }
                    put("vedtaksperioder", event.vedtaksperioder)
                    put("fom", event.fom)
                    put("tom", event.tom)
                    put("ident", event.saksbehandlerIdent)
                    put("årsaker", event.årsaker)
                    put("begrunnelse", event.begrunnelse)
                }
            )
        )
    }

    override fun utbetalingEndret(event: PersonObserver.UtbetalingEndretEvent) {
        queueMessage(
            JsonMessage.newMessage(
                "utbetaling_endret",
                mapOf(
                    "organisasjonsnummer" to event.yrkesaktivitetssporing.somOrganisasjonsnummer,
                    "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
                    "utbetalingId" to event.utbetalingId,
                    "type" to event.type,
                    "forrigeStatus" to event.forrigeStatus,
                    "gjeldendeStatus" to event.gjeldendeStatus,
                    "arbeidsgiverOppdrag" to event.arbeidsgiverOppdrag.tilJsonMap(),
                    "personOppdrag" to event.personOppdrag.tilJsonMap(),
                    "korrelasjonsId" to event.korrelasjonsId
                )
            )
        )
    }

    private fun PersonObserver.UtbetalingEndretEvent.OppdragEventDetaljer.tilJsonMap() =
        mapOf(
            "fagsystemId" to this.fagsystemId,
            "mottaker" to this.mottaker,
            "nettoBeløp" to this.nettoBeløp,
            "linjer" to this.linjer.map {
                it.tilJsonMap()
            }
        )

    private fun PersonObserver.UtbetalingEndretEvent.OppdragEventDetaljer.OppdragEventLinjeDetaljer.tilJsonMap() =
        mapOf(
            "fom" to this.fom,
            "tom" to this.tom,
            "totalbeløp" to this.totalbeløp
        )

    override fun nyVedtaksperiodeUtbetaling(
        organisasjonsnummer: String,
        utbetalingId: UUID,
        vedtaksperiodeId: UUID
    ) {
        queueMessage(
            JsonMessage.newMessage(
                "vedtaksperiode_ny_utbetaling", mapOf(
                "organisasjonsnummer" to organisasjonsnummer,
                "vedtaksperiodeId" to vedtaksperiodeId,
                "utbetalingId" to utbetalingId
            )
            )
        )
    }

    override fun overstyringIgangsatt(event: PersonObserver.OverstyringIgangsatt) {
        queueMessage(
            JsonMessage.newMessage(
                "overstyring_igangsatt", mapOf(
                "revurderingId" to UUID.randomUUID(),
                "kilde" to message.meldingsporing.id,
                "skjæringstidspunkt" to event.skjæringstidspunkt,
                "periodeForEndringFom" to event.periodeForEndring.start,
                "periodeForEndringTom" to event.periodeForEndring.endInclusive,
                "årsak" to event.årsak,
                "typeEndring" to event.typeEndring,
                "berørtePerioder" to event.berørtePerioder.map {
                    mapOf(
                        "vedtaksperiodeId" to it.vedtaksperiodeId,
                        "skjæringstidspunkt" to it.skjæringstidspunkt,
                        "periodeFom" to it.periode.start,
                        "periodeTom" to it.periode.endInclusive,
                        "orgnummer" to it.yrkesaktivitetssporing.somOrganisasjonsnummer,
                        "yrkesaktivitetstype" to it.yrkesaktivitetssporing.somYrkesaktivitetstype,
                        "typeEndring" to it.typeEndring,
                    )
                }
            )))
    }

    override fun utbetalingUtbetalt(event: PersonObserver.UtbetalingUtbetaltEvent) {
        queueMessage(utbetalingAvsluttet("utbetaling_utbetalt", event))
    }

    override fun utbetalingUtenUtbetaling(event: PersonObserver.UtbetalingUtbetaltEvent) {
        queueMessage(utbetalingAvsluttet("utbetaling_uten_utbetaling", event))
    }

    private fun utbetalingAvsluttet(eventName: String, event: PersonObserver.UtbetalingUtbetaltEvent) =
        JsonMessage.newMessage(
            eventName,
            mapOf(
                "organisasjonsnummer" to event.yrkesaktivitetssporing.somOrganisasjonsnummer,
                "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
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
                "arbeidsgiverOppdrag" to event.arbeidsgiverOppdrag.tilJsonMap(),
                "personOppdrag" to event.personOppdrag.tilJsonMap(),
                "utbetalingsdager" to event.utbetalingsdager.map {
                    it.tilJsonMap()
                }
            )
        )

    private fun PersonObserver.UtbetalingUtbetaltEvent.OppdragEventDetaljer.tilJsonMap() =
        mapOf(
            "fagsystemId" to this.fagsystemId,
            "fagområde" to this.fagområde,
            "mottaker" to this.mottaker,
            "nettoBeløp" to this.nettoBeløp,
            "stønadsdager" to this.stønadsdager,
            "fom" to this.fom,
            "tom" to this.tom,
            "linjer" to this.linjer.map {
                it.tilJsonMap()
            }
        )

    private fun PersonObserver.UtbetalingUtbetaltEvent.OppdragEventDetaljer.OppdragEventLinjeDetaljer.tilJsonMap() =
        mapOf(
            "fom" to this.fom,
            "tom" to this.tom,
            "sats" to this.sats,
            "grad" to this.grad,
            "stønadsdager" to this.stønadsdager,
            "totalbeløp" to this.totalbeløp,
            "statuskode" to this.statuskode,
        )

    private fun PersonObserver.Utbetalingsdag.tilJsonMap() =
        mapOf(
            "dato" to this.dato,
            "type" to when (this.type) {
                Dagtype.ArbeidsgiverperiodeDag -> "ArbeidsgiverperiodeDag"
                Dagtype.NavDag -> "NavDag"
                Dagtype.NavHelgDag -> "NavHelgDag"
                Dagtype.Arbeidsdag -> "Arbeidsdag"
                Dagtype.Fridag -> "Fridag"
                Dagtype.AvvistDag -> "AvvistDag"
                Dagtype.UkjentDag -> "UkjentDag"
                Dagtype.ForeldetDag -> "ForeldetDag"
                Dagtype.Permisjonsdag -> "Permisjonsdag"
                Dagtype.Feriedag -> "Feriedag"
                Dagtype.ArbeidIkkeGjenopptattDag -> "ArbeidIkkeGjenopptattDag"
                Dagtype.AndreYtelser -> "AndreYtelser"
                Dagtype.Ventetidsdag -> "Ventetidsdag"
            },
            "beløpTilArbeidsgiver" to this.beløpTilArbeidsgiver,
            "beløpTilBruker" to this.beløpTilBruker,
            "sykdomsgrad" to this.sykdomsgrad,
            "begrunnelser" to this.begrunnelser?.map {
                when (it) {
                    EksternBegrunnelseDTO.SykepengedagerOppbrukt -> "SykepengedagerOppbrukt"
                    EksternBegrunnelseDTO.SykepengedagerOppbruktOver67 -> "SykepengedagerOppbruktOver67"
                    EksternBegrunnelseDTO.MinimumInntekt -> "MinimumInntekt"
                    EksternBegrunnelseDTO.MinimumInntektOver67 -> "MinimumInntektOver67"
                    EksternBegrunnelseDTO.EgenmeldingUtenforArbeidsgiverperiode -> "EgenmeldingUtenforArbeidsgiverperiode"
                    EksternBegrunnelseDTO.AndreYtelserAap -> "AndreYtelserAap"
                    EksternBegrunnelseDTO.AndreYtelserDagpenger -> "AndreYtelserDagpenger"
                    EksternBegrunnelseDTO.AndreYtelserForeldrepenger -> "AndreYtelserForeldrepenger"
                    EksternBegrunnelseDTO.AndreYtelserOmsorgspenger -> "AndreYtelserOmsorgspenger"
                    EksternBegrunnelseDTO.AndreYtelserOpplaringspenger -> "AndreYtelserOpplaringspenger"
                    EksternBegrunnelseDTO.AndreYtelserPleiepenger -> "AndreYtelserPleiepenger"
                    EksternBegrunnelseDTO.AndreYtelserSvangerskapspenger -> "AndreYtelserSvangerskapspenger"
                    EksternBegrunnelseDTO.MinimumSykdomsgrad -> "MinimumSykdomsgrad"
                    EksternBegrunnelseDTO.EtterDødsdato -> "EtterDødsdato"
                    EksternBegrunnelseDTO.ManglerMedlemskap -> "ManglerMedlemskap"
                    EksternBegrunnelseDTO.ManglerOpptjening -> "ManglerOpptjening"
                    EksternBegrunnelseDTO.Over70 -> "Over70"
                }
            }
        )

    override fun feriepengerUtbetalt(event: PersonObserver.FeriepengerUtbetaltEvent) =
        queueMessage(
            JsonMessage.newMessage(
                "feriepenger_utbetalt",
                mapOf(
                    "organisasjonsnummer" to event.yrkesaktivitetssporing.somOrganisasjonsnummer,
                    "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
                    "fom" to event.fom,
                    "tom" to event.tom,
                    "arbeidsgiverOppdrag" to event.arbeidsgiverOppdrag.tilJsonMap(),
                    "personOppdrag" to event.personOppdrag.tilJsonMap()
                )
            )
        )

    private fun PersonObserver.FeriepengerUtbetaltEvent.FeriepengeoppdragEventDetaljer.tilJsonMap() =
        mapOf(
            "fagsystemId" to this.fagsystemId,
            "mottaker" to this.mottaker,
            "totalbeløp" to this.totalbeløp
        )

    override fun vedtaksperiodeEndret(event: PersonObserver.VedtaksperiodeEndretEvent) {
        queueMessage(
            JsonMessage.newMessage(
                "vedtaksperiode_endret",
                mapOf(
                    "organisasjonsnummer" to event.yrkesaktivitetssporing.somOrganisasjonsnummer,
                    "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
                    "vedtaksperiodeId" to event.vedtaksperiodeId,
                    "behandlingId" to event.behandlingId,
                    "gjeldendeTilstand" to event.gjeldendeTilstand,
                    "forrigeTilstand" to event.forrigeTilstand,
                    "hendelser" to event.hendelser,
                    "makstid" to event.makstid,
                    "fom" to event.fom,
                    "tom" to event.tom,
                    "skjæringstidspunkt" to event.skjæringstidspunkt
                )
            )
        )
    }

    override fun vedtaksperioderVenter(eventer: List<PersonObserver.VedtaksperiodeVenterEvent>) {
        queueMessage(
            JsonMessage.newMessage(
                "vedtaksperioder_venter", mapOf(
                "vedtaksperioder" to eventer.map { event ->
                    mapOf(
                        "organisasjonsnummer" to event.yrkesaktivitetssporing.somOrganisasjonsnummer,
                        "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
                        "vedtaksperiodeId" to event.vedtaksperiodeId,
                        "behandlingId" to event.behandlingId,
                        "skjæringstidspunkt" to event.skjæringstidspunkt,
                        "hendelser" to event.hendelser,
                        "ventetSiden" to event.ventetSiden,
                        "venterTil" to event.venterTil,
                        "venterPå" to mapOf(
                            "vedtaksperiodeId" to event.venterPå.vedtaksperiodeId,
                            "skjæringstidspunkt" to event.venterPå.skjæringstidspunkt,
                            "organisasjonsnummer" to event.venterPå.yrkesaktivitetssporing.somOrganisasjonsnummer,
                            "yrkesaktivitetstype" to event.venterPå.yrkesaktivitetssporing.somYrkesaktivitetstype,
                            "venteårsak" to mapOf(
                                "hva" to event.venterPå.venteårsak.hva,
                                "hvorfor" to event.venterPå.venteårsak.hvorfor
                            )
                        )
                    )
                }
            )))
    }

    override fun vedtaksperiodeOpprettet(event: PersonObserver.VedtaksperiodeOpprettet) {
        queueMessage(
            JsonMessage.newMessage(
                "vedtaksperiode_opprettet",
                mapOf(
                    "organisasjonsnummer" to event.yrkesaktivitetssporing.somOrganisasjonsnummer,
                    "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
                    "vedtaksperiodeId" to event.vedtaksperiodeId,
                    "skjæringstidspunkt" to event.skjæringstidspunkt,
                    "fom" to event.periode.start,
                    "tom" to event.periode.endInclusive
                )
            )
        )
    }

    override fun vedtaksperiodeForkastet(event: PersonObserver.VedtaksperiodeForkastetEvent) {
        queueMessage(
            JsonMessage.newMessage(
                "vedtaksperiode_forkastet",
                mapOf(
                    "organisasjonsnummer" to event.yrkesaktivitetssporing.somOrganisasjonsnummer,
                    "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
                    "vedtaksperiodeId" to event.vedtaksperiodeId,
                    "tilstand" to event.gjeldendeTilstand,
                    "hendelser" to event.hendelser,
                    "fom" to event.fom,
                    "tom" to event.tom,
                    "trengerArbeidsgiveropplysninger" to event.trengerArbeidsgiveropplysninger,
                    "speilrelatert" to event.speilrelatert,
                    "sykmeldingsperioder" to event.sykmeldingsperioder.map {
                        mapOf(
                            "fom" to it.start,
                            "tom" to it.endInclusive
                        )
                    }
                )))
    }

    override fun nyBehandling(event: PersonObserver.BehandlingOpprettetEvent) {
        queueMessage(
            JsonMessage.newMessage(
                "behandling_opprettet",
                mutableMapOf(
                    "organisasjonsnummer" to event.yrkesaktivitetssporing.somOrganisasjonsnummer,
                    "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
                    "vedtaksperiodeId" to event.vedtaksperiodeId,
                    "behandlingId" to event.behandlingId,
                    "søknadIder" to event.søknadIder,
                    "type" to event.type,
                    "fom" to event.fom,
                    "tom" to event.tom,
                    "kilde" to mapOf(
                        "meldingsreferanseId" to event.kilde.meldingsreferanseId,
                        "innsendt" to event.kilde.innsendt,
                        "registrert" to event.kilde.registert,
                        "avsender" to event.kilde.avsender
                    )
                )
            )
        )
    }

    override fun behandlingForkastet(event: PersonObserver.BehandlingForkastetEvent) {
        queueMessage(
            JsonMessage.newMessage(
                "behandling_forkastet",
                mapOf(
                    "organisasjonsnummer" to event.yrkesaktivitetssporing.somOrganisasjonsnummer,
                    "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
                    "vedtaksperiodeId" to event.vedtaksperiodeId,
                    "behandlingId" to event.behandlingId,
                    "automatiskBehandling" to event.automatiskBehandling
                )
            )
        )
    }

    override fun behandlingLukket(event: PersonObserver.BehandlingLukketEvent) {
        queueMessage(
            JsonMessage.newMessage(
                "behandling_lukket",
                mapOf(
                    "organisasjonsnummer" to event.yrkesaktivitetssporing.somOrganisasjonsnummer,
                    "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
                    "vedtaksperiodeId" to event.vedtaksperiodeId,
                    "behandlingId" to event.behandlingId
                )
            )
        )
    }

    override fun avsluttetUtenVedtak(event: PersonObserver.AvsluttetUtenVedtakEvent) {
        queueMessage(
            JsonMessage.newMessage(
                "avsluttet_uten_vedtak",
                mapOf(
                    "organisasjonsnummer" to event.yrkesaktivitetssporing.somOrganisasjonsnummer,
                    "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
                    "vedtaksperiodeId" to event.vedtaksperiodeId,
                    "behandlingId" to event.behandlingId,
                    "fom" to event.periode.start,
                    "tom" to event.periode.endInclusive,
                    "skjæringstidspunkt" to event.skjæringstidspunkt,
                    "hendelser" to event.hendelseIder,
                    "avsluttetTidspunkt" to event.avsluttetTidspunkt
                )
            )
        )
    }

    override fun avsluttetMedVedtak(event: PersonObserver.AvsluttetMedVedtakEvent) {
        queueMessage(
            JsonMessage.newMessage(
                "avsluttet_med_vedtak",
                mapOf(
                    "organisasjonsnummer" to event.yrkesaktivitetssporing.somOrganisasjonsnummer,
                    "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
                    "vedtaksperiodeId" to event.vedtaksperiodeId,
                    "behandlingId" to event.behandlingId,
                    "fom" to event.periode.start,
                    "tom" to event.periode.endInclusive,
                    "hendelser" to event.hendelseIder,
                    "skjæringstidspunkt" to event.skjæringstidspunkt,
                    "sykepengegrunnlag" to event.sykepengegrunnlag,
                    "vedtakFattetTidspunkt" to event.vedtakFattetTidspunkt,
                    "utbetalingId" to event.utbetalingId,
                    "sykepengegrunnlagsfakta" to when (val fakta = event.sykepengegrunnlagsfakta) {
                        is PersonObserver.UtkastTilVedtakEvent.FastsattEtterHovedregel -> mapOf(
                            "fastsatt" to fakta.fastsatt,
                            "omregnetÅrsinntekt" to fakta.omregnetÅrsinntekt,
                            "omregnetÅrsinntektTotalt" to fakta.omregnetÅrsinntekt,
                            "sykepengegrunnlag" to fakta.sykepengegrunnlag,
                            "6G" to fakta.`6G`,
                            "arbeidsgivere" to fakta.arbeidsgivere.map { arbeidsgiver ->
                                mapOf(
                                    "arbeidsgiver" to arbeidsgiver.arbeidsgiver,
                                    "omregnetÅrsinntekt" to arbeidsgiver.omregnetÅrsinntekt,
                                    "inntektskilde" to arbeidsgiver.inntektskilde
                                )
                            }
                        )

                        is PersonObserver.UtkastTilVedtakEvent.FastsattEtterSkjønn -> mutableMapOf(
                            "fastsatt" to fakta.fastsatt,
                            "omregnetÅrsinntekt" to fakta.omregnetÅrsinntekt,
                            "omregnetÅrsinntektTotalt" to fakta.omregnetÅrsinntekt,
                            "skjønnsfastsatt" to fakta.skjønnsfastsatt,
                            "sykepengegrunnlag" to fakta.sykepengegrunnlag,
                            "6G" to fakta.`6G`,
                            "arbeidsgivere" to fakta.arbeidsgivere.map { arbeidsgiver ->
                                mapOf(
                                    "arbeidsgiver" to arbeidsgiver.arbeidsgiver,
                                    "omregnetÅrsinntekt" to arbeidsgiver.omregnetÅrsinntekt,
                                    "skjønnsfastsatt" to arbeidsgiver.skjønnsfastsatt,
                                    "inntektskilde" to arbeidsgiver.inntektskilde
                                )
                            }
                        )

                        is PersonObserver.UtkastTilVedtakEvent.FastsattIInfotrygd -> mapOf(
                            "fastsatt" to fakta.fastsatt,
                            "omregnetÅrsinntekt" to fakta.omregnetÅrsinntekt,
                            "omregnetÅrsinntektTotalt" to fakta.omregnetÅrsinntekt
                        )
                    }
                )
            )
        )
    }

    override fun analytiskDatapakke(event: PersonObserver.AnalytiskDatapakkeEvent) {
        queueMessage(
            JsonMessage.newMessage(
                "analytisk_datapakke",
                mapOf(
                    "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
                    "vedtaksperiodeId" to event.vedtaksperiodeId,
                    "behandlingId" to event.behandlingId,
                    "skjæringstidspunkt" to event.skjæringstidspunkt,
                    "beløpTilBruker" to mapOf(
                        "totalBeløp" to event.beløpTilBruker.totalBeløp,
                        "nettoBeløp" to event.beløpTilBruker.nettoBeløp
                    ),
                    "beløpTilArbeidsgiver" to event.beløpTilArbeidsgiver,
                    "fom" to event.fom,
                    "tom" to event.tom,
                    "antallForbrukteSykedagerEtterPeriode" to mapOf(
                        "antallDager" to event.antallForbrukteSykedagerEtterPeriode.antallDager,
                        "nettoDager" to event.antallForbrukteSykedagerEtterPeriode.nettoDager
                    ),
                    "antallGjenståendeSykedagerEtterPeriode" to mapOf(
                        "antallDager" to event.antallGjenståendeSykedagerEtterPeriode.antallDager,
                        "nettoDager" to event.antallGjenståendeSykedagerEtterPeriode.nettoDager
                    ),
                    "harAndreInntekterIBeregning" to event.harAndreInntekterIBeregning
                )
            )
        )
    }

    override fun sykefraværstilfelleIkkeFunnet(event: PersonObserver.SykefraværstilfelleIkkeFunnet) {
        queueMessage(
            JsonMessage.newMessage(
                "sykefraværstilfelle_ikke_funnet", mapOf(
                "skjæringstidspunkt" to event.skjæringstidspunkt,
            )
            )
        )
    }

    override fun skatteinntekterLagtTilGrunn(event: PersonObserver.SkatteinntekterLagtTilGrunnEvent) {
        queueMessage(
            JsonMessage.newMessage(
                "skatteinntekter_lagt_til_grunn",
                mapOf(
                    "organisasjonsnummer" to event.yrkesaktivitetssporing.somOrganisasjonsnummer,
                    "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
                    "vedtaksperiodeId" to event.vedtaksperiodeId,
                    "behandlingId" to event.behandlingId,
                    "skjæringstidspunkt" to event.skjæringstidspunkt,
                    "omregnetÅrsinntekt" to event.omregnetÅrsinntekt,
                    "skatteinntekter" to event.skatteinntekter.map {
                        mapOf<String, Any>(
                            "måned" to it.måned,
                            "beløp" to it.beløp
                        )
                    }
                )))
    }

    override fun inntektsmeldingReplay(event: PersonObserver.TrengerArbeidsgiveropplysningerEvent) {
        queueMessage(event.tilJsonMessage("trenger_inntektsmelding_replay"))
    }

    override fun trengerArbeidsgiveropplysninger(event: PersonObserver.TrengerArbeidsgiveropplysningerEvent) {
        queueMessage(event.tilJsonMessage("trenger_opplysninger_fra_arbeidsgiver"))
    }

    private fun PersonObserver.TrengerArbeidsgiveropplysningerEvent.tilJsonMessage(eventName: String) =
        JsonMessage.newMessage(
            eventName,
            mapOf(
                "organisasjonsnummer" to this.yrkesaktivitetssporing.somOrganisasjonsnummer,
                "yrkesaktivitetstype" to this.yrkesaktivitetssporing.somYrkesaktivitetstype,
                "vedtaksperiodeId" to vedtaksperiodeId,
                "skjæringstidspunkt" to skjæringstidspunkt,
                "sykmeldingsperioder" to sykmeldingsperioder.map {
                    mapOf(
                        "fom" to it.start,
                        "tom" to it.endInclusive
                    )
                },
                "egenmeldingsperioder" to egenmeldingsperioder.map {
                    mapOf(
                        "fom" to it.start,
                        "tom" to it.endInclusive
                    )
                },
                "førsteFraværsdager" to førsteFraværsdager.map {
                    mapOf<String, Any>(
                        "organisasjonsnummer" to it.yrkesaktivitetssporing.somOrganisasjonsnummer,
                        "yrkesaktivitetstype" to it.yrkesaktivitetssporing.somYrkesaktivitetstype,
                        "førsteFraværsdag" to it.førsteFraværsdag
                    )
                },
                "forespurteOpplysninger" to forespurteOpplysninger.map { forespurtOpplysning ->
                    when (forespurtOpplysning) {
                        is Arbeidsgiverperiode -> mapOf(
                            "opplysningstype" to "Arbeidsgiverperiode"
                        )

                        is Inntekt -> mapOf(
                            "opplysningstype" to "Inntekt",
                            "forslag" to mapOf(
                                "forrigeInntekt" to null
                            )
                        )

                        is Refusjon -> mapOf(
                            "opplysningstype" to "Refusjon",
                            "forslag" to emptyList<Nothing>()
                        )
                    }
                }
            )
        )

    override fun trengerIkkeArbeidsgiveropplysninger(event: PersonObserver.TrengerIkkeArbeidsgiveropplysningerEvent) {
        queueMessage(
            JsonMessage.newMessage(
                "trenger_ikke_opplysninger_fra_arbeidsgiver",
                mapOf<String, Any>(
                    "organisasjonsnummer" to event.yrkesaktivitetssporing.somOrganisasjonsnummer,
                    "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
                    "vedtaksperiodeId" to event.vedtaksperiodeId
                )
            )
        )
    }

    override fun utkastTilVedtak(event: PersonObserver.UtkastTilVedtakEvent) {
        val utkastTilVedtak = mutableMapOf(
            "vedtaksperiodeId" to event.vedtaksperiodeId,
            "behandlingId" to event.behandlingId,
            "skjæringstidspunkt" to event.skjæringstidspunkt,
            "tags" to event.tags,
            "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
        )
        if (event.`6G` != null) utkastTilVedtak["sykepengegrunnlagsfakta"] = mapOf("6G" to event.`6G`)
        queueMessage(JsonMessage.newMessage("utkast_til_vedtak", utkastTilVedtak.toMap()))
    }

    private fun leggPåStandardfelter(outgoingMessage: JsonMessage) = outgoingMessage.apply {
        this["fødselsnummer"] = message.meldingsporing.fødselsnummer
    }

    private fun queueMessage(outgoingMessage: JsonMessage) {
        queueMessage(message.meldingsporing.fødselsnummer, leggPåStandardfelter(outgoingMessage).toJson())
    }

    private data class Pakke(
        private val fødselsnummer: String,
        private val eventName: String,
        private val blob: String,
    ) {
        fun tilUtgåendeMelding(): OutgoingMessage {
            sikkerLogg.info("sender $eventName: $blob")
            return OutgoingMessage(
                body = blob,
                key = fødselsnummer
            )
        }
    }
}
