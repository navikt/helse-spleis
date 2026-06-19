package no.nav.helse.spleis.utboks

import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.person.EventBus
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.EventSubscription.Arbeidsgiverperiode
import no.nav.helse.person.EventSubscription.Inntekt
import no.nav.helse.person.EventSubscription.Refusjon
import no.nav.helse.person.EventSubscription.Utbetalingsdag.Dagtype
import no.nav.helse.person.EventSubscription.Utbetalingsdag.EksternBegrunnelseDTO
import no.nav.helse.person.EventSubscription.UtkastTilVedtakEvent.FastsattEtterHovedregel
import no.nav.helse.person.EventSubscription.UtkastTilVedtakEvent.FastsattEtterSkjønn
import no.nav.helse.person.EventSubscription.UtkastTilVedtakEvent.FastsattIInfotrygd
import no.nav.helse.spleis.Behov
import no.nav.helse.spleis.meldinger.model.HendelseMessage

internal class EventBusOversetter(private val eventBus: EventBus, private val message: HendelseMessage) {
    private val personidentifikator = Personidentifikator(message.meldingsporing.fødselsnummer)

    internal fun utgåendeMeldinger() =
        eventBus.events
        .map { event ->
            // ✅ Sier om det er ryddet opp i meldingen når det gjelder å kun sende "organisasjonsnummer" ut for Arbeidstaker
            when (event) {
                is EventSubscription.AnalytiskDatapakkeEvent -> mapAnalytiskDatapakke(event) // ✅ Meldingen inneholder ikke organisasjonsnummer
                is EventSubscription.AvsluttetMedVedtakEvent -> mapAvsluttetMedVedtak(event) // ✅ Legger kun til organisasjonsnummer når det er Arbeidstaker
                is EventSubscription.AvsluttetUtenVedtakEvent -> mapAvsluttetUtenVedtak(event) // ✅ Foreløpig (før Flex sender søknader i venteperioden) er denne arbeidstaker-spesifikk
                is EventSubscription.BehandlingForkastetEvent -> mapBehandlingForkastet(event) // ✅ Legger kun til organisasjonsnummer når det er Arbeidstaker
                is EventSubscription.BehandlingLukketEvent -> mapBehandlingLukket(event) // ✅ Legger kun til organisasjonsnummer når det er Arbeidstaker
                is EventSubscription.BehandlingOpprettetEvent -> mapBehandlingOpprettet(event) // ✅ Legger kun til organisasjonsnummer når det er Arbeidstaker
                is EventSubscription.FeriepengerUtbetaltEvent -> mapFeriepengerUtbetalt(event) // ✅ Er arbeidstaker-spesifikk
                is EventSubscription.InntektsmeldingFørSøknadEvent -> mapInntektsmeldingFørSøknad(event) // ✅ Er arbeidstaker-spesifikk
                is EventSubscription.InntektsmeldingHåndtertEvent -> mapInntektsmeldingHåndtert(event) // ✅ Er arbeidstaker-spesifikk
                is EventSubscription.InntektsmeldingIkkeHåndtertEvent -> mapInntektsmeldingIkkeHåndtert(event) // ✅ Er arbeidstaker-spesifikk
                is EventSubscription.OverlappendeInfotrygdperioder -> mapOverlappendeInfotrygdperioder(event)
                is EventSubscription.OverstyringIgangsatt -> mapOverstyringIgangsatt(event)
                is EventSubscription.PlanlagtAnnulleringEvent -> mapPlanlagtAnnullering(event) // ✅ Legger kun til organisasjonsnummer når det er Arbeidstaker
                is EventSubscription.SkatteinntekterLagtTilGrunnEvent -> mapSkatteinntekterLagtTilGrunn(event) // ✅ Er arbeidstaker-spesifikk
                is EventSubscription.SykefraværstilfelleIkkeFunnet -> mapSykefraværstilfelleIkkeFunnet(event) // ✅ Meldingen er på person-nivå, så den er grei
                is EventSubscription.SøknadHåndtertEvent -> mapSøknadHåndtert(event) // ✅ Legger kun til organisasjonsnummer når det er Arbeidstaker
                is EventSubscription.TrengerArbeidsgiveropplysningerEvent -> mapTrengerArbeidsgiveropplysninger(event) // ✅ Er arbeidstaker-spesifikk
                is EventSubscription.TrengerIkkeArbeidsgiveropplysningerEvent -> mapTrengerIkkeArbeidsgiveropplysninger(event) // ✅ Er arbeidstaker-spesifikk
                is EventSubscription.TrengerInntektsmeldingReplayEvent -> mapTrengerInntektsmeldingReplay(event) // ✅ Er arbeidstaker-spesifikk
                is EventSubscription.UtbetalingAnnullertEvent -> mapUtbetalingAnnullert(event)
                is EventSubscription.UtbetalingEndretEvent -> mapUtbetalingEndret(event)
                is EventSubscription.UtbetalingUtbetaltEvent -> mapUtbetalingUtbetalt(event)
                is EventSubscription.UtbetalingUtenUtbetalingEvent -> mapUtbetalingUtenUtbetaling(event)
                is EventSubscription.UtkastTilVedtakEvent -> mapUtkastTilVedtak(event)
                is EventSubscription.VedtaksperiodeAnnullertEvent -> mapVedtaksperiodeAnnullert(event) // ✅ Legger kun til organisasjonsnummer når det er Arbeidstaker
                is EventSubscription.VedtaksperiodeEndretEvent -> mapVedtaksperiodeEndret(event)
                is EventSubscription.VedtaksperiodeForkastetEvent -> mapVedtaksperiodeForkastet(event)
                is EventSubscription.VedtaksperiodeIkkePåminnetEvent -> mapVedtaksperiodeIkkePåminnet(event) // ✅ Legger kun til organisasjonsnummer når det er Arbeidstaker
                is EventSubscription.VedtaksperiodeNyUtbetalingEvent -> mapVedtaksperiodeNyUtbetaling(event) // ✅ Legger kun til organisasjonsnummer når det er Arbeidstaker
                is EventSubscription.VedtaksperiodeOpprettet -> mapVedtaksperiodeOpprettet(event) // ✅ Legger kun til organisasjonsnummer når det er Arbeidstaker
                is EventSubscription.VedtaksperiodePåminnetEvent -> mapVedtaksperiodePåminnet(event) // ✅ Legger kun til organisasjonsnummer når det er Arbeidstaker
                is EventSubscription.VedtaksperioderVenterEvent -> mapVedtaksperioderVenter(event)
                is EventSubscription.SelvstendigIngenDagerIgjenEvent -> mapSelvstendigIngenDagerIgjen(event) // ✅ Er selvstendig-spesifikk, så den er grei
                is EventSubscription.SelvstendigUtbetaltEtterVentetidEvent -> mapSelvstendigUtbetaltEtterVentetid(event) // ✅ Er selvstendig-spesifikk, så den er grei
                is EventSubscription.TrengerInformasjonTilVilkårsprøvingEvent -> mapTrengerInformasjonTilVilkårsprøving(event)
                is EventSubscription.TrengerInformasjonTilBeregningEvent -> mapTrengerInformasjonTilBeregning(event)
                is EventSubscription.TrengerInitiellHistorikkFraInfotrygdEvent -> mapTrengerInitiellHistorikkFraInfotrygd(event)
                is EventSubscription.TrengerOppdatertHistorikkFraInfotrygdEvent -> mapTrengerOppdatertHistorikkFraInfotrygd(event) // ✅ Meldingen er på person-nivå, så den er grei
                is EventSubscription.UtbetalFeriepengerEvent -> mapUtbetalFeriepenger(event) // ✅ Er arbeidstaker-spesifikk
                is EventSubscription.SimuleringEvent -> mapSimulering(event)
                is EventSubscription.UtbetalingEvent -> mapUtbetaling(event)
                is EventSubscription.GodkjenningEvent -> mapGodkjenning(event)
                is EventSubscription.NyInformasjonIInfotrygdEvent -> mapNyInformasjonIInfotrygdEvent(event)
            }
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

    private fun mapInntektsmeldingFørSøknad(event: EventSubscription.InntektsmeldingFørSøknadEvent): UtgåendeMelding {
        return utgående(
            "inntektsmelding_før_søknad",
            mapOf(
                "inntektsmeldingId" to event.inntektsmeldingId,
                "organisasjonsnummer" to event.arbeidstaker.organisasjonsnummer,
                "yrkesaktivitetstype" to "ARBEIDSTAKER"
            )
        )
    }

    private fun mapInntektsmeldingIkkeHåndtert(event: EventSubscription.InntektsmeldingIkkeHåndtertEvent): UtgåendeMelding {
        return utgående(
            "inntektsmelding_ikke_håndtert",
            mapOf(
                "inntektsmeldingId" to event.meldingsreferanseId,
                "organisasjonsnummer" to event.arbeidstaker.organisasjonsnummer,
                "yrkesaktivitetstype" to "ARBEIDSTAKER",
                "speilrelatert" to event.speilrelatert
            )
        )
    }

    private fun mapInntektsmeldingHåndtert(event: EventSubscription.InntektsmeldingHåndtertEvent): UtgåendeMelding {
        return utgående(
            "inntektsmelding_håndtert", mapOf(
            "inntektsmeldingId" to event.meldingsreferanseId,
            "organisasjonsnummer" to event.arbeidstaker.organisasjonsnummer,
            "yrkesaktivitetstype" to "ARBEIDSTAKER",
            "vedtaksperiodeId" to event.vedtaksperiodeId,
            "vedtaksperioderMedSammeFørsteFraværsdag" to event.vedtaksperioderMedSammeFørsteFraværsdag.map { it }
        )
        )
    }

    private fun mapSøknadHåndtert(event: EventSubscription.SøknadHåndtertEvent): UtgåendeMelding {
        return utgående(
            "søknad_håndtert",
            byggMedYrkesaktivitet(
                event.yrkesaktivitetssporing,
                mapOf(
                    "søknadId" to event.meldingsreferanseId,
                    "vedtaksperiodeId" to event.vedtaksperiodeId
                )
            )
        )
    }

    private fun mapVedtaksperiodeAnnullert(vedtaksperiodeAnnullertEvent: EventSubscription.VedtaksperiodeAnnullertEvent): UtgåendeMelding {
        return utgående(
            "vedtaksperiode_annullert",
            byggMedYrkesaktivitet(
                vedtaksperiodeAnnullertEvent.yrkesaktivitetssporing,
                mapOf(
                    "fom" to vedtaksperiodeAnnullertEvent.fom,
                    "tom" to vedtaksperiodeAnnullertEvent.tom,
                    "vedtaksperiodeId" to vedtaksperiodeAnnullertEvent.vedtaksperiodeId,
                    "behandlingId" to vedtaksperiodeAnnullertEvent.behandlingId,
                )
            )
        )
    }

    private fun mapOverlappendeInfotrygdperioder(event: EventSubscription.OverlappendeInfotrygdperioder): UtgåendeMelding {
        return utgående(
            "overlappende_infotrygdperioder",
            mapOf(
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
                }
            )
        )
    }

    private fun mapVedtaksperiodePåminnet(event: EventSubscription.VedtaksperiodePåminnetEvent): UtgåendeMelding {
        return utgående(
            "vedtaksperiode_påminnet",
            byggMedYrkesaktivitet(
                event.yrkesaktivitetssporing,
                mapOf(
                    "vedtaksperiodeId" to event.vedtaksperiodeId,
                    "tilstand" to event.tilstand,
                    "antallGangerPåminnet" to event.antallGangerPåminnet,
                    "tilstandsendringstidspunkt" to event.tilstandsendringstidspunkt,
                    "påminnelsestidspunkt" to event.påminnelsestidspunkt,
                    "nestePåminnelsestidspunkt" to event.nestePåminnelsestidspunkt
                )
            )
        )
    }

    private fun mapVedtaksperiodeIkkePåminnet(event: EventSubscription.VedtaksperiodeIkkePåminnetEvent): UtgåendeMelding {
        return utgående(
            "vedtaksperiode_ikke_påminnet",
            byggMedYrkesaktivitet(
                event.yrkesaktivitetssporing,
                mapOf(
                    "vedtaksperiodeId" to event.vedtaksperiodeId,
                    "tilstand" to event.nåværendeTilstand
                )
            )
        )
    }

    private fun mapUtbetalingAnnullert(event: EventSubscription.UtbetalingAnnullertEvent): UtgåendeMelding {
        return utgående(
            "utbetaling_annullert",
            mapOf(
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
    }

    private fun mapPlanlagtAnnullering(event: EventSubscription.PlanlagtAnnulleringEvent): UtgåendeMelding {
        return utgående(
            "planlagt_annullering",
            byggMedYrkesaktivitet(
                event.yrkesaktivitetssporing,
                mapOf(
                    "vedtaksperioder" to event.vedtaksperioder,
                    "fom" to event.fom,
                    "tom" to event.tom,
                    "ident" to event.saksbehandlerIdent,
                    "årsaker" to event.årsaker,
                    "begrunnelse" to event.begrunnelse
                )
            )
        )
    }

    private fun mapUtbetalingEndret(event: EventSubscription.UtbetalingEndretEvent): UtgåendeMelding {
        return utgående(
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
    }

    private fun EventSubscription.UtbetalingEndretEvent.OppdragEventDetaljer.tilJsonMap() =
        mapOf(
            "fagsystemId" to this.fagsystemId,
            "mottaker" to this.mottaker,
            "nettoBeløp" to this.nettoBeløp,
            "linjer" to this.linjer.map {
                it.tilJsonMap()
            }
        )

    private fun EventSubscription.UtbetalingEndretEvent.OppdragEventDetaljer.OppdragEventLinjeDetaljer.tilJsonMap() =
        mapOf(
            "fom" to this.fom,
            "tom" to this.tom,
            "totalbeløp" to this.totalbeløp
        )

    private fun mapVedtaksperiodeNyUtbetaling(event: EventSubscription.VedtaksperiodeNyUtbetalingEvent): UtgåendeMelding {
        return utgående(
            "vedtaksperiode_ny_utbetaling",
            byggMedYrkesaktivitet(
                event.yrkesaktivitetssporing,
                mapOf(
                    "vedtaksperiodeId" to event.vedtaksperiodeId,
                    "utbetalingId" to event.utbetalingId
                )
            )
        )
    }

    private fun mapOverstyringIgangsatt(event: EventSubscription.OverstyringIgangsatt): UtgåendeMelding {
        return utgående(
            "overstyring_igangsatt",
            mapOf(
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
            ))
    }

    private fun mapUtbetalingUtbetalt(event: EventSubscription.UtbetalingUtbetaltEvent): UtgåendeMelding {
        return utgående(
            "utbetaling_utbetalt",
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
    }

    private fun mapUtbetalingUtenUtbetaling(event: EventSubscription.UtbetalingUtenUtbetalingEvent): UtgåendeMelding {
        return utgående(
            "utbetaling_uten_utbetaling",
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
    }

    private fun EventSubscription.OppdragEventDetaljer.OppdragEventLinjeDetaljer.tilJsonMap() =
        mapOf(
            "fom" to this.fom,
            "tom" to this.tom,
            "sats" to this.sats,
            "grad" to this.grad,
            "stønadsdager" to this.stønadsdager,
            "totalbeløp" to this.totalbeløp,
            "statuskode" to this.statuskode,
        )

    private fun EventSubscription.OppdragEventDetaljer.tilJsonMap() =
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

    private fun EventSubscription.Utbetalingsdag.tilJsonMap() =
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
                    EksternBegrunnelseDTO.MeldingTilNavDagUtenforVentetid -> "MeldingTilNavDagUtenforVentetid"
                    EksternBegrunnelseDTO.AvslåttMeldingTilNavDag -> "AvslåttMeldingTilNavDag"
                }
            }
        )

    private fun mapFeriepengerUtbetalt(event: EventSubscription.FeriepengerUtbetaltEvent): UtgåendeMelding =
        utgående(
            "feriepenger_utbetalt",
            mapOf(
                "organisasjonsnummer" to event.arbeidstaker.organisasjonsnummer,
                "yrkesaktivitetstype" to "ARBEIDSTAKER",
                "fom" to event.fom,
                "tom" to event.tom,
                "arbeidsgiverOppdrag" to event.arbeidsgiverOppdrag.tilJsonMap(),
                "personOppdrag" to event.personOppdrag.tilJsonMap()
            )
        )

    private fun EventSubscription.FeriepengerUtbetaltEvent.FeriepengeoppdragEventDetaljer.tilJsonMap() =
        mapOf(
            "fagsystemId" to this.fagsystemId,
            "mottaker" to this.mottaker,
            "totalbeløp" to this.totalbeløp
        )

    private fun mapVedtaksperiodeEndret(event: EventSubscription.VedtaksperiodeEndretEvent): UtgåendeMelding {
        return utgående(
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
    }

    private fun mapVedtaksperioderVenter(event: EventSubscription.VedtaksperioderVenterEvent): UtgåendeMelding {
        return utgående(
            "vedtaksperioder_venter", mapOf(
            "vedtaksperioder" to event.vedtaksperioder.map { event ->
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
        ))
    }

    private fun mapVedtaksperiodeOpprettet(event: EventSubscription.VedtaksperiodeOpprettet): UtgåendeMelding {
        return utgående(
            "vedtaksperiode_opprettet",
            byggMedYrkesaktivitet(
                event.yrkesaktivitetssporing,
                mapOf(
                    "vedtaksperiodeId" to event.vedtaksperiodeId,
                    "skjæringstidspunkt" to event.skjæringstidspunkt,
                    "fom" to event.periode.start,
                    "tom" to event.periode.endInclusive
                )
            )
        )
    }

    private fun mapVedtaksperiodeForkastet(event: EventSubscription.VedtaksperiodeForkastetEvent): UtgåendeMelding {
        return utgående(
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
            ))
    }

    private fun mapBehandlingOpprettet(event: EventSubscription.BehandlingOpprettetEvent): UtgåendeMelding {
        return utgående(
            "behandling_opprettet",
            byggMedYrkesaktivitet(
                event.yrkesaktivitetssporing,
                mapOf(
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

    private fun mapBehandlingForkastet(event: EventSubscription.BehandlingForkastetEvent): UtgåendeMelding {
        return utgående(
            "behandling_forkastet",
            byggMedYrkesaktivitet(
                event.yrkesaktivitetssporing,
                mapOf(
                    "vedtaksperiodeId" to event.vedtaksperiodeId,
                    "behandlingId" to event.behandlingId,
                    "automatiskBehandling" to event.automatiskBehandling
                )
            )
        )
    }

    private fun mapBehandlingLukket(event: EventSubscription.BehandlingLukketEvent): UtgåendeMelding {
        return utgående(
            "behandling_lukket",
            byggMedYrkesaktivitet(
                event.yrkesaktivitetssporing,
                mapOf(
                    "vedtaksperiodeId" to event.vedtaksperiodeId,
                    "behandlingId" to event.behandlingId
                )
            )
        )
    }

    private fun mapTrengerInformasjonTilVilkårsprøving(event: EventSubscription.TrengerInformasjonTilVilkårsprøvingEvent): UtgåendeMelding {
        val behov = listOfNotNull(
            Behov(Behov.Behovstype.InntekterForSykepengegrunnlag, mapOf(
                "skjæringstidspunkt" to event.skjæringstidspunkt,
                "beregningStart" to event.beregningsperiodeForSykepengegrunnlagsvurdering.start,
                "beregningSlutt" to event.beregningsperiodeForSykepengegrunnlagsvurdering.slutt
            )),
            Behov(Behov.Behovstype.InntekterForOpptjeningsvurdering, mapOf(
                "skjæringstidspunkt" to event.skjæringstidspunkt,
                "beregningStart" to event.beregningsperiodeForOpptjeningsvurdering.start,
                "beregningSlutt" to event.beregningsperiodeForOpptjeningsvurdering.slutt
            )),
            Behov(Behov.Behovstype.Arbeidsforhold, mapOf(
                "skjæringstidspunkt" to event.skjæringstidspunkt
            )),
            Behov(Behov.Behovstype.Medlemskap, mapOf(
                "skjæringstidspunkt" to event.skjæringstidspunkt,
                "medlemskapPeriodeFom" to event.periodeForMedlemskapsvurdering.start,
                "medlemskapPeriodeTom" to event.periodeForMedlemskapsvurdering.endInclusive
            )
            ),
            Behov(
                Behov.Behovstype.Forsikringsvurdering, mapOf(
                "skjæringstidspunkt" to event.skjæringstidspunkt,
                "spesielleYrkesgrupper" to emptyList<String>()
            )
            ).takeIf { event.yrkesaktivitetssporing.somYrkesaktivitetstype == "SELVSTENDIG" }
        )

        // TODO: Her skulle vi brukt byggMedYrkesaktivitet - men må sjekke appene som svarer behovene for i dag har behovene alltid organisasjonsnummer
        return behov.utgående(message.meldingsporing.id,mapOf(
            "organisasjonsnummer" to event.yrkesaktivitetssporing.somOrganisasjonsnummer,
            "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
            "vedtaksperiodeId" to event.vedtaksperiodeId,
            "behandlingId" to event.behandlingId
        ))
    }

    private fun mapTrengerInformasjonTilBeregning(event: EventSubscription.TrengerInformasjonTilBeregningEvent): UtgåendeMelding {
        val behov = listOfNotNull(
            Behov(Behov.Behovstype.Foreldrepenger, mapOf(
                "foreldrepengerFom" to event.periodeForForeldrepenger.start,
                "foreldrepengerTom" to event.periodeForForeldrepenger.endInclusive
            )),
            Behov(Behov.Behovstype.Pleiepenger, mapOf(
                "pleiepengerFom" to event.periodeForPleiepenger.start,
                "pleiepengerTom" to event.periodeForPleiepenger.endInclusive
            )),
            Behov(Behov.Behovstype.Omsorgspenger, mapOf(
                "omsorgspengerFom" to event.periodeForOmsorgspenger.start,
                "omsorgspengerTom" to event.periodeForOmsorgspenger.endInclusive
            )),
            Behov(Behov.Behovstype.Opplæringspenger, mapOf(
                "opplæringspengerFom" to event.periodeForOpplæringspenger.start,
                "opplæringspengerTom" to event.periodeForOpplæringspenger.endInclusive
            )),
            Behov(Behov.Behovstype.Institusjonsopphold, mapOf(
                "institusjonsoppholdFom" to event.periodeForInstitusjonsopphold.start,
                "institusjonsoppholdTom" to event.periodeForInstitusjonsopphold.endInclusive
            )),
            Behov(Behov.Behovstype.Arbeidsavklaringspenger, mapOf(
                "periodeFom" to event.periodeForArbeidsavklaringspenger.start,
                "periodeTom" to event.periodeForArbeidsavklaringspenger.endInclusive
            )),
            Behov(Behov.Behovstype.Dagpenger, mapOf(
                "periodeFom" to event.periodeForDagpenger.start,
                "periodeTom" to event.periodeForDagpenger.endInclusive
            )),
            Behov(Behov.Behovstype.InntekterForBeregning, mapOf(
                "fom" to event.beregningsperiode.start,
                "tom" to event.beregningsperiode.endInclusive
            )),
            Behov(
                Behov.Behovstype.Forsikringsvurdering, mapOf(
                "skjæringstidspunkt" to event.skjæringstidspunkt,
                "spesielleYrkesgrupper" to emptyList<String>()
            )).takeIf { event.trengerInformasjonOmSelvstendigForsikring }
        )

        // TODO: Her skulle vi brukt byggMedYrkesaktivitet - men må sjekke appene som svarer behovene for i dag har behovene alltid organisasjonsnummer
        return behov.utgående(message.meldingsporing.id,mapOf(
            "organisasjonsnummer" to event.yrkesaktivitetssporing.somOrganisasjonsnummer,
            "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
            "vedtaksperiodeId" to event.vedtaksperiodeId,
            "behandlingId" to event.behandlingId
        ))
    }

    private fun mapTrengerInitiellHistorikkFraInfotrygd(event: EventSubscription.TrengerInitiellHistorikkFraInfotrygdEvent): UtgåendeMelding {
        return listOf(Behov(Behov.Behovstype.Sykepengehistorikk, mapOf(
            "historikkFom" to event.periode.start,
            "historikkTom" to event.periode.endInclusive,
        ))).utgående(message.meldingsporing.id, mapOf(
            "vedtaksperiodeId" to event.vedtaksperiodeId,
            "organisasjonsnummer" to event.yrkesaktivitetssporing.somOrganisasjonsnummer,
            "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype
        ))
    }

    private fun mapTrengerOppdatertHistorikkFraInfotrygd(event: EventSubscription.TrengerOppdatertHistorikkFraInfotrygdEvent): UtgåendeMelding {
        return listOf(Behov(Behov.Behovstype.Sykepengehistorikk, mapOf(
            "historikkFom" to event.periode.start,
            "historikkTom" to event.periode.endInclusive
        ))).utgående(message.meldingsporing.id)
    }

    private fun mapUtbetalFeriepenger(event: EventSubscription.UtbetalFeriepengerEvent): UtgåendeMelding {
        return listOf(Behov(Behov.Behovstype.Feriepengeutbetaling, mapOf(
            "mottaker" to event.mottaker,
            "fagområde" to event.fagområde,
            "linjer" to listOf(mapOf(
                "fom" to "${event.linje.periode.start}",
                "tom" to "${event.linje.periode.endInclusive}",
                "satstype" to event.linje.satstype,
                "sats" to event.linje.sats,
                "endringskode" to event.linje.endringskode,
                "delytelseId" to event.linje.delytelseId,
                "refDelytelseId" to event.linje.refDelytelseId,
                "refFagsystemId" to event.linje.refFagsystemId,
                "statuskode" to event.linje.statuskode,
                "datoStatusFom" to event.linje.datoStatusFom?.toString(),
                "klassekode" to event.linje.klassekode
            )),
            "fagsystemId" to event.fagsystemId,
            "endringskode" to event.endringskode,
            "saksbehandler" to event.saksbehandler
        ))).utgående(message.meldingsporing.id,mapOf(
            "yrkesaktivitetstype" to "ARBEIDSTAKER",
            "organisasjonsnummer" to event.organisasjonsnummer,
            "utbetalingId" to event.utbetalingId,
            "fagsystemId" to event.fagsystemId
        ))
    }

    private fun EventSubscription.Oppdragsdetaljer.somMap(saksbehandler: String): Map<String, Any> = mapOf(
        "mottaker" to mottaker,
        "fagområde" to fagområde,
        "linjer" to linjer.map { linje -> mapOf(
            "fom" to linje.periode.start,
            "tom" to linje.periode.endInclusive,
            "satstype" to linje.satstype,
            "sats" to linje.sats,
            "grad" to linje.grad.toDouble(), // backwards-compatibility mot andre systemer som forventer double: må gjennomgås
            "stønadsdager" to linje.stønadsdager,
            "totalbeløp" to linje.totalbeløp,
            "endringskode" to linje.endringskode,
            "delytelseId" to linje.delytelseId,
            "refDelytelseId" to linje.refDelytelseId,
            "refFagsystemId" to linje.refFagsystemId,
            "statuskode" to linje.statuskode,
            "datoStatusFom" to linje.datoStatusFom,
            "klassekode" to linje.klassekode,
            "datoKlassifikFom" to linje.datoKlassifikFom
        ) },
        "fagsystemId" to fagsystemId,
        "endringskode" to endringskode,
        "saksbehandler" to saksbehandler
    ).let { when (val maksdatoen = maksdato) {
        null -> it
        else -> it.plus("maksdato" to maksdatoen)
    } }

    private fun mapUtbetaling(event: EventSubscription.UtbetalingEvent): UtgåendeMelding {
        // TODO: Her skulle vi brukt byggMedYrkesaktivitet - men må sjekke appene som svarer behovene for i dag har behovene alltid organisasjonsnummer
        return listOf(Behov(Behov.Behovstype.Utbetaling, event.oppdragsdetaljer.somMap(event.saksbehandler))).utgående(message.meldingsporing.id,mapOf(
            "organisasjonsnummer" to event.yrkesaktivitetssporing.somOrganisasjonsnummer,
            "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
            "vedtaksperiodeId" to event.vedtaksperiodeId,
            "behandlingId" to event.behandlingId,
            "utbetalingId" to event.utbetalingId,
            "fagsystemId" to event.oppdragsdetaljer.fagsystemId
        ))
    }

    private fun mapSimulering(event: EventSubscription.SimuleringEvent): UtgåendeMelding {
        // TODO: Her skulle vi brukt byggMedYrkesaktivitet - men må sjekke appene som svarer behovene for i dag har behovene alltid organisasjonsnummer
        return listOf(Behov(Behov.Behovstype.Simulering, event.oppdragsdetaljer.somMap(event.saksbehandler))).utgående(message.meldingsporing.id,mapOf(
            "organisasjonsnummer" to event.yrkesaktivitetssporing.somOrganisasjonsnummer,
            "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
            "vedtaksperiodeId" to event.vedtaksperiodeId,
            "behandlingId" to event.behandlingId,
            "utbetalingId" to event.utbetalingId,
            "fagsystemId" to event.oppdragsdetaljer.fagsystemId
        ))
    }

    private fun mapGodkjenning(event: EventSubscription.GodkjenningEvent): UtgåendeMelding {
        // TODO: Her skulle vi brukt byggMedYrkesaktivitet - men må sjekke appene som svarer behovene for i dag har behovene alltid organisasjonsnummer
        return listOf(Behov(Behov.Behovstype.Godkjenning, event.behovInput)).utgående(message.meldingsporing.id,mapOf(
            "organisasjonsnummer" to event.yrkesaktivitetssporing.somOrganisasjonsnummer,
            "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
            "vedtaksperiodeId" to event.vedtaksperiodeId,
            "behandlingId" to event.behandlingId,
            "utbetalingId" to event.utbetalingId
        ))
    }

    private fun mapNyInformasjonIInfotrygdEvent(event: EventSubscription.NyInformasjonIInfotrygdEvent) =
        utgående("ny_informasjon_i_infotrygd", mapOf("fom" to event.fraOgMed))

    private fun mapAvsluttetUtenVedtak(event: EventSubscription.AvsluttetUtenVedtakEvent): UtgåendeMelding {
        return utgående(
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
    }

    private fun mapAvsluttetMedVedtak(event: EventSubscription.AvsluttetMedVedtakEvent): UtgåendeMelding {
        return utgående(
            "avsluttet_med_vedtak",
            mapOf(
                "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
                "vedtaksperiodeId" to event.vedtaksperiodeId,
                "behandlingId" to event.behandlingId,
                "fom" to event.periode.start,
                "tom" to event.periode.endInclusive,
                "hendelser" to event.hendelseIder,
                "skjæringstidspunkt" to event.skjæringstidspunkt,
                "vedtakFattetTidspunkt" to event.vedtakFattetTidspunkt,
                "utbetalingId" to event.utbetalingId,
                "sykepengegrunnlagsfakta" to mapOf(
                    "sykepengegrunnlag" to event.sykepengegrunnlag,
                    "6G" to event.`6G`
                ).plus(
                    when (event.yrkesaktivitetssporing) {
                        Behandlingsporing.Yrkesaktivitet.Arbeidsledig,
                        is Behandlingsporing.Yrkesaktivitet.Arbeidstaker -> when (val fakta = event.sykepengegrunnlagsfakta) {
                            is FastsattIInfotrygd -> arbeidstakerInfotrygdMap(fakta)
                            is FastsattEtterHovedregel -> arbeidstakerHovedregelMap(fakta)
                            is FastsattEtterSkjønn -> arbeidstakerEtterSkjønnMap(fakta)
                        }.plus(
                            "selvstendig" to null
                        )

                        Behandlingsporing.Yrkesaktivitet.Selvstendig ->
                            mapOf(
                                "selvstendig" to mapOf(
                                    "beregningsgrunnlag" to event.beregningsgrunnlag,
                                ),
                                "fastsatt" to "EtterHovedregel",
                                "arbeidsgivere" to emptyList<Map<String, Any>>()

                            )
                        Behandlingsporing.Yrkesaktivitet.Frilans -> TODO("avsluttet_med_vedtak for frilanser er ikke implementert, og vi burde aldri komme hit.")
                    }
                )
            ),
        )
    }

    private fun mapSelvstendigUtbetaltEtterVentetid(event: EventSubscription.SelvstendigUtbetaltEtterVentetidEvent): UtgåendeMelding {
        return utgående(
            "selvstendig_utbetalt_etter_ventetid",
            mapOf(
                "yrkesaktivitetstype" to Behandlingsporing.Yrkesaktivitet.Selvstendig.somYrkesaktivitetstype,
                "behandlingId" to event.behandlingId,
                "skjæringstidspunkt" to event.skjæringstidspunkt
            )
        )
    }

    private fun mapSelvstendigIngenDagerIgjen(event: EventSubscription.SelvstendigIngenDagerIgjenEvent): UtgåendeMelding {
        return utgående(
            "selvstendig_ingen_dager_igjen",
            mapOf(
                "yrkesaktivitetstype" to Behandlingsporing.Yrkesaktivitet.Selvstendig.somYrkesaktivitetstype,
                "behandlingId" to event.behandlingId,
                "skjæringstidspunkt" to event.skjæringstidspunkt
            )
        )
    }

    private fun mapAnalytiskDatapakke(event: EventSubscription.AnalytiskDatapakkeEvent): UtgåendeMelding {
        return utgående(
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
    }

    private fun mapSykefraværstilfelleIkkeFunnet(event: EventSubscription.SykefraværstilfelleIkkeFunnet): UtgåendeMelding {
        return utgående(
            "sykefraværstilfelle_ikke_funnet",
            mapOf("skjæringstidspunkt" to event.skjæringstidspunkt)
        )
    }

    private fun mapSkatteinntekterLagtTilGrunn(event: EventSubscription.SkatteinntekterLagtTilGrunnEvent): UtgåendeMelding {
        return utgående(
            "skatteinntekter_lagt_til_grunn",
            mapOf(
                "organisasjonsnummer" to event.arbeidstaker.organisasjonsnummer,
                "yrkesaktivitetstype" to "ARBEIDSTAKER",
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
            ))
    }

    private fun mapTrengerInntektsmeldingReplay(event: EventSubscription.TrengerInntektsmeldingReplayEvent): UtgåendeMelding {
        return utgående("trenger_inntektsmelding_replay", event.opplysninger.tilJsonMap())
    }

    private fun mapTrengerArbeidsgiveropplysninger(event: EventSubscription.TrengerArbeidsgiveropplysningerEvent): UtgåendeMelding {
        return utgående("trenger_opplysninger_fra_arbeidsgiver", event.opplysninger.tilJsonMap())
    }

    private fun EventSubscription.TrengerArbeidsgiveropplysninger.tilJsonMap(): Map<String, Any> {
        return mapOf(
            "organisasjonsnummer" to this.arbeidstaker.organisasjonsnummer,
            "yrkesaktivitetstype" to "ARBEIDSTAKER",
            "vedtaksperiodeId" to this.vedtaksperiodeId,
            "skjæringstidspunkt" to this.skjæringstidspunkt,
            "sykmeldingsperioder" to this.sykmeldingsperioder.map {
                mapOf(
                    "fom" to it.start,
                    "tom" to it.endInclusive
                )
            },
            "egenmeldingsperioder" to this.egenmeldingsperioder.map {
                mapOf(
                    "fom" to it.start,
                    "tom" to it.endInclusive
                )
            },
            "førsteFraværsdager" to this.førsteFraværsdager.map {
                mapOf<String, Any>(
                    "organisasjonsnummer" to it.arbeidstaker.organisasjonsnummer,
                    "yrkesaktivitetstype" to "ARBEIDSTAKER",
                    "førsteFraværsdag" to it.førsteFraværsdag
                )
            },
            "forespurteOpplysninger" to this.forespurteOpplysninger.map { forespurtOpplysning ->
                mapOf(
                    "opplysningstype" to when (forespurtOpplysning) {
                        Arbeidsgiverperiode -> "Arbeidsgiverperiode"
                        Inntekt -> "Inntekt"
                        Refusjon -> "Refusjon"
                    }
                )
            }
        )
    }

    private fun mapTrengerIkkeArbeidsgiveropplysninger(event: EventSubscription.TrengerIkkeArbeidsgiveropplysningerEvent): UtgåendeMelding {
        return utgående(
            "trenger_ikke_opplysninger_fra_arbeidsgiver",
            mapOf<String, Any>(
                "organisasjonsnummer" to event.arbeidstaker.organisasjonsnummer,
                "yrkesaktivitetstype" to "ARBEIDSTAKER",
                "vedtaksperiodeId" to event.vedtaksperiodeId
            )
        )
    }

    private fun mapUtkastTilVedtak(event: EventSubscription.UtkastTilVedtakEvent): UtgåendeMelding {
        val utkastTilVedtak = mutableMapOf(
            "vedtaksperiodeId" to event.vedtaksperiodeId,
            "behandlingId" to event.behandlingId,
            "skjæringstidspunkt" to event.skjæringstidspunkt,
            "tags" to event.tags,
            "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
        )
        if (event.`6G` != null) utkastTilVedtak["sykepengegrunnlagsfakta"] = mapOf("6G" to event.`6G`)
        return utgående("utkast_til_vedtak", utkastTilVedtak.toMap())
    }

    /** Legger alltid til yrkesaktivitetstype, men legger kun til organisasjonsnummer for Arbeidstaker **/
    private fun byggMedYrkesaktivitet(yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet, innhold: Map<String, Any>) =
        buildMap {
            put("yrkesaktivitetstype", yrkesaktivitetssporing.somYrkesaktivitetstype)
            compute("organisasjonsnummer") { _, _ ->
                (yrkesaktivitetssporing as? Behandlingsporing.Yrkesaktivitet.Arbeidstaker)?.organisasjonsnummer
            }
            putAll(innhold)
        }

    private fun arbeidstakerEtterSkjønnMap(sykepengegrunnlagsfakta: FastsattEtterSkjønn): Map<String, Any> = mapOf(
        "fastsatt" to sykepengegrunnlagsfakta.fastsatt,
        "omregnetÅrsinntektTotalt" to sykepengegrunnlagsfakta.omregnetÅrsinntekt,
        "skjønnsfastsatt" to sykepengegrunnlagsfakta.skjønnsfastsatt,
        "arbeidsgivere" to sykepengegrunnlagsfakta.arbeidsgivere.map { arbeidsgiver ->
            mapOf(
                "arbeidsgiver" to arbeidsgiver.arbeidsgiver,
                "omregnetÅrsinntekt" to arbeidsgiver.omregnetÅrsinntekt,
                "skjønnsfastsatt" to arbeidsgiver.skjønnsfastsatt,
                "inntektskilde" to arbeidsgiver.inntektskilde
            )
        }
    )

    private fun arbeidstakerHovedregelMap(sykepengegrunnlagsfakta: FastsattEtterHovedregel): Map<String, Any> = mapOf(
        "fastsatt" to sykepengegrunnlagsfakta.fastsatt,
        "omregnetÅrsinntektTotalt" to sykepengegrunnlagsfakta.omregnetÅrsinntekt,
        "arbeidsgivere" to sykepengegrunnlagsfakta.arbeidsgivere.map { arbeidsgiver ->
            mapOf(
                "arbeidsgiver" to arbeidsgiver.arbeidsgiver,
                "omregnetÅrsinntekt" to arbeidsgiver.omregnetÅrsinntekt,
                "inntektskilde" to arbeidsgiver.inntektskilde
            )
        }
    )

    private fun arbeidstakerInfotrygdMap(sykepengegrunnlagsfakta: FastsattIInfotrygd): Map<String, Any> = mapOf(
        "fastsatt" to sykepengegrunnlagsfakta.fastsatt,
        "omregnetÅrsinntektTotalt" to sykepengegrunnlagsfakta.omregnetÅrsinntekt
    )

    private fun utgående(eventName: String, innhold: Map<String, Any>) = UtgåendeMelding.nyRapidmelding(personidentifikator, eventName, innhold)

    private fun List<Behov>.utgående(meldingsreferanseId: MeldingsreferanseId, extra: Map<String, Any> = emptyMap()) = UtgåendeMelding.nyttBehov(personidentifikator, meldingsreferanseId, this, extra)
}
