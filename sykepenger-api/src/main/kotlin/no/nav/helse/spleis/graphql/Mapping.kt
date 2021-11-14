package no.nav.helse.spleis.graphql

import no.nav.helse.person.Periodetype
import no.nav.helse.serde.api.BegrunnelseDTO
import no.nav.helse.serde.api.InntektsgrunnlagDTO
import no.nav.helse.serde.api.SimuleringsdataDTO
import no.nav.helse.serde.api.v2.*
import java.util.*

private fun mapDag(dag: SammenslåttDag) = GraphQLDag(
    dato = dag.dagen,
    sykdomsdagtype = when (dag.sykdomstidslinjedagtype) {
        SykdomstidslinjedagType.ARBEIDSDAG -> GraphQLSykdomsdagtype.ARBEIDSDAG
        SykdomstidslinjedagType.ARBEIDSGIVERDAG -> GraphQLSykdomsdagtype.ARBEIDSGIVERDAG
        SykdomstidslinjedagType.FERIEDAG -> GraphQLSykdomsdagtype.FERIEDAG
        SykdomstidslinjedagType.FORELDET_SYKEDAG -> GraphQLSykdomsdagtype.FORELDET_SYKEDAG
        SykdomstidslinjedagType.FRISK_HELGEDAG -> GraphQLSykdomsdagtype.FRISK_HELGEDAG
        SykdomstidslinjedagType.PERMISJONSDAG -> GraphQLSykdomsdagtype.PERMISJONSDAG
        SykdomstidslinjedagType.SYKEDAG -> GraphQLSykdomsdagtype.SYKEDAG
        SykdomstidslinjedagType.SYK_HELGEDAG -> GraphQLSykdomsdagtype.SYK_HELGEDAG
        SykdomstidslinjedagType.UBESTEMTDAG -> GraphQLSykdomsdagtype.UBESTEMTDAG
        SykdomstidslinjedagType.AVSLÅTT -> GraphQLSykdomsdagtype.AVSLATT
    },
    utbetalingsdagtype = dag.utbetalingstidslinjedagtype,
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

private fun mapUtbetaling(utbetaling: Utbetaling) = GraphQLUtbetaling(
    type = utbetaling.type,
    status = utbetaling.status,
    arbeidsgiverNettoBelop = utbetaling.arbeidsgiverNettoBeløp,
    personNettoBelop = utbetaling.personNettoBeløp,
    arbeidsgiverFagsystemId = utbetaling.arbeidsgiverFagsystemId,
    personFagsystemId = utbetaling.personFagsystemId,
    vurdering = utbetaling.vurdering?.let {
        GraphQLUtbetaling.Vurdering(
            godkjent = it.godkjent,
            tidsstempel = it.tidsstempel,
            automatisk = it.automatisk,
            ident = it.ident
        )
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
        override val type = GraphQLHendelsetype.UKJENT
    }
}

private fun mapSimulering(simulering: SimuleringsdataDTO) = GraphQLSimulering(
    totalbelop = simulering.totalbeløp,
    perioder = simulering.perioder.map { periode ->
        GraphQLSimulering.Periode(
            fom = periode.fom,
            tom = periode.tom,
            utbetalinger = periode.utbetalinger.map { utbetaling ->
                GraphQLSimulering.Utbetaling(
                    utbetalesTilId = utbetaling.utbetalesTilId,
                    utbetalesTilNavn = utbetaling.utbetalesTilNavn,
                    forfall = utbetaling.forfall,
                    feilkonto = utbetaling.feilkonto,
                    detaljer = utbetaling.detaljer.map {
                        GraphQLSimulering.Detaljer(
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

private fun mapPeriodevilkår(vilkår: BeregnetPeriode.Vilkår) = GraphQLBeregnetPeriode.Vilkar(
    sykepengedager = vilkår.sykepengedager.let {
        GraphQLBeregnetPeriode.Sykepengedager(
            skjaeringstidspunkt = it.skjæringstidspunkt,
            maksdato = it.maksdato,
            forbrukteSykedager = it.forbrukteSykedager,
            gjenstaendeSykedager = it.gjenståendeDager,
            oppfylt = it.oppfylt
        )
    },
    alder = vilkår.alder.let {
        GraphQLBeregnetPeriode.Alder(
            alderSisteSykedag = it.alderSisteSykedag,
            oppfylt = it.oppfylt
        )
    },
    soknadsfrist = vilkår.søknadsfrist?.let {
        GraphQLBeregnetPeriode.Soknadsfrist(
            sendtNav = it.sendtNav,
            soknadFom = it.søknadFom,
            soknadTom = it.søknadTom,
            oppfylt = it.oppfylt
        )
    }
)

private fun mapPeriodetype(type: Periodetype) = when (type) {
    Periodetype.FØRSTEGANGSBEHANDLING -> GraphQLPeriodetype.FORSTEGANGSBEHANDLING
    Periodetype.FORLENGELSE -> GraphQLPeriodetype.FORLENGELSE
    Periodetype.OVERGANG_FRA_IT -> GraphQLPeriodetype.OVERGANG_FRA_IT
    Periodetype.INFOTRYGDFORLENGELSE -> GraphQLPeriodetype.INFOTRYGDFORLENGELSE
}

internal fun mapTidslinjeperiode(periode: Tidslinjeperiode) =
    when (periode) {
        is BeregnetPeriode -> GraphQLBeregnetPeriode(
            fom = periode.fom,
            tom = periode.tom,
            tidslinje = periode.sammenslåttTidslinje.map { mapDag(it) },
            behandlingstype = periode.behandlingstype,
            periodetype = mapPeriodetype(periode.periodetype),
            inntektskilde = periode.inntektskilde,
            erForkastet = periode.erForkastet,
            opprettet = periode.opprettet,
            beregningId = periode.beregningId,
            gjenstaendeSykedager = periode.gjenståendeSykedager,
            forbrukteSykedager = periode.forbrukteSykedager,
            skjaeringstidspunkt = periode.skjæringstidspunkt,
            maksdato = periode.maksdato,
            vilkarsgrunnlaghistorikkId = periode.vilkårsgrunnlagshistorikkId,
            utbetaling = mapUtbetaling(periode.utbetaling),
            hendelser = periode.hendelser.map { mapHendelse(it) },
            simulering = periode.simulering?.let { mapSimulering(it) },
            periodevilkar = mapPeriodevilkår(periode.periodevilkår),
            aktivitetslogg = periode.aktivitetslogg
        )
        else -> GraphQLUberegnetPeriode(
            fom = periode.fom,
            tom = periode.tom,
            tidslinje = periode.sammenslåttTidslinje.map { mapDag(it) },
            behandlingstype = periode.behandlingstype,
            periodetype = mapPeriodetype(periode.periodetype),
            inntektskilde = periode.inntektskilde,
            erForkastet = periode.erForkastet,
            opprettet = periode.opprettet
        )
    }

private fun mapInntekt(inntekt: Arbeidsgiverinntekt) = GraphQLPerson.VilkarsgrunnlaghistorikkInnslag.Arbeidsgiverinntekt(
    arbeidsgiver = inntekt.organisasjonsnummer,
    omregnetArsinntekt = inntekt.omregnetÅrsinntekt?.let { omregnetÅrsinntekt ->
        GraphQLPerson.VilkarsgrunnlaghistorikkInnslag.Arbeidsgiverinntekt.OmregnetArsinntekt(
            kilde = when (omregnetÅrsinntekt.kilde) {
                Inntektkilde.Saksbehandler -> GraphQLPerson.VilkarsgrunnlaghistorikkInnslag.Arbeidsgiverinntekt.OmregnetArsinntektKilde.Saksbehandler
                Inntektkilde.Inntektsmelding -> GraphQLPerson.VilkarsgrunnlaghistorikkInnslag.Arbeidsgiverinntekt.OmregnetArsinntektKilde.Inntektsmelding
                Inntektkilde.Infotrygd -> GraphQLPerson.VilkarsgrunnlaghistorikkInnslag.Arbeidsgiverinntekt.OmregnetArsinntektKilde.Infotrygd
                Inntektkilde.AOrdningen -> GraphQLPerson.VilkarsgrunnlaghistorikkInnslag.Arbeidsgiverinntekt.OmregnetArsinntektKilde.AOrdningen
            },
            belop = omregnetÅrsinntekt.beløp,
            manedsbelop = omregnetÅrsinntekt.månedsbeløp,
            inntekterFraAOrdningen = omregnetÅrsinntekt.inntekterFraAOrdningen?.map {
                InntekterFraAOrdningen(
                    maned = it.måned,
                    sum = it.sum
                )
            }
        )
    },
    sammenligningsgrunnlag = inntekt.sammenligningsgrunnlag
)

internal fun mapVilkårsgrunnlag(id: UUID, vilkårsgrunnlag: List<Vilkårsgrunnlag>) =
    GraphQLPerson.VilkarsgrunnlaghistorikkInnslag(
        id = id,
        grunnlag = vilkårsgrunnlag.map { grunnlag ->
            when (grunnlag) {
                is SpleisVilkårsgrunnlag -> GraphQLPerson.VilkarsgrunnlaghistorikkInnslag.SpleisVilkarsgrunnlag(
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
                is InfotrygdVilkårsgrunnlag -> GraphQLPerson.VilkarsgrunnlaghistorikkInnslag.InfotrygdVilkarsgrunnlag(
                    skjaeringstidspunkt = grunnlag.skjæringstidspunkt,
                    omregnetArsinntekt = grunnlag.omregnetÅrsinntekt,
                    sammenligningsgrunnlag = grunnlag.sammenligningsgrunnlag,
                    sykepengegrunnlag = grunnlag.sykepengegrunnlag,
                    inntekter = grunnlag.inntekter.map { inntekt -> mapInntekt(inntekt) }
                )
                else -> object : GraphQLVilkarsgrunnlagElement {
                    override val skjaeringstidspunkt = grunnlag.skjæringstidspunkt
                    override val omregnetArsinntekt = grunnlag.omregnetÅrsinntekt
                    override val sammenligningsgrunnlag = grunnlag.sammenligningsgrunnlag
                    override val sykepengegrunnlag = grunnlag.sykepengegrunnlag
                    override val inntekter = grunnlag.inntekter.map { inntekt -> mapInntekt(inntekt) }
                    override val vilkarsgrunnlagtype = GraphQLVilkarsgrunnlagtype.UKJENT
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
        GraphQLInntektsgrunnlag.Arbeidsgiverinntekt(
            arbeidsgiver = inntekt.arbeidsgiver,
            omregnetArsinntekt = inntekt.omregnetÅrsinntekt?.let { årsinntekt ->
                GraphQLInntektsgrunnlag.Arbeidsgiverinntekt.OmregnetArsinntekt(
                    kilde = when (årsinntekt.kilde) {
                        InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO.Saksbehandler -> GraphQLInntektsgrunnlag.Arbeidsgiverinntekt.OmregnetArsinntekt.Kilde.Saksbehandler
                        InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO.Inntektsmelding -> GraphQLInntektsgrunnlag.Arbeidsgiverinntekt.OmregnetArsinntekt.Kilde.Inntektsmelding
                        InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO.Infotrygd -> GraphQLInntektsgrunnlag.Arbeidsgiverinntekt.OmregnetArsinntekt.Kilde.Infotrygd
                        InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO.AOrdningen -> GraphQLInntektsgrunnlag.Arbeidsgiverinntekt.OmregnetArsinntekt.Kilde.AOrdningen
                    },
                    belop = årsinntekt.beløp,
                    manedsbelop = årsinntekt.månedsbeløp,
                    inntekterFraAOrdningen = årsinntekt.inntekterFraAOrdningen?.map {
                        InntekterFraAOrdningen(
                            maned = it.måned,
                            sum = it.sum
                        )
                    }
                )
            },
            sammenligningsgrunnlag = inntekt.sammenligningsgrunnlag?.let { sammenligningsgrunnlag ->
                GraphQLInntektsgrunnlag.Arbeidsgiverinntekt.Sammenligningsgrunnlag(
                    belop = sammenligningsgrunnlag.beløp,
                    inntekterFraAOrdningen = sammenligningsgrunnlag.inntekterFraAOrdningen.map {
                        InntekterFraAOrdningen(
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
