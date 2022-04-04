package no.nav.helse.spleis.graphql

import java.util.UUID
import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.Periodetype
import no.nav.helse.serde.api.BegrunnelseDTO
import no.nav.helse.serde.api.InntektsgrunnlagDTO
import no.nav.helse.serde.api.v2.Arbeidsgiverinntekt
import no.nav.helse.serde.api.v2.Behandlingstype
import no.nav.helse.serde.api.v2.BeregnetPeriode
import no.nav.helse.serde.api.v2.HendelseDTO
import no.nav.helse.serde.api.v2.InfotrygdVilkårsgrunnlag
import no.nav.helse.serde.api.v2.Inntektkilde
import no.nav.helse.serde.api.v2.InntektsmeldingDTO
import no.nav.helse.serde.api.v2.Periodetilstand
import no.nav.helse.serde.api.v2.SammenslåttDag
import no.nav.helse.serde.api.v2.SpeilOppdrag
import no.nav.helse.serde.api.v2.SpleisVilkårsgrunnlag
import no.nav.helse.serde.api.v2.SykdomstidslinjedagKildetype
import no.nav.helse.serde.api.v2.SykdomstidslinjedagType
import no.nav.helse.serde.api.v2.SykmeldingDTO
import no.nav.helse.serde.api.v2.SøknadArbeidsgiverDTO
import no.nav.helse.serde.api.v2.SøknadNavDTO
import no.nav.helse.serde.api.v2.Tidslinjeperiode
import no.nav.helse.serde.api.v2.Utbetaling
import no.nav.helse.serde.api.v2.Utbetalingstatus
import no.nav.helse.serde.api.v2.UtbetalingstidslinjedagType
import no.nav.helse.serde.api.v2.Utbetalingtype
import no.nav.helse.serde.api.v2.Vilkårsgrunnlag
import no.nav.helse.spleis.graphql.dto.GraphQLAktivitet
import no.nav.helse.spleis.graphql.dto.GraphQLArbeidsgiverinntekt
import no.nav.helse.spleis.graphql.dto.GraphQLBegrunnelse
import no.nav.helse.spleis.graphql.dto.GraphQLBehandlingstype
import no.nav.helse.spleis.graphql.dto.GraphQLBeregnetPeriode
import no.nav.helse.spleis.graphql.dto.GraphQLDag
import no.nav.helse.spleis.graphql.dto.GraphQLHendelse
import no.nav.helse.spleis.graphql.dto.GraphQLHendelsetype
import no.nav.helse.spleis.graphql.dto.GraphQLInfotrygdVilkarsgrunnlag
import no.nav.helse.spleis.graphql.dto.GraphQLInntekterFraAOrdningen
import no.nav.helse.spleis.graphql.dto.GraphQLInntektsgrunnlag
import no.nav.helse.spleis.graphql.dto.GraphQLInntektskilde
import no.nav.helse.spleis.graphql.dto.GraphQLInntektsmelding
import no.nav.helse.spleis.graphql.dto.GraphQLInntektstype
import no.nav.helse.spleis.graphql.dto.GraphQLOmregnetArsinntekt
import no.nav.helse.spleis.graphql.dto.GraphQLOppdrag
import no.nav.helse.spleis.graphql.dto.GraphQLPeriodetilstand
import no.nav.helse.spleis.graphql.dto.GraphQLPeriodetype
import no.nav.helse.spleis.graphql.dto.GraphQLPeriodevilkar
import no.nav.helse.spleis.graphql.dto.GraphQLRefusjon
import no.nav.helse.spleis.graphql.dto.GraphQLSammenligningsgrunnlag
import no.nav.helse.spleis.graphql.dto.GraphQLSimulering
import no.nav.helse.spleis.graphql.dto.GraphQLSimuleringsdetaljer
import no.nav.helse.spleis.graphql.dto.GraphQLSimuleringsperiode
import no.nav.helse.spleis.graphql.dto.GraphQLSimuleringsutbetaling
import no.nav.helse.spleis.graphql.dto.GraphQLSoknadArbeidsgiver
import no.nav.helse.spleis.graphql.dto.GraphQLSoknadNav
import no.nav.helse.spleis.graphql.dto.GraphQLSpleisVilkarsgrunnlag
import no.nav.helse.spleis.graphql.dto.GraphQLSykdomsdagkilde
import no.nav.helse.spleis.graphql.dto.GraphQLSykdomsdagkildetype
import no.nav.helse.spleis.graphql.dto.GraphQLSykdomsdagtype
import no.nav.helse.spleis.graphql.dto.GraphQLSykmelding
import no.nav.helse.spleis.graphql.dto.GraphQLUberegnetPeriode
import no.nav.helse.spleis.graphql.dto.GraphQLUtbetaling
import no.nav.helse.spleis.graphql.dto.GraphQLUtbetalingsdagType
import no.nav.helse.spleis.graphql.dto.GraphQLUtbetalingsinfo
import no.nav.helse.spleis.graphql.dto.GraphQLUtbetalingstatus
import no.nav.helse.spleis.graphql.dto.GraphQLVilkarsgrunnlag
import no.nav.helse.spleis.graphql.dto.GraphQLVilkarsgrunnlaghistorikk
import no.nav.helse.spleis.graphql.dto.GraphQLVilkarsgrunnlagtype
import no.nav.helse.spleis.graphql.dto.GraphQLVurdering

private fun mapDag(dag: SammenslåttDag) = GraphQLDag(
    dato = dag.dagen,
    sykdomsdagtype = when (dag.sykdomstidslinjedagtype) {
        SykdomstidslinjedagType.ARBEIDSDAG -> GraphQLSykdomsdagtype.Arbeidsdag
        SykdomstidslinjedagType.ARBEIDSGIVERDAG -> GraphQLSykdomsdagtype.Arbeidsgiverdag
        SykdomstidslinjedagType.FERIEDAG -> GraphQLSykdomsdagtype.Feriedag
        SykdomstidslinjedagType.FORELDET_SYKEDAG -> GraphQLSykdomsdagtype.ForeldetSykedag
        SykdomstidslinjedagType.FRISK_HELGEDAG -> GraphQLSykdomsdagtype.FriskHelgedag
        SykdomstidslinjedagType.PERMISJONSDAG -> GraphQLSykdomsdagtype.Permisjonsdag
        SykdomstidslinjedagType.SYKEDAG -> GraphQLSykdomsdagtype.Sykedag
        SykdomstidslinjedagType.SYK_HELGEDAG -> GraphQLSykdomsdagtype.SykHelgedag
        SykdomstidslinjedagType.UBESTEMTDAG -> GraphQLSykdomsdagtype.Ubestemtdag
        SykdomstidslinjedagType.AVSLÅTT -> GraphQLSykdomsdagtype.Avslatt
    },
    utbetalingsdagtype = when (dag.utbetalingstidslinjedagtype) {
        UtbetalingstidslinjedagType.ArbeidsgiverperiodeDag -> GraphQLUtbetalingsdagType.ArbeidsgiverperiodeDag
        UtbetalingstidslinjedagType.NavDag -> GraphQLUtbetalingsdagType.NavDag
        UtbetalingstidslinjedagType.NavHelgDag -> GraphQLUtbetalingsdagType.NavHelgDag
        UtbetalingstidslinjedagType.Helgedag -> GraphQLUtbetalingsdagType.Helgedag
        UtbetalingstidslinjedagType.Arbeidsdag -> GraphQLUtbetalingsdagType.Arbeidsdag
        UtbetalingstidslinjedagType.Feriedag -> GraphQLUtbetalingsdagType.Feriedag
        UtbetalingstidslinjedagType.AvvistDag -> GraphQLUtbetalingsdagType.AvvistDag
        UtbetalingstidslinjedagType.UkjentDag -> GraphQLUtbetalingsdagType.UkjentDag
        UtbetalingstidslinjedagType.ForeldetDag -> GraphQLUtbetalingsdagType.ForeldetDag
    },
    kilde = GraphQLSykdomsdagkilde(
        id = dag.kilde.id,
        type = when (dag.kilde.type) {
            SykdomstidslinjedagKildetype.Inntektsmelding -> GraphQLSykdomsdagkildetype.Inntektsmelding
            SykdomstidslinjedagKildetype.Søknad -> GraphQLSykdomsdagkildetype.Soknad
            SykdomstidslinjedagKildetype.Sykmelding -> GraphQLSykdomsdagkildetype.Sykmelding
            SykdomstidslinjedagKildetype.Saksbehandler -> GraphQLSykdomsdagkildetype.Saksbehandler
            SykdomstidslinjedagKildetype.Ukjent -> GraphQLSykdomsdagkildetype.Ukjent
        }
    ),
    grad = dag.grad,
    utbetalingsinfo = dag.utbetalingsinfo?.let {
        GraphQLUtbetalingsinfo(
            inntekt = it.inntekt,
            utbetaling = it.utbetaling,
            personbelop = it.personbeløp,
            arbeidsgiverbelop = it.arbeidsgiverbeløp,
            refusjonsbelop = it.refusjonsbeløp,
            totalGrad = it.totalGrad
        )
    },
    begrunnelser = dag.begrunnelser?.map {
        when (it) {
            BegrunnelseDTO.SykepengedagerOppbrukt -> GraphQLBegrunnelse.SykepengedagerOppbrukt
            BegrunnelseDTO.SykepengedagerOppbruktOver67 -> GraphQLBegrunnelse.SykepengedagerOppbruktOver67
            BegrunnelseDTO.MinimumInntekt -> GraphQLBegrunnelse.MinimumInntekt
            BegrunnelseDTO.MinimumInntektOver67 -> GraphQLBegrunnelse.MinimumInntektOver67
            BegrunnelseDTO.EgenmeldingUtenforArbeidsgiverperiode -> GraphQLBegrunnelse.EgenmeldingUtenforArbeidsgiverperiode
            BegrunnelseDTO.MinimumSykdomsgrad -> GraphQLBegrunnelse.MinimumSykdomsgrad
            BegrunnelseDTO.EtterDødsdato -> GraphQLBegrunnelse.EtterDodsdato
            BegrunnelseDTO.ManglerMedlemskap -> GraphQLBegrunnelse.ManglerMedlemskap
            BegrunnelseDTO.ManglerOpptjening -> GraphQLBegrunnelse.ManglerOpptjening
            BegrunnelseDTO.Over70 -> GraphQLBegrunnelse.Over70
        }
    }
)

private fun mapOppdrag(oppdrag: SpeilOppdrag): GraphQLOppdrag =
    GraphQLOppdrag(
        fagsystemId = oppdrag.fagsystemId,
        tidsstempel = oppdrag.tidsstempel,
        simulering = oppdrag.simulering?.let { simulering ->
            GraphQLSimulering(
                totalbelop = simulering.totalbeløp,
                perioder = simulering.perioder.map { periode ->
                    GraphQLSimuleringsperiode(
                        fom = periode.fom,
                        tom = periode.tom,
                        utbetalinger = periode.utbetalinger.map { utbetaling ->
                            GraphQLSimuleringsutbetaling(
                                utbetalesTilId = utbetaling.mottakerId,
                                utbetalesTilNavn = utbetaling.mottakerNavn,
                                forfall = utbetaling.forfall,
                                feilkonto = utbetaling.feilkonto,
                                detaljer = utbetaling.detaljer.map {
                                    GraphQLSimuleringsdetaljer(
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

private fun mapUtbetaling(utbetaling: Utbetaling) = GraphQLUtbetaling(
    id = utbetaling.id,
    type = utbetaling.type.toString(),
    typeEnum = utbetaling.type,
    status = utbetaling.status.toString(),
    statusEnum = when (utbetaling.status) {
        Utbetalingstatus.Annullert -> GraphQLUtbetalingstatus.Annullert
        Utbetalingstatus.Forkastet -> GraphQLUtbetalingstatus.Forkastet
        Utbetalingstatus.Godkjent -> GraphQLUtbetalingstatus.Godkjent
        Utbetalingstatus.GodkjentUtenUtbetaling -> GraphQLUtbetalingstatus.GodkjentUtenUtbetaling
        Utbetalingstatus.IkkeGodkjent -> GraphQLUtbetalingstatus.IkkeGodkjent
        Utbetalingstatus.Overført -> GraphQLUtbetalingstatus.Overfort
        Utbetalingstatus.Sendt -> GraphQLUtbetalingstatus.Sendt
        Utbetalingstatus.Ubetalt -> GraphQLUtbetalingstatus.Ubetalt
        Utbetalingstatus.UtbetalingFeilet -> GraphQLUtbetalingstatus.UtbetalingFeilet
        Utbetalingstatus.Utbetalt -> GraphQLUtbetalingstatus.Utbetalt
    },
    arbeidsgiverNettoBelop = utbetaling.arbeidsgiverNettoBeløp,
    personNettoBelop = utbetaling.personNettoBeløp,
    arbeidsgiverFagsystemId = utbetaling.arbeidsgiverFagsystemId,
    personFagsystemId = utbetaling.personFagsystemId,
    vurdering = utbetaling.vurdering?.let {
        GraphQLVurdering(
            godkjent = it.godkjent,
            tidsstempel = it.tidsstempel,
            automatisk = it.automatisk,
            ident = it.ident
        )
    },
    arbeidsgiveroppdrag = utbetaling.oppdrag[utbetaling.arbeidsgiverFagsystemId]?.let {
        mapOppdrag(it)
    },
    personoppdrag = utbetaling.oppdrag[utbetaling.personFagsystemId]?.let {
        mapOppdrag(it)
    }
)

private fun mapHendelse(hendelse: HendelseDTO) = when (hendelse) {
    is InntektsmeldingDTO -> GraphQLInntektsmelding(
        id = hendelse.id,
        mottattDato = hendelse.mottattDato,
        beregnetInntekt = hendelse.beregnetInntekt
    )
    is SøknadNavDTO -> GraphQLSoknadNav(
        id = hendelse.id,
        fom = hendelse.fom,
        tom = hendelse.tom,
        rapportertDato = hendelse.rapportertdato,
        sendtNav = hendelse.sendtNav
    )
    is SøknadArbeidsgiverDTO -> GraphQLSoknadArbeidsgiver(
        id = hendelse.id,
        fom = hendelse.fom,
        tom = hendelse.tom,
        rapportertDato = hendelse.rapportertdato,
        sendtArbeidsgiver = hendelse.sendtArbeidsgiver
    )
    is SykmeldingDTO -> GraphQLSykmelding(
        id = hendelse.id,
        fom = hendelse.fom,
        tom = hendelse.tom,
        rapportertDato = hendelse.rapportertdato
    )
    else -> object : GraphQLHendelse {
        override val id = hendelse.id
        override val type = GraphQLHendelsetype.Ukjent
    }
}

private fun mapPeriodevilkår(vilkår: BeregnetPeriode.Vilkår) = GraphQLPeriodevilkar(
    sykepengedager = vilkår.sykepengedager.let {
        GraphQLPeriodevilkar.Sykepengedager(
            skjaeringstidspunkt = it.skjæringstidspunkt,
            maksdato = it.maksdato,
            forbrukteSykedager = it.forbrukteSykedager,
            gjenstaendeSykedager = it.gjenståendeDager,
            oppfylt = it.oppfylt
        )
    },
    alder = vilkår.alder.let {
        GraphQLPeriodevilkar.Alder(
            alderSisteSykedag = it.alderSisteSykedag,
            oppfylt = it.oppfylt
        )
    },
    soknadsfrist = vilkår.søknadsfrist?.let {
        GraphQLPeriodevilkar.Soknadsfrist(
            sendtNav = it.sendtNav,
            soknadFom = it.søknadFom,
            soknadTom = it.søknadTom,
            oppfylt = it.oppfylt
        )
    }
)

private fun mapPeriodetype(type: Periodetype) = when (type) {
    Periodetype.FØRSTEGANGSBEHANDLING -> GraphQLPeriodetype.Forstegangsbehandling
    Periodetype.FORLENGELSE -> GraphQLPeriodetype.Forlengelse
    Periodetype.OVERGANG_FRA_IT -> GraphQLPeriodetype.OvergangFraIt
    Periodetype.INFOTRYGDFORLENGELSE -> GraphQLPeriodetype.Infotrygdforlengelse
}

private fun mapInntektstype(kilde: Inntektskilde) = when (kilde) {
    Inntektskilde.EN_ARBEIDSGIVER -> GraphQLInntektstype.EnArbeidsgiver
    Inntektskilde.FLERE_ARBEIDSGIVERE -> GraphQLInntektstype.FlereArbeidsgivere
}

internal fun mapTidslinjeperiode(periode: Tidslinjeperiode) =
    when (periode) {
        is BeregnetPeriode -> GraphQLBeregnetPeriode(
            fom = periode.fom,
            tom = periode.tom,
            tidslinje = periode.sammenslåttTidslinje.map { mapDag(it) },
            behandlingstype = when (periode.behandlingstype) {
                Behandlingstype.UBEREGNET -> GraphQLBehandlingstype.Uberegnet
                Behandlingstype.BEHANDLET -> GraphQLBehandlingstype.Behandlet
                Behandlingstype.VENTER -> GraphQLBehandlingstype.Venter
            },
            periodetype = mapPeriodetype(periode.periodetype),
            inntektstype = mapInntektstype(periode.inntektskilde),
            erForkastet = periode.erForkastet,
            opprettet = periode.opprettet,
            vedtaksperiodeId = periode.vedtaksperiodeId,
            beregningId = periode.beregningId,
            gjenstaendeSykedager = periode.gjenståendeSykedager,
            forbrukteSykedager = periode.forbrukteSykedager,
            skjaeringstidspunkt = periode.skjæringstidspunkt,
            maksdato = periode.maksdato,
            vilkarsgrunnlaghistorikkId = periode.vilkårsgrunnlagshistorikkId,
            utbetaling = mapUtbetaling(periode.utbetaling),
            hendelser = periode.hendelser.map { mapHendelse(it) },
            periodevilkar = mapPeriodevilkår(periode.periodevilkår),
            aktivitetslogg = periode.aktivitetslogg.map {
                GraphQLAktivitet(
                    vedtaksperiodeId = it.vedtaksperiodeId,
                    alvorlighetsgrad = it.alvorlighetsgrad,
                    melding = it.melding,
                    tidsstempel = it.tidsstempel
                )
            },
            refusjon = periode.refusjon?.let { refusjon ->
                GraphQLRefusjon(
                    arbeidsgiverperioder = refusjon.arbeidsgiverperioder.map {
                        GraphQLRefusjon.GraphQLRefusjonsperiode(
                            it.fom,
                            it.tom
                        )
                    },
                    endringer = refusjon.endringer.map { GraphQLRefusjon.GraphQLRefusjonsendring(it.beløp, it.dato) },
                    forsteFravaersdag = refusjon.førsteFraværsdag,
                    sisteRefusjonsdag = refusjon.sisteRefusjonsdag,
                    belop = refusjon.beløp
                )
            },
            tilstand = when (periode.tilstand) {
                Periodetilstand.TilUtbetaling -> GraphQLPeriodetilstand.TilUtbetaling
                Periodetilstand.TilAnnullering -> GraphQLPeriodetilstand.TilAnnullering
                Periodetilstand.Utbetalt -> GraphQLPeriodetilstand.Utbetalt
                Periodetilstand.Annullert -> GraphQLPeriodetilstand.Annullert
                Periodetilstand.AnnulleringFeilet -> GraphQLPeriodetilstand.AnnulleringFeilet
                Periodetilstand.Oppgaver -> GraphQLPeriodetilstand.Oppgaver
                Periodetilstand.Venter -> GraphQLPeriodetilstand.Venter
                Periodetilstand.VenterPåKiling -> GraphQLPeriodetilstand.VenterPaKiling
                Periodetilstand.IngenUtbetaling -> GraphQLPeriodetilstand.IngenUtbetaling
                Periodetilstand.KunFerie -> GraphQLPeriodetilstand.KunFerie
                Periodetilstand.Feilet -> GraphQLPeriodetilstand.Feilet
                Periodetilstand.RevurderingFeilet -> GraphQLPeriodetilstand.RevurderingFeilet
                Periodetilstand.TilInfotrygd -> GraphQLPeriodetilstand.TilInfotrygd
            }
        )
        else -> GraphQLUberegnetPeriode(
            fom = periode.fom,
            tom = periode.tom,
            tidslinje = periode.sammenslåttTidslinje.map { mapDag(it) },
            behandlingstype = when (periode.behandlingstype) {
                Behandlingstype.UBEREGNET -> GraphQLBehandlingstype.Uberegnet
                Behandlingstype.BEHANDLET -> GraphQLBehandlingstype.Behandlet
                Behandlingstype.VENTER -> GraphQLBehandlingstype.Venter
            },
            periodetype = mapPeriodetype(periode.periodetype),
            inntektstype = mapInntektstype(periode.inntektskilde),
            erForkastet = periode.erForkastet,
            opprettet = periode.opprettet,
            vedtaksperiodeId = periode.vedtaksperiodeId,
        )
    }

private fun mapInntekt(inntekt: Arbeidsgiverinntekt) = GraphQLArbeidsgiverinntekt(
    arbeidsgiver = inntekt.organisasjonsnummer,
    omregnetArsinntekt = inntekt.omregnetÅrsinntekt?.let { omregnetÅrsinntekt ->
        GraphQLOmregnetArsinntekt(
            kilde = when (omregnetÅrsinntekt.kilde) {
                Inntektkilde.Saksbehandler -> GraphQLInntektskilde.Saksbehandler
                Inntektkilde.Inntektsmelding -> GraphQLInntektskilde.Inntektsmelding
                Inntektkilde.Infotrygd -> GraphQLInntektskilde.Infotrygd
                Inntektkilde.AOrdningen -> GraphQLInntektskilde.AOrdningen
                Inntektkilde.IkkeRapportert -> GraphQLInntektskilde.IkkeRapportert
            },
            belop = omregnetÅrsinntekt.beløp,
            manedsbelop = omregnetÅrsinntekt.månedsbeløp,
            inntekterFraAOrdningen = omregnetÅrsinntekt.inntekterFraAOrdningen?.map {
                GraphQLInntekterFraAOrdningen(
                    maned = it.måned,
                    sum = it.sum
                )
            }
        )
    },
    sammenligningsgrunnlag = inntekt.sammenligningsgrunnlag?.let {
        GraphQLSammenligningsgrunnlag(
            belop = it,
            inntekterFraAOrdningen = emptyList()
        )
    },
    deaktivert = inntekt.deaktivert
)

internal fun mapVilkårsgrunnlag(id: UUID, vilkårsgrunnlag: List<Vilkårsgrunnlag>) =
    GraphQLVilkarsgrunnlaghistorikk(
        id = id,
        grunnlag = vilkårsgrunnlag.map { grunnlag ->
            when (grunnlag) {
                is SpleisVilkårsgrunnlag -> GraphQLSpleisVilkarsgrunnlag(
                    skjaeringstidspunkt = grunnlag.skjæringstidspunkt,
                    omregnetArsinntekt = grunnlag.omregnetÅrsinntekt,
                    sammenligningsgrunnlag = grunnlag.sammenligningsgrunnlag,
                    sykepengegrunnlag = grunnlag.sykepengegrunnlag,
                    inntekter = grunnlag.inntekter.map { inntekt -> mapInntekt(inntekt) },
                    avviksprosent = grunnlag.avviksprosent,
                    grunnbelop = grunnlag.grunnbeløp,
                    antallOpptjeningsdagerErMinst = grunnlag.antallOpptjeningsdagerErMinst,
                    opptjeningFra = grunnlag.opptjeningFra,
                    oppfyllerKravOmMinstelonn = grunnlag.oppfyllerKravOmMinstelønn,
                    oppfyllerKravOmOpptjening = grunnlag.oppfyllerKravOmOpptjening,
                    oppfyllerKravOmMedlemskap = grunnlag.oppfyllerKravOmMedlemskap
                )
                is InfotrygdVilkårsgrunnlag -> GraphQLInfotrygdVilkarsgrunnlag(
                    skjaeringstidspunkt = grunnlag.skjæringstidspunkt,
                    omregnetArsinntekt = grunnlag.omregnetÅrsinntekt,
                    sammenligningsgrunnlag = grunnlag.sammenligningsgrunnlag,
                    sykepengegrunnlag = grunnlag.sykepengegrunnlag,
                    inntekter = grunnlag.inntekter.map { inntekt -> mapInntekt(inntekt) }
                )
                else -> object : GraphQLVilkarsgrunnlag {
                    override val skjaeringstidspunkt = grunnlag.skjæringstidspunkt
                    override val omregnetArsinntekt = grunnlag.omregnetÅrsinntekt
                    override val sammenligningsgrunnlag = grunnlag.sammenligningsgrunnlag
                    override val sykepengegrunnlag = grunnlag.sykepengegrunnlag
                    override val inntekter = grunnlag.inntekter.map { inntekt -> mapInntekt(inntekt) }
                    override val vilkarsgrunnlagtype = GraphQLVilkarsgrunnlagtype.Ukjent
                }
            }
        }
    )

internal fun mapInntektsgrunnlag(inntektsgrunnlag: InntektsgrunnlagDTO) = GraphQLInntektsgrunnlag(
    skjaeringstidspunkt = inntektsgrunnlag.skjæringstidspunkt,
    sykepengegrunnlag = inntektsgrunnlag.sykepengegrunnlag,
    omregnetArsinntekt = inntektsgrunnlag.omregnetÅrsinntekt,
    sammenligningsgrunnlag = inntektsgrunnlag.sammenligningsgrunnlag,
    avviksprosent = inntektsgrunnlag.avviksprosent,
    maksUtbetalingPerDag = inntektsgrunnlag.maksUtbetalingPerDag,
    inntekter = inntektsgrunnlag.inntekter.map { inntekt ->
        GraphQLArbeidsgiverinntekt(
            arbeidsgiver = inntekt.arbeidsgiver,
            omregnetArsinntekt = inntekt.omregnetÅrsinntekt?.let { årsinntekt ->
                GraphQLOmregnetArsinntekt(
                    kilde = when (årsinntekt.kilde) {
                        InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO.Saksbehandler -> GraphQLInntektskilde.Saksbehandler
                        InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO.Inntektsmelding -> GraphQLInntektskilde.Inntektsmelding
                        InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO.Infotrygd -> GraphQLInntektskilde.Infotrygd
                        InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO.AOrdningen -> GraphQLInntektskilde.AOrdningen
                        InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO.IkkeRapportert -> GraphQLInntektskilde.IkkeRapportert
                    },
                    belop = årsinntekt.beløp,
                    manedsbelop = årsinntekt.månedsbeløp,
                    inntekterFraAOrdningen = årsinntekt.inntekterFraAOrdningen?.map {
                        GraphQLInntekterFraAOrdningen(
                            maned = it.måned,
                            sum = it.sum
                        )
                    }
                )
            },
            sammenligningsgrunnlag = inntekt.sammenligningsgrunnlag?.let { sammenligningsgrunnlag ->
                GraphQLSammenligningsgrunnlag(
                    belop = sammenligningsgrunnlag.beløp,
                    inntekterFraAOrdningen = sammenligningsgrunnlag.inntekterFraAOrdningen.map {
                        GraphQLInntekterFraAOrdningen(
                            maned = it.måned,
                            sum = it.sum
                        )
                    }
                )
            }
        )
    },
    oppfyllerKravOmMinstelonn = inntektsgrunnlag.oppfyllerKravOmMinstelønn,
    grunnbelop = inntektsgrunnlag.grunnbeløp
)
