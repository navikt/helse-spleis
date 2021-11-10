package no.nav.helse.spleis.graphql

import no.nav.helse.serde.api.SimuleringsdataDTO
import no.nav.helse.serde.api.v2.*

private fun mapDag(dag: SammenslåttDag) = GraphQLDag(
    dato = dag.dagen,
    sykdomsdagtype = dag.sykdomstidslinjedagtype,
    utbetalingsdagtype = dag.utbetalingstidslinjedagtype,
    kilde = dag.kilde,
    grad = dag.grad,
    utbetalingsinfo = dag.utbetalingsinfo,
    begrunnelser = dag.begrunnelser
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

fun mapTidslinjeperiode(periode: Tidslinjeperiode) =
    when (periode) {
        is BeregnetPeriode -> GraphQLBeregnetPeriode(
            fom = periode.fom,
            tom = periode.tom,
            tidslinje = periode.sammenslåttTidslinje.map { mapDag(it) },
            behandlingstype = periode.behandlingstype,
            periodetype = periode.periodetype,
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
            periodetype = periode.periodetype,
            inntektskilde = periode.inntektskilde,
            erForkastet = periode.erForkastet,
            opprettet = periode.opprettet
        )
    }
