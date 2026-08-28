package no.nav.helse.spleis.rest

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.dto.AnnulleringskandidatDto
import no.nav.helse.dto.serialisering.SelvstendigFaktaavklartInntektUtDto
import no.nav.helse.spleis.dto.HendelseDTO
import no.nav.helse.spleis.dto.HendelsetypeDto
import no.nav.helse.spleis.rest.dto.Annulleringskandidat
import no.nav.helse.spleis.rest.dto.Begrunnelse
import no.nav.helse.spleis.rest.dto.Dag
import no.nav.helse.spleis.rest.dto.Hendelse
import no.nav.helse.spleis.rest.dto.InfotrygdVilkarsgrunnlag
import no.nav.helse.spleis.rest.dto.InntektFraAOrdningen
import no.nav.helse.spleis.rest.dto.InntekterFraAOrdningen
import no.nav.helse.spleis.rest.dto.Inntektskilde
import no.nav.helse.spleis.rest.dto.Inntektsmelding
import no.nav.helse.spleis.rest.dto.OmregnetArsinntekt
import no.nav.helse.spleis.rest.dto.Oppdrag
import no.nav.helse.spleis.rest.dto.PensjonsgivendeInntekt
import no.nav.helse.spleis.rest.dto.Periodevilkar
import no.nav.helse.spleis.rest.dto.Refusjonselement
import no.nav.helse.spleis.rest.dto.Simulering
import no.nav.helse.spleis.rest.dto.Simuleringsdetaljer
import no.nav.helse.spleis.rest.dto.Simuleringsperiode
import no.nav.helse.spleis.rest.dto.Simuleringsutbetaling
import no.nav.helse.spleis.rest.dto.SkjonnsmessigFastsatt
import no.nav.helse.spleis.rest.dto.SoknadArbeidsgiver
import no.nav.helse.spleis.rest.dto.SoknadArbeidsledig
import no.nav.helse.spleis.rest.dto.SoknadFrilans
import no.nav.helse.spleis.rest.dto.SoknadNav
import no.nav.helse.spleis.rest.dto.SoknadSelvstendig
import no.nav.helse.spleis.rest.dto.SpleisVilkarsgrunnlag
import no.nav.helse.spleis.rest.dto.Sykdomsdagkilde
import no.nav.helse.spleis.rest.dto.Sykdomsdagkildetype
import no.nav.helse.spleis.rest.dto.Sykdomsdagtype
import no.nav.helse.spleis.rest.dto.Sykepengegrunnlagsgrense
import no.nav.helse.spleis.rest.dto.Sykmelding
import no.nav.helse.spleis.rest.dto.UtbetalingsdagType
import no.nav.helse.spleis.rest.dto.Utbetalingsinfo
import no.nav.helse.spleis.rest.dto.Vurdering
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
import no.nav.helse.spleis.rest.dto.Arbeidsgiverinntekt as ArbeidsgiverinntektDto
import no.nav.helse.spleis.rest.dto.Arbeidsgiverrefusjon as ArbeidsgiverrefusjonDto
import no.nav.helse.spleis.rest.dto.BeregnetPeriode as BeregnetPeriodeDto
import no.nav.helse.spleis.rest.dto.Periodetilstand as PeriodetilstandDto
import no.nav.helse.spleis.rest.dto.Periodetype as PeriodetypeDto
import no.nav.helse.spleis.rest.dto.UberegnetPeriode as UberegnetPeriodeDto
import no.nav.helse.spleis.rest.dto.Utbetaling as UtbetalingDto
import no.nav.helse.spleis.rest.dto.Utbetalingstatus as UtbetalingstatusDto
import no.nav.helse.spleis.rest.dto.Utbetalingtype as UtbetalingtypeDto
import no.nav.helse.spleis.speil.dto.Arbeidsgiverinntekt as ArbeidsgiverinntektSpeil
import no.nav.helse.spleis.speil.dto.Arbeidsgiverrefusjon as ArbeidsgiverrefusjonSpeil

private fun mapDag(dag: SammenslåttDag) = Dag(
    dato = dag.dagen,
    sykdomsdagtype = when (dag.sykdomstidslinjedagtype) {
        SykdomstidslinjedagType.ARBEIDSDAG -> Sykdomsdagtype.Arbeidsdag
        SykdomstidslinjedagType.ARBEIDSGIVERDAG -> Sykdomsdagtype.Arbeidsgiverdag
        SykdomstidslinjedagType.MELDING_TIL_NAV_DAG -> Sykdomsdagtype.MeldingTilNavDag
        SykdomstidslinjedagType.FERIEDAG -> Sykdomsdagtype.Feriedag
        SykdomstidslinjedagType.ARBEID_IKKE_GJENOPPTATT_DAG -> Sykdomsdagtype.ArbeidIkkeGjenopptattDag
        SykdomstidslinjedagType.FORELDET_SYKEDAG -> Sykdomsdagtype.ForeldetSykedag
        SykdomstidslinjedagType.FRISK_HELGEDAG -> Sykdomsdagtype.FriskHelgedag
        SykdomstidslinjedagType.PERMISJONSDAG -> Sykdomsdagtype.Permisjonsdag
        SykdomstidslinjedagType.SYKEDAG -> Sykdomsdagtype.Sykedag
        SykdomstidslinjedagType.SYKEDAG_NAV -> Sykdomsdagtype.SykedagNav
        SykdomstidslinjedagType.SYK_HELGEDAG -> Sykdomsdagtype.SykHelgedag
        SykdomstidslinjedagType.ANDRE_YTELSER_FORELDREPENGER -> Sykdomsdagtype.AndreYtelserForeldrepenger
        SykdomstidslinjedagType.ANDRE_YTELSER_AAP -> Sykdomsdagtype.AndreYtelserAap
        SykdomstidslinjedagType.ANDRE_YTELSER_OMSORGSPENGER -> Sykdomsdagtype.AndreYtelserOmsorgspenger
        SykdomstidslinjedagType.ANDRE_YTELSER_PLEIEPENGER -> Sykdomsdagtype.AndreYtelserPleiepenger
        SykdomstidslinjedagType.ANDRE_YTELSER_SVANGERSKAPSPENGER -> Sykdomsdagtype.AndreYtelserSvangerskapspenger
        SykdomstidslinjedagType.ANDRE_YTELSER_OPPLÆRINGSPENGER -> Sykdomsdagtype.AndreYtelserOpplaringspenger
        SykdomstidslinjedagType.ANDRE_YTELSER_DAGPENGER -> Sykdomsdagtype.AndreYtelserDagpenger
        SykdomstidslinjedagType.UBESTEMTDAG -> Sykdomsdagtype.Ubestemtdag
        SykdomstidslinjedagType.AVSLÅTT_MELDING_TIL_NAV_DAG -> Sykdomsdagtype.AvslattMeldingTilNavDag
    },
    utbetalingsdagtype = when (dag.utbetalingstidslinjedagtype) {
        UtbetalingstidslinjedagType.ArbeidsgiverperiodeDag -> UtbetalingsdagType.ArbeidsgiverperiodeDag
        UtbetalingstidslinjedagType.NavDag -> UtbetalingsdagType.NavDag
        UtbetalingstidslinjedagType.NavHelgDag -> UtbetalingsdagType.NavHelgDag
        UtbetalingstidslinjedagType.Helgedag -> UtbetalingsdagType.Helgedag
        UtbetalingstidslinjedagType.Arbeidsdag -> UtbetalingsdagType.Arbeidsdag
        UtbetalingstidslinjedagType.Feriedag -> UtbetalingsdagType.Feriedag
        UtbetalingstidslinjedagType.AvvistDag -> UtbetalingsdagType.AvvistDag
        UtbetalingstidslinjedagType.UkjentDag -> UtbetalingsdagType.UkjentDag
        UtbetalingstidslinjedagType.ForeldetDag -> UtbetalingsdagType.ForeldetDag
        UtbetalingstidslinjedagType.Ventetidsdag -> UtbetalingsdagType.Ventetidsdag
    },
    kilde = Sykdomsdagkilde(
        id = dag.kilde.id,
        type = when (dag.kilde.type) {
            SykdomstidslinjedagKildetype.Inntektsmelding -> Sykdomsdagkildetype.Inntektsmelding
            SykdomstidslinjedagKildetype.Søknad -> Sykdomsdagkildetype.Soknad
            SykdomstidslinjedagKildetype.Sykmelding -> Sykdomsdagkildetype.Sykmelding
            SykdomstidslinjedagKildetype.Saksbehandler -> Sykdomsdagkildetype.Saksbehandler
            SykdomstidslinjedagKildetype.Ukjent -> Sykdomsdagkildetype.Ukjent
        }
    ),
    grad = dag.grad?.toDouble(),
    utbetalingsinfo = dag.utbetalingsinfo?.let {
        Utbetalingsinfo(
            inntekt = null, // deprecated: speil bruker ikke denne verdien
            utbetaling = it.arbeidsgiverbeløp, // deprecated: verdien settes til det samme som arbeidsgiverbeløp
            personbelop = it.personbeløp,
            arbeidsgiverbelop = it.arbeidsgiverbeløp,
            refusjonsbelop = null, // deprecated: speil bruker ikke denne verdien
            totalGrad = it.totalGrad.toDouble() // double er deprecated: speil forventer float, men runder ned før visning
        )
    },
    begrunnelser = dag.begrunnelser?.map {
        when (it) {
            BegrunnelseDTO.SykepengedagerOppbrukt -> Begrunnelse.SykepengedagerOppbrukt
            BegrunnelseDTO.SykepengedagerOppbruktOver67 -> Begrunnelse.SykepengedagerOppbruktOver67
            BegrunnelseDTO.MinimumInntekt -> Begrunnelse.MinimumInntekt
            BegrunnelseDTO.MinimumInntektOver67 -> Begrunnelse.MinimumInntektOver67
            BegrunnelseDTO.EgenmeldingUtenforArbeidsgiverperiode -> Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode
            BegrunnelseDTO.MeldingTilNavDagUtenforVentetid -> Begrunnelse.MeldingTilNavDagUtenforVentetid
            BegrunnelseDTO.AvslåttMeldingTilNavDag -> Begrunnelse.AvslattMeldingTilNavDag
            BegrunnelseDTO.AndreYtelserAap -> Begrunnelse.AndreYtelser
            BegrunnelseDTO.AndreYtelserDagpenger -> Begrunnelse.AndreYtelser
            BegrunnelseDTO.AndreYtelserForeldrepenger -> Begrunnelse.AndreYtelser
            BegrunnelseDTO.AndreYtelserOmsorgspenger -> Begrunnelse.AndreYtelser
            BegrunnelseDTO.AndreYtelserOpplaringspenger -> Begrunnelse.AndreYtelser
            BegrunnelseDTO.AndreYtelserPleiepenger -> Begrunnelse.AndreYtelser
            BegrunnelseDTO.AndreYtelserSvangerskapspenger -> Begrunnelse.AndreYtelser
            BegrunnelseDTO.MinimumSykdomsgrad -> Begrunnelse.MinimumSykdomsgrad
            BegrunnelseDTO.EtterDødsdato -> Begrunnelse.EtterDodsdato
            BegrunnelseDTO.ManglerMedlemskap -> Begrunnelse.ManglerMedlemskap
            BegrunnelseDTO.ManglerOpptjening -> Begrunnelse.ManglerOpptjening
            BegrunnelseDTO.Over70 -> Begrunnelse.Over70
        }
    }
)

private fun mapOppdrag(oppdrag: SpeilOppdrag): Oppdrag =
    Oppdrag(
        fagsystemId = oppdrag.fagsystemId,
        tidsstempel = oppdrag.tidsstempel,
        simulering = oppdrag.simulering?.let { simulering ->
            Simulering(
                totalbelop = simulering.totalbeløp,
                perioder = simulering.perioder.map { periode ->
                    Simuleringsperiode(
                        fom = periode.fom,
                        tom = periode.tom,
                        utbetalinger = periode.utbetalinger.map { utbetaling ->
                            Simuleringsutbetaling(
                                utbetalesTilId = utbetaling.mottakerId,
                                utbetalesTilNavn = utbetaling.mottakerNavn,
                                forfall = utbetaling.forfall,
                                feilkonto = utbetaling.feilkonto,
                                detaljer = utbetaling.detaljer.map {
                                    Simuleringsdetaljer(
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
        Vurdering(
            godkjent = it.godkjent,
            tidsstempel = it.tidsstempel,
            automatisk = it.automatisk,
            ident = it.ident
        )
    },
    arbeidsgiveroppdrag = utbetaling.oppdrag[utbetaling.arbeidsgiverFagsystemId]?.let { mapOppdrag(it) },
    personoppdrag = utbetaling.oppdrag[utbetaling.personFagsystemId]?.let { mapOppdrag(it) }
)

private fun mapHendelse(hendelse: HendelseDTO): Hendelse? = when (hendelse.type) {
    HendelsetypeDto.NY_SØKNAD -> Sykmelding(
        id = hendelse.id,
        eksternDokumentId = hendelse.eksternDokumentId,
        fom = hendelse.fom!!,
        tom = hendelse.tom!!,
        rapportertDato = hendelse.rapportertdato!!
    )

    HendelsetypeDto.SENDT_SØKNAD_NAV -> SoknadNav(
        id = hendelse.id,
        eksternDokumentId = hendelse.eksternDokumentId,
        fom = hendelse.fom!!,
        tom = hendelse.tom!!,
        rapportertDato = hendelse.rapportertdato!!,
        sendtNav = hendelse.sendtNav!!
    )

    HendelsetypeDto.SENDT_SØKNAD_FRILANS -> SoknadFrilans(
        id = hendelse.id,
        eksternDokumentId = hendelse.eksternDokumentId,
        fom = hendelse.fom!!,
        tom = hendelse.tom!!,
        rapportertDato = hendelse.rapportertdato!!,
        sendtNav = hendelse.sendtNav!!
    )

    HendelsetypeDto.SENDT_SØKNAD_SELVSTENDIG -> SoknadSelvstendig(
        id = hendelse.id,
        eksternDokumentId = hendelse.eksternDokumentId,
        fom = hendelse.fom!!,
        tom = hendelse.tom!!,
        rapportertDato = hendelse.rapportertdato!!,
        sendtNav = hendelse.sendtNav!!
    )

    HendelsetypeDto.SENDT_SØKNAD_ARBEIDSLEDIG -> SoknadArbeidsledig(
        id = hendelse.id,
        eksternDokumentId = hendelse.eksternDokumentId,
        fom = hendelse.fom!!,
        tom = hendelse.tom!!,
        rapportertDato = hendelse.rapportertdato!!,
        sendtNav = hendelse.sendtNav!!
    )

    HendelsetypeDto.SENDT_SØKNAD_ARBEIDSGIVER -> SoknadArbeidsgiver(
        id = hendelse.id,
        eksternDokumentId = hendelse.eksternDokumentId,
        fom = hendelse.fom!!,
        tom = hendelse.tom!!,
        rapportertDato = hendelse.rapportertdato!!,
        sendtArbeidsgiver = hendelse.sendtArbeidsgiver!!
    )

    HendelsetypeDto.INNTEKTSMELDING -> Inntektsmelding(
        id = hendelse.id,
        eksternDokumentId = hendelse.eksternDokumentId,
        mottattDato = hendelse.mottattDato!!,
        beregnetInntekt = hendelse.beregnetInntekt!!
    )

    HendelsetypeDto.INNTEKT_FRA_AORDNINGEN -> InntektFraAOrdningen(
        id = hendelse.id,
        eksternDokumentId = hendelse.eksternDokumentId,
        mottattDato = hendelse.mottattDato!!
    )

    else -> null
}

private fun mapPeriodevilkår(vilkår: BeregnetPeriode.Vilkår) = Periodevilkar(
    sykepengedager = vilkår.sykepengedager.let {
        Periodevilkar.Sykepengedager(
            skjaeringstidspunkt = it.skjæringstidspunkt,
            maksdato = it.maksdato,
            forbrukteSykedager = it.forbrukteSykedager,
            gjenstaendeSykedager = it.gjenståendeDager,
            oppfylt = it.oppfylt
        )
    },
    alder = vilkår.alder.let {
        Periodevilkar.Alder(
            alderSisteSykedag = it.alderSisteSykedag,
            oppfylt = it.oppfylt
        )
    }
)

private fun mapPensjonsgivendeInntekter(pensjonsgivendeInntekter: List<SelvstendigFaktaavklartInntektUtDto.PensjonsgivendeInntektDto>) =
    pensjonsgivendeInntekter.map { PensjonsgivendeInntekt(it.årstall.value, it.beløp.årlig.beløp) }

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
        Annulleringskandidat(
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
        periodevilkar = Periodevilkar(
            sykepengedager = Periodevilkar.Sykepengedager(
                skjaeringstidspunkt = LocalDate.MIN,
                maksdato = LocalDate.MAX,
                forbrukteSykedager = null,
                gjenstaendeSykedager = null,
                oppfylt = false
            ),
            alder = Periodevilkar.Alder(
                alderSisteSykedag = 0,
                oppfylt = false
            )
        ),
        periodetilstand = mapTilstand(periode.periodetilstand),
        vilkarsgrunnlagId = null,
        pensjonsgivendeInntekter = mapPensjonsgivendeInntekter(periode.pensjonsgivendeInntekter),
        annulleringskandidater = emptyList()
    )

private fun Set<UUID>.tilHendelseDto(hendelser: List<HendelseDTO>): List<Hendelse> {
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
        Refusjonselement(
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
        SkjonnsmessigFastsatt(
            belop = it.årlig,
            manedsbelop = it.månedlig
        )
    },
    fom = skjæringstidspunkt,
    tom = null,
    deaktivert = inntekt.deaktivert
)

private fun Inntekt.tilOmregnetArsinntekt() = OmregnetArsinntekt(
    kilde = when (this.kilde) {
        Inntektkilde.Saksbehandler -> Inntektskilde.Saksbehandler
        Inntektkilde.Inntektsmelding -> Inntektskilde.Inntektsmelding
        Inntektkilde.Infotrygd -> Inntektskilde.Infotrygd
        Inntektkilde.AOrdningen -> Inntektskilde.AOrdningen
        Inntektkilde.IkkeRapportert -> Inntektskilde.IkkeRapportert
    },
    belop = this.beløp,
    manedsbelop = this.månedsbeløp,
    inntekterFraAOrdningen = this.inntekterFraAOrdningen?.map {
        InntekterFraAOrdningen(
            maned = it.måned,
            sum = it.sum
        )
    }
)

internal fun mapVilkårsgrunnlag(id: UUID, vilkårsgrunnlag: Vilkårsgrunnlag) =
    when (vilkårsgrunnlag) {
        is SpleisVilkårsgrunnlag -> SpleisVilkarsgrunnlag(
            id = id,
            skjaeringstidspunkt = vilkårsgrunnlag.skjæringstidspunkt,
            omregnetArsinntekt = vilkårsgrunnlag.omregnetÅrsinntekt,
            sykepengegrunnlag = vilkårsgrunnlag.sykepengegrunnlag,
            beregningsgrunnlag = vilkårsgrunnlag.beregningsgrunnlag,
            inntekter = vilkårsgrunnlag.inntekter.map { inntekt -> mapInntekt(vilkårsgrunnlag.skjæringstidspunkt, inntekt) },
            grunnbelop = vilkårsgrunnlag.grunnbeløp,
            sykepengegrunnlagsgrense = mapSykepengegrunnlagsgrense(vilkårsgrunnlag.sykepengegrunnlagsgrense),
            antallOpptjeningsdagerErMinst = vilkårsgrunnlag.antallOpptjeningsdagerErMinst,
            opptjeningFra = vilkårsgrunnlag.opptjeningFra,
            oppfyllerKravOmMinstelonn = vilkårsgrunnlag.oppfyllerKravOmMinstelønn,
            oppfyllerKravOmOpptjening = vilkårsgrunnlag.oppfyllerKravOmOpptjening,
            oppfyllerKravOmMedlemskap = vilkårsgrunnlag.oppfyllerKravOmMedlemskap,
            arbeidsgiverrefusjoner = vilkårsgrunnlag.arbeidsgiverrefusjoner.map { refusjon -> mapArbeidsgiverRefusjon(refusjon) },
            forsikringsvurderingId = vilkårsgrunnlag.forsikringsvurderingId,
            opptjeningsvurderingId = vilkårsgrunnlag.opptjeningsvurderingId
        )

        is InfotrygdVilkårsgrunnlag -> InfotrygdVilkarsgrunnlag(
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
    Sykepengegrunnlagsgrense(sykepengegrunnlagsgrenseDTO.grunnbeløp, sykepengegrunnlagsgrenseDTO.grense, sykepengegrunnlagsgrenseDTO.virkningstidspunkt)
