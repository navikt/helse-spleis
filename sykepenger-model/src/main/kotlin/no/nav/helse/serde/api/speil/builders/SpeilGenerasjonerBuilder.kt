package no.nav.helse.serde.api.speil.builders

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Alder
import no.nav.helse.dto.EndringskodeDto
import no.nav.helse.dto.GenerasjonTilstandDto
import no.nav.helse.dto.UtbetalingTilstandDto
import no.nav.helse.dto.UtbetalingtypeDto
import no.nav.helse.dto.VedtaksperiodetilstandDto
import no.nav.helse.dto.serialisering.ArbeidsgiverUtDto
import no.nav.helse.dto.serialisering.GenerasjonUtDto
import no.nav.helse.dto.serialisering.OppdragUtDto
import no.nav.helse.dto.serialisering.VedtaksperiodeUtDto
import no.nav.helse.person.aktivitetslogg.UtbetalingInntektskilde
import no.nav.helse.serde.api.SpekematDTO
import no.nav.helse.serde.api.dto.AnnullertPeriode
import no.nav.helse.serde.api.dto.AnnullertUtbetaling
import no.nav.helse.serde.api.dto.BeregnetPeriode
import no.nav.helse.serde.api.dto.EndringskodeDTO
import no.nav.helse.serde.api.dto.Periodetilstand
import no.nav.helse.serde.api.dto.SpeilGenerasjonDTO
import no.nav.helse.serde.api.dto.SpeilOppdrag
import no.nav.helse.serde.api.dto.SpeilTidslinjeperiode
import no.nav.helse.serde.api.dto.SpeilTidslinjeperiode.Companion.utledPeriodetyper
import no.nav.helse.serde.api.dto.Tidslinjeperiodetype
import no.nav.helse.serde.api.dto.UberegnetPeriode
import no.nav.helse.serde.api.dto.Utbetalingstidslinjedag
import no.nav.helse.serde.api.dto.UtbetalingstidslinjedagType
import no.nav.helse.serde.api.speil.builders.ArbeidsgiverBuilder.Companion.fjernUnødvendigeRader
import no.nav.helse.serde.api.speil.merge

internal class SpeilGenerasjonerBuilder(
    private val organisasjonsnummer: String,
    private val alder: Alder,
    private val arbeidsgiverUtDto: ArbeidsgiverUtDto,
    private val vilkårsgrunnlaghistorikk: IVilkårsgrunnlagHistorikk,
    private val pølsepakke: SpekematDTO.PølsepakkeDTO
) {
    private val utbetalinger = mapUtbetalinger()
    private val annulleringer = mapAnnulleringer()
    private val utbetalingstidslinjer = mapUtbetalingstidslinjer()

    private val allePerioder = mapPerioder()

    internal fun build(): List<SpeilGenerasjonDTO> {
        return buildSpekemat()
    }

    private fun mapPerioder(): List<SpeilTidslinjeperiode> {
        val aktive = arbeidsgiverUtDto.vedtaksperioder.flatMap { mapVedtaksperiode(false, it) }
        val forkastede = arbeidsgiverUtDto.forkastede.flatMap { mapVedtaksperiode(true, it.vedtaksperiode) }
        return aktive + forkastede
    }
    private fun mapVedtaksperiode(erForkastet: Boolean, vedtaksperiode: VedtaksperiodeUtDto): List<SpeilTidslinjeperiode> {
        var forrigeGenerasjon: SpeilTidslinjeperiode? = null
        return vedtaksperiode.generasjoner.generasjoner.mapNotNull { generasjon ->
            when (generasjon.tilstand) {
                GenerasjonTilstandDto.BEREGNET,
                GenerasjonTilstandDto.BEREGNET_OMGJØRING,
                GenerasjonTilstandDto.BEREGNET_REVURDERING,
                GenerasjonTilstandDto.REVURDERT_VEDTAK_AVVIST,
                GenerasjonTilstandDto.VEDTAK_FATTET,
                GenerasjonTilstandDto.VEDTAK_IVERKSATT -> mapBeregnetPeriode(vedtaksperiode, generasjon)
                GenerasjonTilstandDto.UBEREGNET,
                GenerasjonTilstandDto.UBEREGNET_OMGJØRING,
                GenerasjonTilstandDto.UBEREGNET_REVURDERING,
                GenerasjonTilstandDto.AVSLUTTET_UTEN_VEDTAK -> mapUberegnetPeriode(erForkastet, vedtaksperiode, generasjon)
                GenerasjonTilstandDto.TIL_INFOTRYGD -> when (val forrige = forrigeGenerasjon) {
                    // todo: må mappe TIL_INFOTRYGD som annullert periode så lenge annullerte perioder ikke har link til annulleringutbetalingen
                    is BeregnetPeriode -> mapAnnullertPeriode(vedtaksperiode, forrige, generasjon)
                    else -> mapTilInfotrygdperiode(vedtaksperiode, forrige, generasjon)
                }
                GenerasjonTilstandDto.ANNULLERT_PERIODE -> mapAnnullertPeriode(vedtaksperiode, null, generasjon)
            }.also {
                forrigeGenerasjon = it
            }
        }
    }

    private fun mapTilInfotrygdperiode(vedtaksperiode: VedtaksperiodeUtDto, forrigePeriode: SpeilTidslinjeperiode?, generasjon: GenerasjonUtDto): UberegnetPeriode? {
        if (forrigePeriode == null) return null // todo: her kan vi mappe perioder som tas ut av Speil
        return mapUberegnetPeriode(true, vedtaksperiode, generasjon, Periodetilstand.Annullert)
    }

    private fun mapUberegnetPeriode(erForkastet: Boolean, vedtaksperiode: VedtaksperiodeUtDto, generasjon: GenerasjonUtDto, periodetilstand: Periodetilstand? = null): UberegnetPeriode {
        val sisteEndring = generasjon.endringer.last()
        val sykdomstidslinje = SykdomstidslinjeBuilder(sisteEndring.sykdomstidslinje).build()
        return UberegnetPeriode(
            vedtaksperiodeId = vedtaksperiode.id,
            generasjonId = generasjon.id,
            kilde = generasjon.kilde.meldingsreferanseId,
            fom = sisteEndring.periode.fom,
            tom = sisteEndring.periode.tom,
            sammenslåttTidslinje = sykdomstidslinje.merge(emptyList()),
            periodetype = Tidslinjeperiodetype.FØRSTEGANGSBEHANDLING, // feltet gir ikke mening for uberegnede perioder
            inntektskilde = UtbetalingInntektskilde.EN_ARBEIDSGIVER, // feltet gir ikke mening for uberegnede perioder
            erForkastet = erForkastet,
            sorteringstidspunkt = generasjon.endringer.first().tidsstempel,
            opprettet = generasjon.endringer.first().tidsstempel,
            oppdatert = sisteEndring.tidsstempel,
            skjæringstidspunkt = vedtaksperiode.skjæringstidspunkt,
            hendelser = dokumenterTilOgMedDenneGenerasjonen(vedtaksperiode, generasjon),
            periodetilstand = periodetilstand ?: generasjon.avsluttet?.let { Periodetilstand.IngenUtbetaling } ?: when (vedtaksperiode.tilstand) {
                is VedtaksperiodetilstandDto.AVVENTER_REVURDERING -> Periodetilstand.UtbetaltVenterPåAnnenPeriode
                is VedtaksperiodetilstandDto.AVVENTER_BLOKKERENDE_PERIODE -> Periodetilstand.VenterPåAnnenPeriode

                is VedtaksperiodetilstandDto.AVVENTER_HISTORIKK,
                is VedtaksperiodetilstandDto.AVVENTER_HISTORIKK_REVURDERING,
                is VedtaksperiodetilstandDto.AVVENTER_VILKÅRSPRØVING,
                is VedtaksperiodetilstandDto.AVVENTER_VILKÅRSPRØVING_REVURDERING -> Periodetilstand.ForberederGodkjenning

                is VedtaksperiodetilstandDto.AVVENTER_INNTEKTSMELDING,
                is VedtaksperiodetilstandDto.AVVENTER_INFOTRYGDHISTORIKK -> Periodetilstand.ManglerInformasjon
                else -> error("Forventer ikke mappingregel for ${vedtaksperiode.tilstand}")
            }

        )
    }
    private fun mapBeregnetPeriode(vedtaksperiode: VedtaksperiodeUtDto, generasjon: GenerasjonUtDto): BeregnetPeriode {
        val sisteEndring = generasjon.endringer.last()
        val utbetaling = utbetalinger.single { it.id == sisteEndring.utbetalingId }
        val utbetalingstidslinje = utbetalingstidslinjer.single { it.first == sisteEndring.utbetalingId }.second.build()
        val avgrensetUtbetalingstidslinje = utbetalingstidslinje.filter { it.dato in sisteEndring.periode.fom..sisteEndring.periode.tom }
        val skjæringstidspunkt = sisteEndring.skjæringstidspunkt!!
        val sisteSykepengedag = avgrensetUtbetalingstidslinje.sisteNavDag()?.dato ?: sisteEndring.periode.tom
        val sykdomstidslinje = SykdomstidslinjeBuilder(sisteEndring.sykdomstidslinje).build()
        return BeregnetPeriode(
            vedtaksperiodeId = vedtaksperiode.id,
            generasjonId = generasjon.id,
            kilde = generasjon.kilde.meldingsreferanseId,
            fom = sisteEndring.periode.fom,
            tom = sisteEndring.periode.tom,
            sammenslåttTidslinje = sykdomstidslinje.merge(utbetalingstidslinje),
            erForkastet = false,
            periodetype = Tidslinjeperiodetype.FØRSTEGANGSBEHANDLING, // TODO: fikse
            inntektskilde = UtbetalingInntektskilde.EN_ARBEIDSGIVER, // verdien av feltet brukes ikke i speil
            opprettet = vedtaksperiode.opprettet,
            generasjonOpprettet = generasjon.endringer.first().tidsstempel,
            oppdatert = vedtaksperiode.oppdatert,
            periodetilstand = utledePeriodetilstand(vedtaksperiode.tilstand, utbetaling, avgrensetUtbetalingstidslinje),
            skjæringstidspunkt = skjæringstidspunkt,
            hendelser = dokumenterTilOgMedDenneGenerasjonen(vedtaksperiode, generasjon),
            beregningId = utbetaling.id,
            utbetaling = utbetaling,
            periodevilkår = periodevilkår(sisteSykepengedag, utbetaling, alder, skjæringstidspunkt),
            vilkårsgrunnlagId = sisteEndring.vilkårsgrunnlagId!!,
            forrigeGenerasjon = null
        )
    }

    private fun mapAnnullertPeriode(vedtaksperiode: VedtaksperiodeUtDto, forrigeBeregnetPeriode: BeregnetPeriode?, generasjon: GenerasjonUtDto): AnnullertPeriode? {
        val sisteEndring = generasjon.endringer.last()
        // todo: når alle annullerte generasjoner har en endring som peker på den annullerte utbetalingen så kan vi hente
        // ut annulleringen vha: `annulleringer.single { it.id == sisteEndring.utbetalingId }`
        val annulleringen = annulleringer.singleOrNull { it.id == sisteEndring.utbetalingId }
            ?: forrigeBeregnetPeriode?.let { annulleringer.singleOrNull { it.annullerer(forrigeBeregnetPeriode.utbetaling.korrelasjonsId) } }
            ?: return null // todo: Forventer å finne en annullering for vedtaksperioden. Det er flere vedtaksperioder som er forkastet, som ikke skulle vært der fordi de ikke er annullert på ekte
        return AnnullertPeriode(
            vedtaksperiodeId = vedtaksperiode.id,
            generasjonId = generasjon.id,
            kilde = generasjon.kilde.meldingsreferanseId,
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
            utbetaling = no.nav.helse.serde.api.dto.Utbetaling(
                annulleringen.id,
                no.nav.helse.serde.api.dto.Utbetalingtype.ANNULLERING,
                annulleringen.korrelasjonsId,
                LocalDate.MAX,
                0,
                0,
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

    private fun dokumenterTilOgMedDenneGenerasjonen(vedtaksperiode: VedtaksperiodeUtDto, generasjon: GenerasjonUtDto): Set<UUID> {
        return vedtaksperiode.generasjoner.generasjoner
            .asSequence()
            .takeWhile { it.id != generasjon.id }
            .plus(generasjon)
            .flatMap { it.endringer }
            .map { it.dokumentsporing.id }
            .toSet()
    }

    private fun List<Utbetalingstidslinjedag>.sisteNavDag() =
        lastOrNull { it.type == UtbetalingstidslinjedagType.NavDag }

    private fun utledePeriodetilstand(periodetilstand: VedtaksperiodetilstandDto, utbetalingDTO: no.nav.helse.serde.api.dto.Utbetaling, avgrensetUtbetalingstidslinje: List<Utbetalingstidslinjedag>) =
        when (utbetalingDTO.status) {
            no.nav.helse.serde.api.dto.Utbetalingstatus.IkkeGodkjent -> Periodetilstand.RevurderingFeilet
            no.nav.helse.serde.api.dto.Utbetalingstatus.Utbetalt -> when {
                avgrensetUtbetalingstidslinje.none { it.utbetalingsinfo()?.harUtbetaling() == true } -> Periodetilstand.IngenUtbetaling
                else -> Periodetilstand.Utbetalt
            }
            no.nav.helse.serde.api.dto.Utbetalingstatus.Ubetalt -> when {
                periodetilstand in setOf(VedtaksperiodetilstandDto.AVVENTER_GODKJENNING_REVURDERING, VedtaksperiodetilstandDto.AVVENTER_GODKJENNING) -> Periodetilstand.TilGodkjenning
                periodetilstand in setOf(VedtaksperiodetilstandDto.AVVENTER_HISTORIKK_REVURDERING, VedtaksperiodetilstandDto.AVVENTER_SIMULERING, VedtaksperiodetilstandDto.AVVENTER_SIMULERING_REVURDERING) -> Periodetilstand.ForberederGodkjenning
                periodetilstand in setOf(VedtaksperiodetilstandDto.AVVENTER_HISTORIKK) -> Periodetilstand.ForberederGodkjenning
                periodetilstand == VedtaksperiodetilstandDto.AVVENTER_REVURDERING -> Periodetilstand.UtbetaltVenterPåAnnenPeriode // flere AG; en annen AG har laget utbetaling på vegne av *denne* (revurdering)
                periodetilstand == VedtaksperiodetilstandDto.AVVENTER_BLOKKERENDE_PERIODE -> Periodetilstand.VenterPåAnnenPeriode // flere AG; en annen AG har laget utbetaling på vegne av *denne* (førstegangsvurdering)
                else -> error("har ikke mappingregel for utbetalingstatus ${utbetalingDTO.status} og periodetilstand=$periodetilstand")
            }
            no.nav.helse.serde.api.dto.Utbetalingstatus.GodkjentUtenUtbetaling -> when {
                utbetalingDTO.type == no.nav.helse.serde.api.dto.Utbetalingtype.REVURDERING -> Periodetilstand.Utbetalt
                else -> Periodetilstand.IngenUtbetaling
            }
            no.nav.helse.serde.api.dto.Utbetalingstatus.Godkjent,
            no.nav.helse.serde.api.dto.Utbetalingstatus.Overført -> Periodetilstand.TilUtbetaling
            else -> error("har ikke mappingregel for ${utbetalingDTO.status}")
        }

    private fun periodevilkår(
        sisteSykepengedag: LocalDate,
        utbetaling: no.nav.helse.serde.api.dto.Utbetaling,
        alder: Alder,
        skjæringstidspunkt: LocalDate
    ): BeregnetPeriode.Vilkår {
        val sykepengedager = BeregnetPeriode.Sykepengedager(
            skjæringstidspunkt,
            utbetaling.maksdato,
            utbetaling.forbrukteSykedager,
            utbetaling.gjenståendeDager,
            utbetaling.maksdato > sisteSykepengedag
        )
        val alderSisteSykepengedag = alder.let {
            val alderSisteSykedag = it.alderPåDato(sisteSykepengedag)
            BeregnetPeriode.Alder(alderSisteSykedag, alderSisteSykedag < 70)
        }
        return BeregnetPeriode.Vilkår(sykepengedager, alderSisteSykepengedag)
    }

    private fun mapUtbetalingstidslinjer(): List<Pair<UUID, UtbetalingstidslinjeBuilder>> {
        return arbeidsgiverUtDto.utbetalinger.map {
            it.id to UtbetalingstidslinjeBuilder(it.utbetalingstidslinje)
        }
    }

    private fun mapUtbetalinger(): List<no.nav.helse.serde.api.dto.Utbetaling> {
        return arbeidsgiverUtDto.utbetalinger
            .filter { it.type in setOf(UtbetalingtypeDto.REVURDERING, UtbetalingtypeDto.UTBETALING) }
            .mapNotNull {
                no.nav.helse.serde.api.dto.Utbetaling(
                    id = it.id,
                    type = when (it.type) {
                        UtbetalingtypeDto.REVURDERING -> no.nav.helse.serde.api.dto.Utbetalingtype.REVURDERING
                        UtbetalingtypeDto.UTBETALING -> no.nav.helse.serde.api.dto.Utbetalingtype.UTBETALING
                        else -> error("Forventer ikke mapping for utbetalingtype=${it.type}")
                    },
                    korrelasjonsId = it.korrelasjonsId,
                    status = when (it.tilstand) {
                        UtbetalingTilstandDto.ANNULLERT -> no.nav.helse.serde.api.dto.Utbetalingstatus.Annullert
                        UtbetalingTilstandDto.GODKJENT -> no.nav.helse.serde.api.dto.Utbetalingstatus.Godkjent
                        UtbetalingTilstandDto.GODKJENT_UTEN_UTBETALING -> no.nav.helse.serde.api.dto.Utbetalingstatus.GodkjentUtenUtbetaling
                        UtbetalingTilstandDto.IKKE_GODKJENT -> no.nav.helse.serde.api.dto.Utbetalingstatus.IkkeGodkjent
                        UtbetalingTilstandDto.IKKE_UTBETALT -> no.nav.helse.serde.api.dto.Utbetalingstatus.Ubetalt
                        UtbetalingTilstandDto.OVERFØRT -> no.nav.helse.serde.api.dto.Utbetalingstatus.Overført
                        UtbetalingTilstandDto.UTBETALT -> no.nav.helse.serde.api.dto.Utbetalingstatus.Utbetalt
                        else -> return@mapNotNull null
                    },
                    maksdato = it.maksdato,
                    forbrukteSykedager = it.forbrukteSykedager!!,
                    gjenståendeDager = it.gjenståendeSykedager!!,
                    arbeidsgiverNettoBeløp = it.arbeidsgiverOppdrag.nettoBeløp,
                    arbeidsgiverFagsystemId = it.arbeidsgiverOppdrag.fagsystemId,
                    personNettoBeløp = it.personOppdrag.nettoBeløp,
                    personFagsystemId = it.personOppdrag.fagsystemId,
                    oppdrag = mapOf(
                        it.arbeidsgiverOppdrag.fagsystemId to mapOppdrag(it.arbeidsgiverOppdrag),
                        it.personOppdrag.fagsystemId to mapOppdrag(it.personOppdrag),
                    ),
                    vurdering = it.vurdering?.let { vurdering ->
                        no.nav.helse.serde.api.dto.Utbetaling.Vurdering(
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
                        UtbetalingTilstandDto.ANNULLERT -> no.nav.helse.serde.api.dto.Utbetalingstatus.Annullert
                        UtbetalingTilstandDto.GODKJENT -> no.nav.helse.serde.api.dto.Utbetalingstatus.Godkjent
                        UtbetalingTilstandDto.GODKJENT_UTEN_UTBETALING -> no.nav.helse.serde.api.dto.Utbetalingstatus.GodkjentUtenUtbetaling
                        UtbetalingTilstandDto.IKKE_GODKJENT -> no.nav.helse.serde.api.dto.Utbetalingstatus.IkkeGodkjent
                        UtbetalingTilstandDto.IKKE_UTBETALT -> no.nav.helse.serde.api.dto.Utbetalingstatus.Ubetalt
                        UtbetalingTilstandDto.OVERFØRT -> no.nav.helse.serde.api.dto.Utbetalingstatus.Overført
                        UtbetalingTilstandDto.UTBETALT -> no.nav.helse.serde.api.dto.Utbetalingstatus.Utbetalt
                        else -> return@mapNotNull null
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
            },
            utbetalingslinjer = dto.linjer.map { linje ->
                SpeilOppdrag.Utbetalingslinje(
                    fom = linje.fom,
                    tom = linje.tom,
                    dagsats = linje.beløp,
                    grad = linje.grad!!,
                    endringskode = when (linje.endringskode) {
                        EndringskodeDto.ENDR -> EndringskodeDTO.ENDR
                        EndringskodeDto.NY -> EndringskodeDTO.NY
                        EndringskodeDto.UEND -> EndringskodeDTO.UEND
                    }
                )
            }
        )
    }

    private fun buildSpekemat(): List<SpeilGenerasjonDTO> {
        val generasjoner = buildTidslinjeperioder()
        return pølsepakke.rader
            .fjernUnødvendigeRader()
            .map { rad -> mapRadTilSpeilGenerasjon(rad, generasjoner) }
            .filterNot { rad -> rad.size == 0 } // fjerner tomme rader
    }

    private fun buildTidslinjeperioder(): List<SpeilTidslinjeperiode> {
        return allePerioder
    }

    private fun mapRadTilSpeilGenerasjon(rad: SpekematDTO.PølsepakkeDTO.PølseradDTO, generasjoner: List<SpeilTidslinjeperiode>): SpeilGenerasjonDTO {
        val perioder = rad.pølser.mapNotNull { pølse -> generasjoner.firstOrNull { it.generasjonId == pølse.behandlingId } }
        return SpeilGenerasjonDTO(
            id = UUID.randomUUID(),
            kildeTilGenerasjon = rad.kildeTilRad,
            perioder = perioder
                .map { it.registrerBruk(vilkårsgrunnlaghistorikk, organisasjonsnummer) }
                .utledPeriodetyper()
        )
    }
}
