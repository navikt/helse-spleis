package no.nav.helse.spleis.speil.builders

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import no.nav.helse.dto.BehandlingtilstandDto
import no.nav.helse.dto.BeløpstidslinjeDto
import no.nav.helse.dto.BeløpstidslinjeDto.BeløpstidslinjeperiodeDto
import no.nav.helse.dto.PeriodeDto
import no.nav.helse.dto.UtbetalingTilstandDto
import no.nav.helse.dto.UtbetalingtypeDto
import no.nav.helse.dto.VedtaksperiodetilstandDto
import no.nav.helse.dto.serialisering.ArbeidsgiverUtDto
import no.nav.helse.dto.serialisering.BehandlingUtDto
import no.nav.helse.dto.serialisering.MaksdatoresultatUtDto
import no.nav.helse.dto.serialisering.OppdragUtDto
import no.nav.helse.dto.serialisering.SelvstendigFaktaavklartInntektUtDto
import no.nav.helse.dto.serialisering.UbrukteRefusjonsopplysningerUtDto
import no.nav.helse.dto.serialisering.VedtaksperiodeUtDto
import no.nav.helse.forrigeDag
import no.nav.helse.mapWithNext
import no.nav.helse.spleis.speil.SpekematDTO
import no.nav.helse.spleis.speil.builders.ArbeidsgiverBuilder.Companion.fjernUnødvendigeRader
import no.nav.helse.spleis.speil.dto.AlderDTO
import no.nav.helse.spleis.speil.dto.AnnullertPeriode
import no.nav.helse.spleis.speil.dto.AnnullertUtbetaling
import no.nav.helse.spleis.speil.dto.BeregnetPeriode
import no.nav.helse.spleis.speil.dto.Periodetilstand
import no.nav.helse.spleis.speil.dto.Refusjonselement
import no.nav.helse.spleis.speil.dto.SpeilGenerasjonDTO
import no.nav.helse.spleis.speil.dto.SpeilOppdrag
import no.nav.helse.spleis.speil.dto.SpeilTidslinjeperiode
import no.nav.helse.spleis.speil.dto.SpeilTidslinjeperiode.Companion.utledPeriodetyper
import no.nav.helse.spleis.speil.dto.Tidslinjeperiodetype
import no.nav.helse.spleis.speil.dto.UberegnetPeriode
import no.nav.helse.spleis.speil.dto.Utbetaling
import no.nav.helse.spleis.speil.dto.Utbetalingstatus
import no.nav.helse.spleis.speil.dto.Utbetalingstidslinjedag
import no.nav.helse.spleis.speil.dto.UtbetalingstidslinjedagType
import no.nav.helse.spleis.speil.dto.Utbetalingtype
import no.nav.helse.spleis.speil.merge
import no.nav.helse.økonomi.Inntekt.Companion.daglig

internal class SpeilGenerasjonerBuilder(
    private val organisasjonsnummer: String,
    private val alder: AlderDTO,
    private val arbeidsgiverUtDto: ArbeidsgiverUtDto,
    private val vilkårsgrunnlaghistorikk: IVilkårsgrunnlagHistorikk,
    private val pølsepakke: SpekematDTO.PølsepakkeDTO
) {
    private val utbetalinger = mapUtbetalinger()
    private val annulleringer = mapAnnulleringer()

    private val gjeldendeRefusjonsopplysningerPerSkjæringstidspunkt = arbeidsgiverUtDto.vedtaksperioder
        .groupBy { it.skjæringstidspunkt }
        .tilRefusjonstidslinjer(arbeidsgiverUtDto.ubrukteRefusjonsopplysninger)
        .slåSammenSammenhengendeRefusjonstidslinjer()
        .tilRefusjonselementerUtenGapOgÅpenHale()
        .mapValues { (_, refusjonselementer) ->
            IArbeidsgiverrefusjon(organisasjonsnummer, refusjonselementer)
        }

    internal fun build(): List<SpeilGenerasjonDTO> {
        return buildSpekemat()
    }

    private fun mapPerioder(): List<SpeilTidslinjeperiode> {
        val aktive = arbeidsgiverUtDto.vedtaksperioder.map {
            it.behandlinger.behandlinger.last().takeIf { sisteBehandling -> sisteBehandling.endringer.last().vilkårsgrunnlagId != null }?.endringer?.last()?.let { sisteBeregnedeEndring ->
                vilkårsgrunnlaghistorikk.leggRefusjonsopplysningerIBøtta(sisteBeregnedeEndring.vilkårsgrunnlagId!!, gjeldendeRefusjonsopplysningerPerSkjæringstidspunkt.getValue(sisteBeregnedeEndring.skjæringstidspunkt))
            }
            mapVedtaksperiode(it)
        }.flatten()
        val forkastede = arbeidsgiverUtDto.forkastede.flatMap { mapVedtaksperiode(it.vedtaksperiode) }
        return aktive + forkastede
    }

    private fun mapVedtaksperiode(vedtaksperiode: VedtaksperiodeUtDto): List<SpeilTidslinjeperiode> {
        var forrigeGenerasjon: SpeilTidslinjeperiode? = null
        return vedtaksperiode.behandlinger.behandlinger.mapNotNull { generasjon ->
            when (generasjon.tilstand) {
                BehandlingtilstandDto.BEREGNET,
                BehandlingtilstandDto.BEREGNET_OMGJØRING,
                BehandlingtilstandDto.BEREGNET_REVURDERING,
                BehandlingtilstandDto.REVURDERT_VEDTAK_AVVIST,
                BehandlingtilstandDto.VEDTAK_FATTET,
                BehandlingtilstandDto.VEDTAK_IVERKSATT -> mapBeregnetPeriode(vedtaksperiode, generasjon)

                BehandlingtilstandDto.UBEREGNET,
                BehandlingtilstandDto.UBEREGNET_OMGJØRING,
                BehandlingtilstandDto.UBEREGNET_REVURDERING,
                BehandlingtilstandDto.UBEREGNET_ANNULLERING,
                BehandlingtilstandDto.AVSLUTTET_UTEN_VEDTAK -> mapUberegnetPeriode(vedtaksperiode, generasjon)

                BehandlingtilstandDto.TIL_INFOTRYGD -> mapTilInfotrygdperiode(vedtaksperiode, forrigeGenerasjon, generasjon)
                BehandlingtilstandDto.OVERFØRT_ANNULLERING,
                BehandlingtilstandDto.ANNULLERT_PERIODE -> mapAnnullertPeriode(vedtaksperiode, generasjon)
            }.also {
                forrigeGenerasjon = it
            }
        }
    }

    private fun mapTilInfotrygdperiode(vedtaksperiode: VedtaksperiodeUtDto, forrigePeriode: SpeilTidslinjeperiode?, generasjon: BehandlingUtDto): UberegnetPeriode? {
        if (forrigePeriode == null) return null // todo: her kan vi mappe perioder som tas ut av Speil
        return mapUberegnetPeriode(vedtaksperiode, generasjon, Periodetilstand.Annullert)
    }

    private fun mapUberegnetPeriode(vedtaksperiode: VedtaksperiodeUtDto, generasjon: BehandlingUtDto, periodetilstand: Periodetilstand? = null): UberegnetPeriode {
        val sisteEndring = generasjon.endringer.last()
        val sykdomstidslinje = SykdomstidslinjeBuilder(sisteEndring.sykdomstidslinje, sisteEndring.periode, sisteEndring.dagerNavOvertarAnsvar).build()
        val utbetalingstidslinje = UtbetalingstidslinjeBuilder(sisteEndring.utbetalingstidslinje).build()
        return UberegnetPeriode(
            vedtaksperiodeId = vedtaksperiode.id,
            behandlingId = generasjon.id,
            kilde = generasjon.kilde.meldingsreferanseId.id,
            fom = sisteEndring.periode.fom,
            tom = sisteEndring.periode.tom,
            sammenslåttTidslinje = sykdomstidslinje.merge(utbetalingstidslinje),
            periodetype = Tidslinjeperiodetype.FØRSTEGANGSBEHANDLING, // feltet gir ikke mening for uberegnede perioder
            erForkastet = false,
            opprettet = generasjon.endringer.first().tidsstempel,
            oppdatert = sisteEndring.tidsstempel,
            skjæringstidspunkt = vedtaksperiode.skjæringstidspunkt,
            hendelser = dokumenterTilOgMedDenneGenerasjonen(vedtaksperiode, generasjon),
            pensjonsgivendeInntekter = (sisteEndring.faktaavklartInntekt as? SelvstendigFaktaavklartInntektUtDto)?.pensjonsgivendeInntekter ?: emptyList(),
            periodetilstand = periodetilstand ?: generasjon.avsluttet?.let { Periodetilstand.IngenUtbetaling } ?: when (vedtaksperiode.tilstand) {
                is VedtaksperiodetilstandDto.AVVENTER_REVURDERING -> Periodetilstand.UtbetaltVenterPåAnnenPeriode

                is VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE,
                is VedtaksperiodetilstandDto.AVVENTER_BLOKKERENDE_PERIODE,
                is VedtaksperiodetilstandDto.AVVENTER_SØKNAD_FOR_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODE,
                is VedtaksperiodetilstandDto.AVVENTER_AVSLUTTET_UTEN_UTBETALING -> Periodetilstand.VenterPåAnnenPeriode

                is VedtaksperiodetilstandDto.AVVENTER_HISTORIKK,
                is VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_HISTORIKK,
                is VedtaksperiodetilstandDto.AVVENTER_HISTORIKK_REVURDERING,
                is VedtaksperiodetilstandDto.AVVENTER_VILKÅRSPRØVING,
                is VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_VILKÅRSPRØVING,
                is VedtaksperiodetilstandDto.AVVENTER_VILKÅRSPRØVING_REVURDERING -> Periodetilstand.ForberederGodkjenning

                is VedtaksperiodetilstandDto.AVVENTER_INNTEKTSMELDING -> Periodetilstand.AvventerInntektsopplysninger

                is VedtaksperiodetilstandDto.AVVENTER_ANNULLERING -> Periodetilstand.AvventerAnnullering


                is VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK,
                is VedtaksperiodetilstandDto.AVVENTER_INFOTRYGDHISTORIKK -> Periodetilstand.ManglerInformasjon

                else -> error("Forventer ikke mappingregel for ${vedtaksperiode.tilstand}")
            }

        )
    }

    private fun mapBeregnetPeriode(vedtaksperiode: VedtaksperiodeUtDto, generasjon: BehandlingUtDto): BeregnetPeriode {
        val sisteEndring = generasjon.endringer.last()
        val utbetaling = utbetalinger.singleOrNull { it.id == sisteEndring.utbetalingId } ?: error("Fant ikke tilhørende utbetaling for vedtaksperiodeId=${vedtaksperiode.id}")
        val utbetalingstidslinje = UtbetalingstidslinjeBuilder(sisteEndring.utbetalingstidslinje).build()
        val skjæringstidspunkt = sisteEndring.skjæringstidspunkt
        val sisteSykepengedag = utbetalingstidslinje.sisteNavDag()?.dato ?: sisteEndring.periode.tom
        val sykdomstidslinje = SykdomstidslinjeBuilder(sisteEndring.sykdomstidslinje, sisteEndring.periode, sisteEndring.dagerNavOvertarAnsvar).build()
        return BeregnetPeriode(
            vedtaksperiodeId = vedtaksperiode.id,
            behandlingId = generasjon.id,
            kilde = generasjon.kilde.meldingsreferanseId.id,
            fom = sisteEndring.periode.fom,
            tom = sisteEndring.periode.tom,
            sammenslåttTidslinje = sykdomstidslinje.merge(utbetalingstidslinje),
            erForkastet = false,
            periodetype = Tidslinjeperiodetype.FØRSTEGANGSBEHANDLING, // TODO: fikse
            opprettet = vedtaksperiode.opprettet,
            behandlingOpprettet = generasjon.endringer.first().tidsstempel,
            oppdatert = vedtaksperiode.oppdatert,
            periodetilstand = utledePeriodetilstand(vedtaksperiode.tilstand, generasjon.tilstand, utbetalingstidslinje),
            skjæringstidspunkt = skjæringstidspunkt,
            hendelser = dokumenterTilOgMedDenneGenerasjonen(vedtaksperiode, generasjon),
            maksdato = sisteEndring.maksdatoresultat.maksdato,
            forbrukteSykedager = sisteEndring.maksdatoresultat.forbrukteDager.antallDager(),
            gjenståendeDager = sisteEndring.maksdatoresultat.gjenståendeDager,
            beregningId = sisteEndring.id,
            utbetaling = utbetaling,
            periodevilkår = periodevilkår(sisteSykepengedag, sisteEndring.maksdatoresultat, alder, skjæringstidspunkt),
            vilkårsgrunnlagId = sisteEndring.vilkårsgrunnlagId!!,
            refusjonstidslinje = mapRefusjonstidslinje(arbeidsgiverUtDto.ubrukteRefusjonsopplysninger, generasjon.id, sisteEndring.refusjonstidslinje),
            pensjonsgivendeInntekter = (sisteEndring.faktaavklartInntekt as? SelvstendigFaktaavklartInntektUtDto)?.pensjonsgivendeInntekter ?: emptyList(),
            annulleringskandidater = vedtaksperiode.annulleringskandidater
        )
    }

    private fun mapAnnullertPeriode(vedtaksperiode: VedtaksperiodeUtDto, generasjon: BehandlingUtDto): AnnullertPeriode {
        val sisteEndring = generasjon.endringer.last()
        val annulleringen = annulleringer.single { it.id == sisteEndring.utbetalingId }
        return AnnullertPeriode(
            vedtaksperiodeId = vedtaksperiode.id,
            behandlingId = generasjon.id,
            kilde = generasjon.kilde.meldingsreferanseId.id,
            fom = sisteEndring.periode.fom,
            tom = sisteEndring.periode.tom,
            opprettet = generasjon.endringer.first().tidsstempel,
            // feltet gir ikke mening for annullert periode:
            vilkår = BeregnetPeriode.Vilkår(
                sykepengedager = BeregnetPeriode.Sykepengedager(sisteEndring.periode.fom, LocalDate.MAX, null, null, false),
                alder = alder.let {
                    val alderSisteSykedag = it.alderPåDato(sisteEndring.periode.tom)
                    BeregnetPeriode.Alder(alderSisteSykedag, alderSisteSykedag < 70)
                }
            ),
            beregnet = annulleringen.annulleringstidspunkt,
            oppdatert = vedtaksperiode.oppdatert,
            periodetilstand = annulleringen.periodetilstand,
            hendelser = dokumenterTilOgMedDenneGenerasjonen(vedtaksperiode, generasjon),
            beregningId = annulleringen.id,
            pensjonsgivendeInntekter = (sisteEndring.faktaavklartInntekt as? SelvstendigFaktaavklartInntektUtDto)?.pensjonsgivendeInntekter ?: emptyList(),
            utbetaling = Utbetaling(
                annulleringen.id,
                Utbetalingtype.ANNULLERING,
                annulleringen.utbetalingstatus,
                0,
                0,
                annulleringen.arbeidsgiverFagsystemId,
                annulleringen.personFagsystemId,
                emptyMap(),
                null
            )
        )
    }

    private fun dokumenterTilOgMedDenneGenerasjonen(vedtaksperiode: VedtaksperiodeUtDto, generasjon: BehandlingUtDto): Set<UUID> {
        return vedtaksperiode.behandlinger.behandlinger
            .asSequence()
            .takeWhile { it.id != generasjon.id }
            .plus(generasjon)
            .flatMap { it.endringer }
            .map { it.dokumentsporing.id.id }
            .toSet()
    }

    private fun List<Utbetalingstidslinjedag>.sisteNavDag() =
        lastOrNull { it.type == UtbetalingstidslinjedagType.NavDag }

    private fun utledePeriodetilstand(periodetilstand: VedtaksperiodetilstandDto, behandlingtilstandDto: BehandlingtilstandDto, avgrensetUtbetalingstidslinje: List<Utbetalingstidslinjedag>) =
        when (behandlingtilstandDto) {
            BehandlingtilstandDto.REVURDERT_VEDTAK_AVVIST -> Periodetilstand.RevurderingFeilet

            BehandlingtilstandDto.VEDTAK_FATTET -> Periodetilstand.TilUtbetaling

            BehandlingtilstandDto.VEDTAK_IVERKSATT -> when {
                avgrensetUtbetalingstidslinje.none { it.utbetalingsinfo()?.harUtbetaling() == true } -> Periodetilstand.IngenUtbetaling
                else -> Periodetilstand.Utbetalt
            }

            BehandlingtilstandDto.BEREGNET,
            BehandlingtilstandDto.BEREGNET_OMGJØRING,
            BehandlingtilstandDto.BEREGNET_REVURDERING -> when (periodetilstand) {
                VedtaksperiodetilstandDto.AVVENTER_GODKJENNING_REVURDERING,
                VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_GODKJENNING,
                VedtaksperiodetilstandDto.AVVENTER_GODKJENNING -> Periodetilstand.TilGodkjenning

                VedtaksperiodetilstandDto.AVVENTER_HISTORIKK_REVURDERING,
                VedtaksperiodetilstandDto.AVVENTER_SIMULERING,
                VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_SIMULERING,
                VedtaksperiodetilstandDto.AVVENTER_HISTORIKK,
                VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_HISTORIKK,
                VedtaksperiodetilstandDto.AVVENTER_SIMULERING_REVURDERING -> Periodetilstand.ForberederGodkjenning

                VedtaksperiodetilstandDto.AVVENTER_REVURDERING -> Periodetilstand.UtbetaltVenterPåAnnenPeriode // flere AG; en annen AG har laget utbetaling på vegne av *denne* (revurdering)

                VedtaksperiodetilstandDto.AVVENTER_BLOKKERENDE_PERIODE,
                VedtaksperiodetilstandDto.AVVENTER_SØKNAD_FOR_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODE,
                VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE,
                VedtaksperiodetilstandDto.AVVENTER_INNTEKTSMELDING -> Periodetilstand.VenterPåAnnenPeriode // flere AG; en annen AG har laget utbetaling på vegne av *denne* (førstegangsvurdering)

                VedtaksperiodetilstandDto.SELVSTENDIG_TIL_UTBETALING,
                VedtaksperiodetilstandDto.TIL_UTBETALING -> Periodetilstand.TilUtbetaling

                VedtaksperiodetilstandDto.AVSLUTTET,
                VedtaksperiodetilstandDto.SELVSTENDIG_AVSLUTTET -> Periodetilstand.Utbetalt

                VedtaksperiodetilstandDto.FRILANS_START,
                VedtaksperiodetilstandDto.FRILANS_AVVENTER_INFOTRYGDHISTORIKK,
                VedtaksperiodetilstandDto.FRILANS_AVVENTER_BLOKKERENDE_PERIODE,

                VedtaksperiodetilstandDto.ARBEIDSLEDIG_START,
                VedtaksperiodetilstandDto.ARBEIDSLEDIG_AVVENTER_INFOTRYGDHISTORIKK,
                VedtaksperiodetilstandDto.ARBEIDSLEDIG_AVVENTER_BLOKKERENDE_PERIODE,

                VedtaksperiodetilstandDto.AVSLUTTET_UTEN_UTBETALING,
                VedtaksperiodetilstandDto.AVVENTER_AVSLUTTET_UTEN_UTBETALING,
                VedtaksperiodetilstandDto.AVVENTER_INFOTRYGDHISTORIKK,
                VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK,
                VedtaksperiodetilstandDto.AVVENTER_VILKÅRSPRØVING,
                VedtaksperiodetilstandDto.AVVENTER_A_ORDNINGEN,
                VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_VILKÅRSPRØVING,
                VedtaksperiodetilstandDto.AVVENTER_VILKÅRSPRØVING_REVURDERING,
                VedtaksperiodetilstandDto.ARBEIDSTAKER_START,
                VedtaksperiodetilstandDto.SELVSTENDIG_START,
                VedtaksperiodetilstandDto.TIL_INFOTRYGD,
                VedtaksperiodetilstandDto.AVVENTER_ANNULLERING,
                VedtaksperiodetilstandDto.TIL_ANNULLERING -> error("har ikke mappingregel for utbetalingstatus $behandlingtilstandDto og periodetilstand=$periodetilstand")
            }

            else -> error("har ikke mappingregel for $behandlingtilstandDto")
        }

    private fun periodevilkår(
        sisteSykepengedag: LocalDate,
        maksdatoresultat: MaksdatoresultatUtDto,
        alder: AlderDTO,
        skjæringstidspunkt: LocalDate
    ): BeregnetPeriode.Vilkår {
        val sykepengedager = BeregnetPeriode.Sykepengedager(
            skjæringstidspunkt,
            maksdatoresultat.maksdato,
            maksdatoresultat.forbrukteDager.antallDager(),
            maksdatoresultat.gjenståendeDager,
            maksdatoresultat.maksdato > sisteSykepengedag
        )
        val alderSisteSykepengedag = alder.alderPåDato(sisteSykepengedag).let {
            BeregnetPeriode.Alder(it, it < 70)
        }
        return BeregnetPeriode.Vilkår(sykepengedager, alderSisteSykepengedag)
    }

    private fun mapUtbetalinger(): List<Utbetaling> {
        return arbeidsgiverUtDto.utbetalinger
            .filter { it.type in setOf(UtbetalingtypeDto.REVURDERING, UtbetalingtypeDto.UTBETALING) }
            .mapNotNull {
                Utbetaling(
                    id = it.id,
                    type = when (it.type) {
                        UtbetalingtypeDto.REVURDERING -> Utbetalingtype.REVURDERING
                        UtbetalingtypeDto.UTBETALING -> Utbetalingtype.UTBETALING
                        else -> error("Forventer ikke mapping for utbetalingtype=${it.type}")
                    },
                    status = when (it.tilstand) {
                        UtbetalingTilstandDto.ANNULLERT -> Utbetalingstatus.Annullert
                        UtbetalingTilstandDto.GODKJENT_UTEN_UTBETALING -> Utbetalingstatus.GodkjentUtenUtbetaling
                        UtbetalingTilstandDto.IKKE_GODKJENT -> Utbetalingstatus.IkkeGodkjent
                        UtbetalingTilstandDto.IKKE_UTBETALT -> Utbetalingstatus.Ubetalt
                        UtbetalingTilstandDto.OVERFØRT -> Utbetalingstatus.Overført
                        UtbetalingTilstandDto.UTBETALT -> Utbetalingstatus.Utbetalt
                        else -> return@mapNotNull null
                    },
                    arbeidsgiverNettoBeløp = it.arbeidsgiverOppdrag.nettoBeløp,
                    arbeidsgiverFagsystemId = it.arbeidsgiverOppdrag.fagsystemId,
                    personNettoBeløp = it.personOppdrag.nettoBeløp,
                    personFagsystemId = it.personOppdrag.fagsystemId,
                    oppdrag = mapOf(
                        it.arbeidsgiverOppdrag.fagsystemId to mapOppdrag(it.arbeidsgiverOppdrag),
                        it.personOppdrag.fagsystemId to mapOppdrag(it.personOppdrag),
                    ),
                    vurdering = it.vurdering?.let { vurdering ->
                        Utbetaling.Vurdering(
                            godkjent = vurdering.godkjent,
                            tidsstempel = vurdering.tidspunkt,
                            automatisk = vurdering.automatiskBehandling,
                            ident = vurdering.ident
                        )
                    }
                )
            }
    }

    private fun mapAnnulleringer(): List<AnnullertUtbetaling> {
        return arbeidsgiverUtDto.utbetalinger
            .filter { it.type == UtbetalingtypeDto.ANNULLERING }
            .mapNotNull {
                AnnullertUtbetaling(
                    id = it.id,
                    korrelasjonsId = it.korrelasjonsId,
                    annulleringstidspunkt = it.tidsstempel,
                    arbeidsgiverFagsystemId = it.personOppdrag.fagsystemId,
                    personFagsystemId = it.personOppdrag.fagsystemId,
                    utbetalingstatus = when (it.tilstand) {
                        UtbetalingTilstandDto.ANNULLERT -> Utbetalingstatus.Annullert
                        UtbetalingTilstandDto.GODKJENT_UTEN_UTBETALING -> Utbetalingstatus.GodkjentUtenUtbetaling
                        UtbetalingTilstandDto.IKKE_GODKJENT -> Utbetalingstatus.IkkeGodkjent
                        UtbetalingTilstandDto.IKKE_UTBETALT -> Utbetalingstatus.Ubetalt
                        UtbetalingTilstandDto.OVERFØRT -> Utbetalingstatus.Overført
                        UtbetalingTilstandDto.UTBETALT -> Utbetalingstatus.Utbetalt
                        UtbetalingTilstandDto.FORKASTET -> Utbetalingstatus.Annullert
                        UtbetalingTilstandDto.NY -> return@mapNotNull null
                    }
                )
            }
    }

    private fun mapOppdrag(dto: OppdragUtDto): SpeilOppdrag {
        return SpeilOppdrag(
            fagsystemId = dto.fagsystemId,
            tidsstempel = dto.tidsstempel,
            nettobeløp = dto.nettoBeløp,
            simulering = dto.simuleringsResultat?.let {
                SpeilOppdrag.Simulering(
                    totalbeløp = it.totalbeløp,
                    perioder = it.perioder.map {
                        SpeilOppdrag.Simuleringsperiode(
                            fom = it.fom,
                            tom = it.tom,
                            utbetalinger = it.utbetalinger.map {
                                SpeilOppdrag.Simuleringsutbetaling(
                                    mottakerNavn = it.utbetalesTil.navn,
                                    mottakerId = it.utbetalesTil.id,
                                    forfall = it.forfallsdato,
                                    feilkonto = it.feilkonto,
                                    detaljer = it.detaljer.map {
                                        SpeilOppdrag.Simuleringsdetaljer(
                                            faktiskFom = it.fom,
                                            faktiskTom = it.tom,
                                            konto = it.konto,
                                            beløp = it.beløp,
                                            tilbakeføring = it.tilbakeføring,
                                            sats = it.sats.sats,
                                            typeSats = it.sats.type,
                                            antallSats = it.sats.antall,
                                            uføregrad = it.uføregrad,
                                            klassekode = it.klassekode.kode,
                                            klassekodeBeskrivelse = it.klassekode.beskrivelse,
                                            utbetalingstype = it.utbetalingstype,
                                            refunderesOrgNr = it.refunderesOrgnummer
                                        )
                                    }
                                )
                            }
                        )
                    }
                )
            }
        )
    }

    private fun buildSpekemat(): List<SpeilGenerasjonDTO> {
        val generasjoner = mapPerioder()
        return pølsepakke.rader
            .fjernUnødvendigeRader()
            .map { rad -> mapRadTilSpeilGenerasjon(rad, generasjoner) }
            .filterNot { rad -> rad.size == 0 } // fjerner tomme rader
    }

    private fun mapRadTilSpeilGenerasjon(rad: SpekematDTO.PølsepakkeDTO.PølseradDTO, generasjoner: List<SpeilTidslinjeperiode>): SpeilGenerasjonDTO {
        val perioder = rad.pølser.mapNotNull { pølse -> generasjoner.firstOrNull { it.behandlingId == pølse.behandlingId } }
        return SpeilGenerasjonDTO(
            id = UUID.randomUUID(),
            kildeTilGenerasjon = rad.kildeTilRad,
            perioder = perioder
                .map { it.registrerBruk(vilkårsgrunnlaghistorikk, organisasjonsnummer) }
                .utledPeriodetyper()
        )
    }

    companion object {
        private fun mapRefusjonstidslinje(ubrukteRefusjonsopplysninger: UbrukteRefusjonsopplysningerUtDto, behandlingId: UUID, refusjonstidslinje: BeløpstidslinjeDto): BeløpstidslinjeDto {
            if (ubrukteRefusjonsopplysninger.sisteBehandlingId != behandlingId) return refusjonstidslinje
            return ubrukteRefusjonsopplysninger.sisteRefusjonstidslinje!!
        }

        private fun Map<LocalDate, List<VedtaksperiodeUtDto>>.tilRefusjonstidslinjer(ubrukteRefusjonsopplysninger: UbrukteRefusjonsopplysningerUtDto) = mapValues { (_, perioder) ->
            perioder.map { periode ->
                periode.behandlinger.behandlinger.last().let { sisteBehandling ->
                    mapRefusjonstidslinje(ubrukteRefusjonsopplysninger, sisteBehandling.id, sisteBehandling.endringer.last().refusjonstidslinje)
                }
            }
        }

        private fun Map<LocalDate, List<BeløpstidslinjeDto>>.slåSammenSammenhengendeRefusjonstidslinjer() = mapValues { (_, beløpstidslinjer) ->
            beløpstidslinjer.flatMap { it.perioder }.fold(emptyList<BeløpstidslinjeperiodeDto>()) { resultat, periode ->
                when {
                    resultat.isEmpty() -> listOf(periode)
                    resultat.last().kanUtvidesForSpeil(periode) -> {
                        resultat.dropLast(1) + resultat.last().copy(tom = periode.tom)
                    }

                    else -> resultat + periode
                }
            }
        }

        private fun BeløpstidslinjeperiodeDto.kanUtvidesForSpeil(other: BeløpstidslinjeperiodeDto) =
            this.tom.plusDays(1) == other.fom &&
                this.dagligBeløp == other.dagligBeløp

        private fun Map<LocalDate, List<BeløpstidslinjeperiodeDto>>.tilRefusjonselementerUtenGapOgÅpenHale() = mapValues { (_, beløpstidslinjeperioder) ->
            beløpstidslinjeperioder.mapWithNext { nåværende, neste ->
                Refusjonselement(
                    fom = nåværende.fom,
                    tom = neste?.fom?.forrigeDag,
                    beløp = nåværende.dagligBeløp.daglig.månedlig,
                    meldingsreferanseId = nåværende.kilde.meldingsreferanseId.id
                )
            }
        }
    }
}

private fun Collection<PeriodeDto>.antallDager() = sumOf { ChronoUnit.DAYS.between(it.fom, it.tom.plusDays(1)) }.toInt()
