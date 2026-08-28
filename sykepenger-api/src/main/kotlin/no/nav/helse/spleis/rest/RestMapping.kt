package no.nav.helse.spleis.rest

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.dto.AnnulleringskandidatDto
import no.nav.helse.dto.serialisering.SelvstendigFaktaavklartInntektUtDto
import no.nav.helse.spleis.dto.HendelseDTO
import no.nav.helse.spleis.dto.HendelsetypeDto
import no.nav.helse.spleis.rest.dto.ApiAnnulleringskandidat
import no.nav.helse.spleis.rest.dto.ApiBegrunnelse
import no.nav.helse.spleis.rest.dto.ApiDag
import no.nav.helse.spleis.rest.dto.ApiHendelse
import no.nav.helse.spleis.rest.dto.ApiInfotrygdVilkarsgrunnlag
import no.nav.helse.spleis.rest.dto.ApiInntektFraAOrdningen
import no.nav.helse.spleis.rest.dto.ApiInntekterFraAOrdningen
import no.nav.helse.spleis.rest.dto.ApiInntektskilde
import no.nav.helse.spleis.rest.dto.ApiInntektsmelding
import no.nav.helse.spleis.rest.dto.ApiOmregnetArsinntekt
import no.nav.helse.spleis.rest.dto.ApiOppdrag
import no.nav.helse.spleis.rest.dto.ApiPensjonsgivendeInntekt
import no.nav.helse.spleis.rest.dto.ApiPeriodevilkar
import no.nav.helse.spleis.rest.dto.ApiRefusjonselement
import no.nav.helse.spleis.rest.dto.ApiSimulering
import no.nav.helse.spleis.rest.dto.ApiSimuleringsdetaljer
import no.nav.helse.spleis.rest.dto.ApiSimuleringsperiode
import no.nav.helse.spleis.rest.dto.ApiSimuleringsutbetaling
import no.nav.helse.spleis.rest.dto.ApiSkjonnsmessigFastsatt
import no.nav.helse.spleis.rest.dto.ApiSoknadArbeidsgiver
import no.nav.helse.spleis.rest.dto.ApiSoknadArbeidsledig
import no.nav.helse.spleis.rest.dto.ApiSoknadFrilans
import no.nav.helse.spleis.rest.dto.ApiSoknadNav
import no.nav.helse.spleis.rest.dto.ApiSoknadSelvstendig
import no.nav.helse.spleis.rest.dto.ApiSpleisVilkarsgrunnlag
import no.nav.helse.spleis.rest.dto.ApiSykdomsdagkilde
import no.nav.helse.spleis.rest.dto.ApiSykdomsdagkildetype
import no.nav.helse.spleis.rest.dto.ApiSykdomsdagtype
import no.nav.helse.spleis.rest.dto.ApiSykepengegrunnlagsgrense
import no.nav.helse.spleis.rest.dto.ApiSykmelding
import no.nav.helse.spleis.rest.dto.ApiUtbetalingsdagType
import no.nav.helse.spleis.rest.dto.ApiUtbetalingsinfo
import no.nav.helse.spleis.rest.dto.ApiVurdering
import no.nav.helse.spleis.speil.builders.SykepengegrunnlagsgrenseDTO
import no.nav.helse.spleis.speil.dto.AnnullertPeriode
import no.nav.helse.spleis.speil.dto.BegrunnelseDTO
import no.nav.helse.spleis.speil.dto.BeregnetPeriode
import no.nav.helse.spleis.speil.dto.InfotrygdVilkårsgrunnlag
import no.nav.helse.spleis.speil.dto.Inntekt
import no.nav.helse.spleis.speil.dto.Inntektkilde
import no.nav.helse.spleis.speil.dto.Periodetilstand
import no.nav.helse.spleis.speil.dto.SammenslåttDag
import no.nav.helse.spleis.speil.dto.SpeilOppdrag
import no.nav.helse.spleis.speil.dto.SpeilTidslinjeperiode
import no.nav.helse.spleis.speil.dto.SpleisVilkårsgrunnlag
import no.nav.helse.spleis.speil.dto.SykdomstidslinjedagKildetype
import no.nav.helse.spleis.speil.dto.SykdomstidslinjedagType
import no.nav.helse.spleis.speil.dto.Tidslinjeperiodetype
import no.nav.helse.spleis.speil.dto.UberegnetPeriode
import no.nav.helse.spleis.speil.dto.Utbetaling
import no.nav.helse.spleis.speil.dto.Utbetalingstatus
import no.nav.helse.spleis.speil.dto.UtbetalingstidslinjedagType
import no.nav.helse.spleis.speil.dto.Utbetalingtype
import no.nav.helse.spleis.speil.dto.Vilkårsgrunnlag
import no.nav.helse.spleis.rest.dto.ApiArbeidsgiverinntekt as ArbeidsgiverinntektDto
import no.nav.helse.spleis.rest.dto.ApiArbeidsgiverrefusjon as ArbeidsgiverrefusjonDto
import no.nav.helse.spleis.rest.dto.ApiBeregnetPeriode as BeregnetPeriodeDto
import no.nav.helse.spleis.rest.dto.ApiPeriodetilstand as PeriodetilstandDto
import no.nav.helse.spleis.rest.dto.ApiPeriodetype as PeriodetypeDto
import no.nav.helse.spleis.rest.dto.ApiUberegnetPeriode as UberegnetPeriodeDto
import no.nav.helse.spleis.rest.dto.ApiUtbetaling as UtbetalingDto
import no.nav.helse.spleis.rest.dto.ApiUtbetalingstatus as UtbetalingstatusDto
import no.nav.helse.spleis.rest.dto.ApiUtbetalingtype as UtbetalingtypeDto
import no.nav.helse.spleis.speil.dto.Arbeidsgiverinntekt as ArbeidsgiverinntektSpeil
import no.nav.helse.spleis.speil.dto.Arbeidsgiverrefusjon as ArbeidsgiverrefusjonSpeil

private fun mapDag(dag: SammenslåttDag) = ApiDag(
    dato = dag.dagen,
    sykdomsdagtype = when (dag.sykdomstidslinjedagtype) {
        SykdomstidslinjedagType.ARBEIDSDAG -> ApiSykdomsdagtype.Arbeidsdag
        SykdomstidslinjedagType.ARBEIDSGIVERDAG -> ApiSykdomsdagtype.Arbeidsgiverdag
        SykdomstidslinjedagType.MELDING_TIL_NAV_DAG -> ApiSykdomsdagtype.MeldingTilNavDag
        SykdomstidslinjedagType.FERIEDAG -> ApiSykdomsdagtype.Feriedag
        SykdomstidslinjedagType.ARBEID_IKKE_GJENOPPTATT_DAG -> ApiSykdomsdagtype.ArbeidIkkeGjenopptattDag
        SykdomstidslinjedagType.FORELDET_SYKEDAG -> ApiSykdomsdagtype.ForeldetSykedag
        SykdomstidslinjedagType.FRISK_HELGEDAG -> ApiSykdomsdagtype.FriskHelgedag
        SykdomstidslinjedagType.PERMISJONSDAG -> ApiSykdomsdagtype.Permisjonsdag
        SykdomstidslinjedagType.SYKEDAG -> ApiSykdomsdagtype.Sykedag
        SykdomstidslinjedagType.SYKEDAG_NAV -> ApiSykdomsdagtype.SykedagNav
        SykdomstidslinjedagType.SYK_HELGEDAG -> ApiSykdomsdagtype.SykHelgedag
        SykdomstidslinjedagType.ANDRE_YTELSER_FORELDREPENGER -> ApiSykdomsdagtype.AndreYtelserForeldrepenger
        SykdomstidslinjedagType.ANDRE_YTELSER_AAP -> ApiSykdomsdagtype.AndreYtelserAap
        SykdomstidslinjedagType.ANDRE_YTELSER_OMSORGSPENGER -> ApiSykdomsdagtype.AndreYtelserOmsorgspenger
        SykdomstidslinjedagType.ANDRE_YTELSER_PLEIEPENGER -> ApiSykdomsdagtype.AndreYtelserPleiepenger
        SykdomstidslinjedagType.ANDRE_YTELSER_SVANGERSKAPSPENGER -> ApiSykdomsdagtype.AndreYtelserSvangerskapspenger
        SykdomstidslinjedagType.ANDRE_YTELSER_OPPLÆRINGSPENGER -> ApiSykdomsdagtype.AndreYtelserOpplaringspenger
        SykdomstidslinjedagType.ANDRE_YTELSER_DAGPENGER -> ApiSykdomsdagtype.AndreYtelserDagpenger
        SykdomstidslinjedagType.UBESTEMTDAG -> ApiSykdomsdagtype.Ubestemtdag
        SykdomstidslinjedagType.AVSLÅTT_MELDING_TIL_NAV_DAG -> ApiSykdomsdagtype.AvslattMeldingTilNavDag
    },
    utbetalingsdagtype = when (dag.utbetalingstidslinjedagtype) {
        UtbetalingstidslinjedagType.ArbeidsgiverperiodeDag -> ApiUtbetalingsdagType.ArbeidsgiverperiodeDag
        UtbetalingstidslinjedagType.NavDag -> ApiUtbetalingsdagType.NavDag
        UtbetalingstidslinjedagType.NavHelgDag -> ApiUtbetalingsdagType.NavHelgDag
        UtbetalingstidslinjedagType.Helgedag -> ApiUtbetalingsdagType.Helgedag
        UtbetalingstidslinjedagType.Arbeidsdag -> ApiUtbetalingsdagType.Arbeidsdag
        UtbetalingstidslinjedagType.Feriedag -> ApiUtbetalingsdagType.Feriedag
        UtbetalingstidslinjedagType.AvvistDag -> ApiUtbetalingsdagType.AvvistDag
        UtbetalingstidslinjedagType.UkjentDag -> ApiUtbetalingsdagType.UkjentDag
        UtbetalingstidslinjedagType.ForeldetDag -> ApiUtbetalingsdagType.ForeldetDag
        UtbetalingstidslinjedagType.Ventetidsdag -> ApiUtbetalingsdagType.Ventetidsdag
    },
    kilde = ApiSykdomsdagkilde(
        id = dag.kilde.id,
        type = when (dag.kilde.type) {
            SykdomstidslinjedagKildetype.Inntektsmelding -> ApiSykdomsdagkildetype.Inntektsmelding
            SykdomstidslinjedagKildetype.Søknad -> ApiSykdomsdagkildetype.Soknad
            SykdomstidslinjedagKildetype.Sykmelding -> ApiSykdomsdagkildetype.Sykmelding
            SykdomstidslinjedagKildetype.Saksbehandler -> ApiSykdomsdagkildetype.Saksbehandler
            SykdomstidslinjedagKildetype.Ukjent -> ApiSykdomsdagkildetype.Ukjent
        }
    ),
    grad = dag.grad?.toDouble(),
    utbetalingsinfo = dag.utbetalingsinfo?.let {
        ApiUtbetalingsinfo(
            inntekt = null, // deprecated: speil bruker ikke denne verdien
            utbetaling = it.arbeidsgiverbeløp, // deprecated: verdien settes til det samme som arbeidsgiverbeløp
            personbelop = it.personbeløp,
            arbeidsgiverbelop = it.arbeidsgiverbeløp,
            refusjonsbelop = null, // deprecated: speil bruker ikke denne verdien
            totalGrad = it.totalGrad // double er deprecated: speil forventer float, men runder ned før visning
        )
    },
    begrunnelser = dag.begrunnelser?.map {
        when (it) {
            BegrunnelseDTO.SykepengedagerOppbrukt -> ApiBegrunnelse.SykepengedagerOppbrukt
            BegrunnelseDTO.SykepengedagerOppbruktOver67 -> ApiBegrunnelse.SykepengedagerOppbruktOver67
            BegrunnelseDTO.MinimumInntekt -> ApiBegrunnelse.MinimumInntekt
            BegrunnelseDTO.MinimumInntektOver67 -> ApiBegrunnelse.MinimumInntektOver67
            BegrunnelseDTO.EgenmeldingUtenforArbeidsgiverperiode -> ApiBegrunnelse.EgenmeldingUtenforArbeidsgiverperiode
            BegrunnelseDTO.MeldingTilNavDagUtenforVentetid -> ApiBegrunnelse.MeldingTilNavDagUtenforVentetid
            BegrunnelseDTO.AvslåttMeldingTilNavDag -> ApiBegrunnelse.AvslattMeldingTilNavDag
            BegrunnelseDTO.AndreYtelserAap -> ApiBegrunnelse.AndreYtelser
            BegrunnelseDTO.AndreYtelserDagpenger -> ApiBegrunnelse.AndreYtelser
            BegrunnelseDTO.AndreYtelserForeldrepenger -> ApiBegrunnelse.AndreYtelser
            BegrunnelseDTO.AndreYtelserOmsorgspenger -> ApiBegrunnelse.AndreYtelser
            BegrunnelseDTO.AndreYtelserOpplaringspenger -> ApiBegrunnelse.AndreYtelser
            BegrunnelseDTO.AndreYtelserPleiepenger -> ApiBegrunnelse.AndreYtelser
            BegrunnelseDTO.AndreYtelserSvangerskapspenger -> ApiBegrunnelse.AndreYtelser
            BegrunnelseDTO.MinimumSykdomsgrad -> ApiBegrunnelse.MinimumSykdomsgrad
            BegrunnelseDTO.EtterDødsdato -> ApiBegrunnelse.EtterDodsdato
            BegrunnelseDTO.ManglerMedlemskap -> ApiBegrunnelse.ManglerMedlemskap
            BegrunnelseDTO.ManglerOpptjening -> ApiBegrunnelse.ManglerOpptjening
            BegrunnelseDTO.Over70 -> ApiBegrunnelse.Over70
        }
    }
)

private fun mapOppdrag(oppdrag: SpeilOppdrag): ApiOppdrag =
    ApiOppdrag(
        fagsystemId = oppdrag.fagsystemId,
        tidsstempel = oppdrag.tidsstempel,
        simulering = oppdrag.simulering?.let { simulering ->
            ApiSimulering(
                totalbelop = simulering.totalbeløp,
                perioder = simulering.perioder.map { periode ->
                    ApiSimuleringsperiode(
                        fom = periode.fom,
                        tom = periode.tom,
                        utbetalinger = periode.utbetalinger.map { utbetaling ->
                            ApiSimuleringsutbetaling(
                                utbetalesTilId = utbetaling.mottakerId,
                                utbetalesTilNavn = utbetaling.mottakerNavn,
                                forfall = utbetaling.forfall,
                                feilkonto = utbetaling.feilkonto,
                                detaljer = utbetaling.detaljer.map {
                                    ApiSimuleringsdetaljer(
                                        faktiskFom = it.faktiskFom,
                                        faktiskTom = it.faktiskTom,
                                        konto = it.konto,
                                        belop = it.beløp,
                                        tilbakeforing = it.tilbakeføring,
                                        sats = it.sats,
                                        typeSats = it.typeSats,
                                        antallSats = it.antallSats,
                                        uforegrad = it.uføregrad,
                                        klassekode = it.klassekode,
                                        klassekodeBeskrivelse = it.klassekodeBeskrivelse,
                                        utbetalingstype = it.utbetalingstype,
                                        refunderesOrgNr = it.refunderesOrgNr
                                    )
                                }
                            )
                        }
                    )
                }
            )
        },
        utbetalingslinjer = emptyList()
    )

private fun mapUtbetaling(utbetaling: Utbetaling) = UtbetalingDto(
    id = utbetaling.id,
    typeEnum = when (utbetaling.type) {
        Utbetalingtype.UTBETALING -> UtbetalingtypeDto.UTBETALING
        Utbetalingtype.ETTERUTBETALING -> UtbetalingtypeDto.ETTERUTBETALING
        Utbetalingtype.ANNULLERING -> UtbetalingtypeDto.ANNULLERING
        Utbetalingtype.REVURDERING -> UtbetalingtypeDto.REVURDERING
        Utbetalingtype.FERIEPENGER -> UtbetalingtypeDto.FERIEPENGER
    },
    statusEnum = when (utbetaling.status) {
        Utbetalingstatus.Annullert -> UtbetalingstatusDto.Annullert
        Utbetalingstatus.GodkjentUtenUtbetaling -> UtbetalingstatusDto.GodkjentUtenUtbetaling
        Utbetalingstatus.IkkeGodkjent -> UtbetalingstatusDto.IkkeGodkjent
        Utbetalingstatus.Overført -> UtbetalingstatusDto.Overfort
        Utbetalingstatus.Ubetalt -> UtbetalingstatusDto.Ubetalt
        Utbetalingstatus.Utbetalt -> UtbetalingstatusDto.Utbetalt
    },
    arbeidsgiverNettoBelop = utbetaling.arbeidsgiverNettoBeløp,
    personNettoBelop = utbetaling.personNettoBeløp,
    arbeidsgiverFagsystemId = utbetaling.arbeidsgiverFagsystemId,
    personFagsystemId = utbetaling.personFagsystemId,
    vurdering = utbetaling.vurdering?.let {
        ApiVurdering(
            godkjent = it.godkjent,
            tidsstempel = it.tidsstempel,
            automatisk = it.automatisk,
            ident = it.ident
        )
    },
    arbeidsgiveroppdrag = utbetaling.oppdrag[utbetaling.arbeidsgiverFagsystemId]?.let { mapOppdrag(it) },
    personoppdrag = utbetaling.oppdrag[utbetaling.personFagsystemId]?.let { mapOppdrag(it) }
)

private fun mapHendelse(hendelse: HendelseDTO): ApiHendelse? = when (hendelse.type) {
    HendelsetypeDto.NY_SØKNAD -> ApiSykmelding(
        id = hendelse.id,
        eksternDokumentId = hendelse.eksternDokumentId,
        fom = hendelse.fom!!,
        tom = hendelse.tom!!,
        rapportertDato = hendelse.rapportertdato!!
    )

    HendelsetypeDto.SENDT_SØKNAD_NAV -> ApiSoknadNav(
        id = hendelse.id,
        eksternDokumentId = hendelse.eksternDokumentId,
        fom = hendelse.fom!!,
        tom = hendelse.tom!!,
        rapportertDato = hendelse.rapportertdato!!,
        sendtNav = hendelse.sendtNav!!
    )

    HendelsetypeDto.SENDT_SØKNAD_FRILANS -> ApiSoknadFrilans(
        id = hendelse.id,
        eksternDokumentId = hendelse.eksternDokumentId,
        fom = hendelse.fom!!,
        tom = hendelse.tom!!,
        rapportertDato = hendelse.rapportertdato!!,
        sendtNav = hendelse.sendtNav!!
    )

    HendelsetypeDto.SENDT_SØKNAD_SELVSTENDIG -> ApiSoknadSelvstendig(
        id = hendelse.id,
        eksternDokumentId = hendelse.eksternDokumentId,
        fom = hendelse.fom!!,
        tom = hendelse.tom!!,
        rapportertDato = hendelse.rapportertdato!!,
        sendtNav = hendelse.sendtNav!!
    )

    HendelsetypeDto.SENDT_SØKNAD_ARBEIDSLEDIG -> ApiSoknadArbeidsledig(
        id = hendelse.id,
        eksternDokumentId = hendelse.eksternDokumentId,
        fom = hendelse.fom!!,
        tom = hendelse.tom!!,
        rapportertDato = hendelse.rapportertdato!!,
        sendtNav = hendelse.sendtNav!!
    )

    HendelsetypeDto.SENDT_SØKNAD_ARBEIDSGIVER -> ApiSoknadArbeidsgiver(
        id = hendelse.id,
        eksternDokumentId = hendelse.eksternDokumentId,
        fom = hendelse.fom!!,
        tom = hendelse.tom!!,
        rapportertDato = hendelse.rapportertdato!!,
        sendtArbeidsgiver = hendelse.sendtArbeidsgiver!!
    )

    HendelsetypeDto.INNTEKTSMELDING -> ApiInntektsmelding(
        id = hendelse.id,
        eksternDokumentId = hendelse.eksternDokumentId,
        mottattDato = hendelse.mottattDato!!,
        beregnetInntekt = hendelse.beregnetInntekt!!
    )

    HendelsetypeDto.INNTEKT_FRA_AORDNINGEN -> ApiInntektFraAOrdningen(
        id = hendelse.id,
        eksternDokumentId = hendelse.eksternDokumentId,
        mottattDato = hendelse.mottattDato!!
    )

    else -> null
}

private fun mapPeriodevilkår(vilkår: BeregnetPeriode.Vilkår) = ApiPeriodevilkar(
    sykepengedager = vilkår.sykepengedager.let {
        ApiPeriodevilkar.ApiSykepengedager(
            skjaeringstidspunkt = it.skjæringstidspunkt,
            maksdato = it.maksdato,
            forbrukteSykedager = it.forbrukteSykedager,
            gjenstaendeSykedager = it.gjenståendeDager,
            oppfylt = it.oppfylt
        )
    },
    alder = vilkår.alder.let {
        ApiPeriodevilkar.ApiAlder(
            alderSisteSykedag = it.alderSisteSykedag,
            oppfylt = it.oppfylt
        )
    }
)

private fun mapPensjonsgivendeInntekter(pensjonsgivendeInntekter: List<SelvstendigFaktaavklartInntektUtDto.PensjonsgivendeInntektDto>) =
    pensjonsgivendeInntekter.map { ApiPensjonsgivendeInntekt(it.årstall.value, it.beløp.årlig.beløp) }

private fun mapPeriodetype(type: Tidslinjeperiodetype) = when (type) {
    Tidslinjeperiodetype.FØRSTEGANGSBEHANDLING -> PeriodetypeDto.Forstegangsbehandling
    Tidslinjeperiodetype.FORLENGELSE -> PeriodetypeDto.Forlengelse
    Tidslinjeperiodetype.OVERGANG_FRA_IT -> PeriodetypeDto.OvergangFraIt
    Tidslinjeperiodetype.INFOTRYGDFORLENGELSE -> PeriodetypeDto.Infotrygdforlengelse
}

internal fun mapTidslinjeperiode(periode: SpeilTidslinjeperiode, hendelser: List<HendelseDTO>) =
    when (periode) {
        is AnnullertPeriode -> mapAnnullertPeriode(periode, hendelser)
        is BeregnetPeriode -> mapBeregnetPeriode(periode, hendelser)
        is UberegnetPeriode -> UberegnetPeriodeDto(
            behandlingId = periode.behandlingId,
            kilde = periode.kilde,
            fom = periode.fom,
            tom = periode.tom,
            tidslinje = periode.sammenslåttTidslinje.map { mapDag(it) },
            periodetype = mapPeriodetype(periode.periodetype),
            erForkastet = periode.erForkastet,
            opprettet = periode.oppdatert,
            vedtaksperiodeId = periode.vedtaksperiodeId,
            periodetilstand = mapTilstand(periode.periodetilstand),
            skjaeringstidspunkt = periode.skjæringstidspunkt,
            hendelser = periode.hendelser.tilHendelseDto(hendelser),
            pensjonsgivendeInntekter = mapPensjonsgivendeInntekter(periode.pensjonsgivendeInntekter)
        )
    }

private fun mapBeregnetPeriode(periode: BeregnetPeriode, hendelser: List<HendelseDTO>) =
    BeregnetPeriodeDto(
        behandlingId = periode.behandlingId,
        kilde = periode.kilde,
        fom = periode.fom,
        tom = periode.tom,
        tidslinje = periode.sammenslåttTidslinje.map { mapDag(it) },
        periodetype = mapPeriodetype(periode.periodetype),
        erForkastet = periode.erForkastet,
        opprettet = periode.behandlingOpprettet,
        vedtaksperiodeId = periode.vedtaksperiodeId,
        beregningId = periode.beregningId,
        gjenstaendeSykedager = periode.gjenståendeDager,
        forbrukteSykedager = periode.forbrukteSykedager,
        skjaeringstidspunkt = periode.skjæringstidspunkt,
        maksdato = periode.maksdato,
        utbetaling = mapUtbetaling(periode.utbetaling),
        hendelser = periode.hendelser.tilHendelseDto(hendelser),
        periodevilkar = mapPeriodevilkår(periode.periodevilkår),
        periodetilstand = mapTilstand(periode.periodetilstand),
        vilkarsgrunnlagId = periode.vilkårsgrunnlagId,
        pensjonsgivendeInntekter = mapPensjonsgivendeInntekter(periode.pensjonsgivendeInntekter),
        annulleringskandidater = mapAnnulleringskandidater(periode.annulleringskandidater)
    )

private fun mapAnnulleringskandidater(annulleringskandidater: List<AnnulleringskandidatDto>) =
    annulleringskandidater.map {
        ApiAnnulleringskandidat(
            vedtaksperiodeId = it.vedtaksperiodeId,
            organisasjonsnummer = it.organisasjonsnummer,
            fom = it.fom,
            tom = it.tom
        )
    }

private fun mapAnnullertPeriode(periode: AnnullertPeriode, hendelser: List<HendelseDTO>) =
    BeregnetPeriodeDto(
        behandlingId = periode.behandlingId,
        kilde = periode.kilde,
        fom = periode.fom,
        tom = periode.tom,
        tidslinje = periode.sammenslåttTidslinje.map { mapDag(it) },
        periodetype = mapPeriodetype(periode.periodetype),
        erForkastet = periode.erForkastet,
        opprettet = periode.opprettet,
        vedtaksperiodeId = periode.vedtaksperiodeId,
        beregningId = periode.beregningId,
        gjenstaendeSykedager = null,
        forbrukteSykedager = null,
        skjaeringstidspunkt = periode.skjæringstidspunkt,
        maksdato = LocalDate.MAX,
        utbetaling = mapUtbetaling(periode.utbetaling),
        hendelser = periode.hendelser.tilHendelseDto(hendelser),
        periodevilkar = ApiPeriodevilkar(
            sykepengedager = ApiPeriodevilkar.ApiSykepengedager(
                skjaeringstidspunkt = LocalDate.MIN,
                maksdato = LocalDate.MAX,
                forbrukteSykedager = null,
                gjenstaendeSykedager = null,
                oppfylt = false
            ),
            alder = ApiPeriodevilkar.ApiAlder(
                alderSisteSykedag = 0,
                oppfylt = false
            )
        ),
        periodetilstand = mapTilstand(periode.periodetilstand),
        vilkarsgrunnlagId = null,
        pensjonsgivendeInntekter = mapPensjonsgivendeInntekter(periode.pensjonsgivendeInntekter),
        annulleringskandidater = emptyList()
    )

private fun Set<UUID>.tilHendelseDto(hendelser: List<HendelseDTO>): List<ApiHendelse> {
    return this
        .mapNotNull { dokumentId -> hendelser.firstOrNull { hendelseDTO -> hendelseDTO.id == dokumentId.toString() } }
        .mapNotNull { mapHendelse(it) }
}

private fun mapTilstand(tilstand: Periodetilstand) = when (tilstand) {
    Periodetilstand.TilUtbetaling -> PeriodetilstandDto.TilUtbetaling
    Periodetilstand.TilAnnullering -> PeriodetilstandDto.TilAnnullering
    Periodetilstand.AvventerAnnullering -> PeriodetilstandDto.AvventerAnnullering
    Periodetilstand.Utbetalt -> PeriodetilstandDto.Utbetalt
    Periodetilstand.Annullert -> PeriodetilstandDto.Annullert
    Periodetilstand.AnnulleringFeilet -> PeriodetilstandDto.AnnulleringFeilet
    Periodetilstand.IngenUtbetaling -> PeriodetilstandDto.IngenUtbetaling
    Periodetilstand.RevurderingFeilet -> PeriodetilstandDto.RevurderingFeilet
    Periodetilstand.TilInfotrygd -> PeriodetilstandDto.TilInfotrygd
    Periodetilstand.ForberederGodkjenning -> PeriodetilstandDto.ForberederGodkjenning
    Periodetilstand.ManglerInformasjon -> PeriodetilstandDto.ManglerInformasjon
    Periodetilstand.VenterPåAnnenPeriode -> PeriodetilstandDto.VenterPaAnnenPeriode
    Periodetilstand.TilGodkjenning -> PeriodetilstandDto.TilGodkjenning
    Periodetilstand.UtbetaltVenterPåAnnenPeriode -> PeriodetilstandDto.UtbetaltVenterPaAnnenPeriode
    Periodetilstand.AvventerInntektsopplysninger -> PeriodetilstandDto.AvventerInntektsopplysninger
}

private fun mapArbeidsgiverRefusjon(arbeidsgiverrefusjon: ArbeidsgiverrefusjonSpeil) = ArbeidsgiverrefusjonDto(
    arbeidsgiver = arbeidsgiverrefusjon.arbeidsgiver,
    refusjonsopplysninger = arbeidsgiverrefusjon.refusjonsopplysninger.map {
        ApiRefusjonselement(
            fom = it.fom,
            tom = it.tom,
            belop = it.beløp,
            meldingsreferanseId = it.meldingsreferanseId
        )
    }
)

private fun mapInntekt(skjæringstidspunkt: LocalDate, inntekt: ArbeidsgiverinntektSpeil) = ArbeidsgiverinntektDto(
    arbeidsgiver = inntekt.organisasjonsnummer,
    omregnetArsinntekt = inntekt.omregnetÅrsinntekt.tilOmregnetArsinntekt(),
    skjonnsmessigFastsatt = inntekt.skjønnsmessigFastsatt?.let {
        ApiSkjonnsmessigFastsatt(
            belop = it.årlig,
            manedsbelop = it.månedlig
        )
    },
    fom = skjæringstidspunkt,
    tom = null,
    deaktivert = inntekt.deaktivert
)

private fun Inntekt.tilOmregnetArsinntekt() = ApiOmregnetArsinntekt(
    kilde = when (this.kilde) {
        Inntektkilde.Saksbehandler -> ApiInntektskilde.Saksbehandler
        Inntektkilde.Inntektsmelding -> ApiInntektskilde.Inntektsmelding
        Inntektkilde.Infotrygd -> ApiInntektskilde.Infotrygd
        Inntektkilde.AOrdningen -> ApiInntektskilde.AOrdningen
        Inntektkilde.IkkeRapportert -> ApiInntektskilde.IkkeRapportert
    },
    belop = this.beløp,
    manedsbelop = this.månedsbeløp,
    inntekterFraAOrdningen = this.inntekterFraAOrdningen?.map {
        ApiInntekterFraAOrdningen(
            maned = it.måned,
            sum = it.sum
        )
    }
)

internal fun mapVilkårsgrunnlag(id: UUID, vilkårsgrunnlag: Vilkårsgrunnlag) =
    when (vilkårsgrunnlag) {
        is SpleisVilkårsgrunnlag -> {
            val inntekter = vilkårsgrunnlag.inntekter.map { inntekt -> mapInntekt(vilkårsgrunnlag.skjæringstidspunkt, inntekt) }
            ApiSpleisVilkarsgrunnlag(
                id = id,
                skjaeringstidspunkt = vilkårsgrunnlag.skjæringstidspunkt,
                omregnetArsinntekt = vilkårsgrunnlag.omregnetÅrsinntekt,
                sykepengegrunnlag = vilkårsgrunnlag.sykepengegrunnlag,
                beregningsgrunnlag = vilkårsgrunnlag.beregningsgrunnlag,
                inntekter = inntekter,
                grunnbelop = vilkårsgrunnlag.grunnbeløp,
                sykepengegrunnlagsgrense = mapSykepengegrunnlagsgrense(vilkårsgrunnlag.sykepengegrunnlagsgrense),
                antallOpptjeningsdagerErMinst = vilkårsgrunnlag.antallOpptjeningsdagerErMinst,
                opptjeningFra = vilkårsgrunnlag.opptjeningFra,
                oppfyllerKravOmMinstelonn = vilkårsgrunnlag.oppfyllerKravOmMinstelønn,
                oppfyllerKravOmOpptjening = vilkårsgrunnlag.oppfyllerKravOmOpptjening,
                oppfyllerKravOmMedlemskap = vilkårsgrunnlag.oppfyllerKravOmMedlemskap,
                arbeidsgiverrefusjoner = vilkårsgrunnlag.arbeidsgiverrefusjoner.map { refusjon -> mapArbeidsgiverRefusjon(refusjon) },
                forsikringsvurderingId = vilkårsgrunnlag.forsikringsvurderingId,
                opptjeningsvurderingId = vilkårsgrunnlag.opptjeningsvurderingId,
                skjonnsmessigFastsattAarlig = inntekter
                    .filter { it.deaktivert != true }
                    .mapNotNull { it.skjonnsmessigFastsatt }
                    .takeIf(List<*>::isNotEmpty)
                    ?.sumOf { it.belop }
            )
        }

        is InfotrygdVilkårsgrunnlag -> ApiInfotrygdVilkarsgrunnlag(
            id = id,
            skjaeringstidspunkt = vilkårsgrunnlag.skjæringstidspunkt,
            // For infotrygd har vi ikke noe konsept for hvorvidt en inntekt er skjønnsfastsatt
            omregnetArsinntekt = vilkårsgrunnlag.beregningsgrunnlag,
            sykepengegrunnlag = vilkårsgrunnlag.sykepengegrunnlag,
            inntekter = vilkårsgrunnlag.inntekter.map { inntekt -> mapInntekt(vilkårsgrunnlag.skjæringstidspunkt, inntekt) },
            arbeidsgiverrefusjoner = vilkårsgrunnlag.arbeidsgiverrefusjoner.map { refusjon -> mapArbeidsgiverRefusjon(refusjon) },
            opptjeningsvurderingId = vilkårsgrunnlag.opptjeningsvurderingId
        )

        else -> throw IllegalStateException("har ikke mapping for vilkårsgrunnlag ${vilkårsgrunnlag::class.simpleName ?: "[ukjent klassenavn]"}")
    }

private fun mapSykepengegrunnlagsgrense(sykepengegrunnlagsgrenseDTO: SykepengegrunnlagsgrenseDTO) =
    ApiSykepengegrunnlagsgrense(sykepengegrunnlagsgrenseDTO.grunnbeløp, sykepengegrunnlagsgrenseDTO.grense, sykepengegrunnlagsgrenseDTO.virkningstidspunkt)
