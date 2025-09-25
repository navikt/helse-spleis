package no.nav.helse.utbetalingstidslinje.beregning

import no.nav.helse.erHelg
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.beløp.Beløpsdag
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingstidslinje.ArbeidstakerUtbetalingstidslinjeBuilderVedtaksperiode
import no.nav.helse.utbetalingstidslinje.SelvstendigUtbetalingstidslinjeBuilderVedtaksperiode
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Økonomi

fun beregnUtbetalinger(request: BeregningRequest): BeregningResponse {
    val beregnetVedtaksperioder = beregnVedtaksperioder(request)
    val hensyntattAlleInntektskilder = beregnAlleInntektskilder(request, beregnetVedtaksperioder)
    return BeregningResponse(
        yrkesaktiviteter = hensyntattAlleInntektskilder
    )
}

private fun beregnVedtaksperioder(request: BeregningRequest): List<BeregningResponse.BeregnetVedtaksperiode> {
    return request.perioderSomMåHensyntasVedBeregning.map { vedtaksperiode ->
        val utbetalingstidslinje = when (vedtaksperiode.dataForBeregning) {
            is BeregningRequest.VedtaksperiodeForBeregning.DataForBeregning.Arbeidstaker -> beregnArbeidstaker(request, vedtaksperiode.sykdomstidslinje, vedtaksperiode.dataForBeregning)
            is BeregningRequest.VedtaksperiodeForBeregning.DataForBeregning.Selvstendig -> beregnSelvstendig(request, vedtaksperiode.sykdomstidslinje, vedtaksperiode.dataForBeregning)
        }
        BeregningResponse.BeregnetVedtaksperiode(
            vedtaksperiodeId = vedtaksperiode.vedtaksperiodeId,
            utbetalingstidslinje = utbetalingstidslinje,
            inntektjusteringer = request.inntektjusteringer
                .mapValues { (_, inntektjustering) -> inntektjustering.subset(vedtaksperiode.periode).medBeløp() }
                .filterValues { it.isNotEmpty() }
        )
    }
}

private fun beregnAlleInntektskilder(request: BeregningRequest, beregnetVedtaksperioder: List<BeregningResponse.BeregnetVedtaksperiode>): List<BeregningResponse.BeregnetYrkesaktivitet> {
    // nå vi må lage en ghost-tidslinje per arbeidsgiver for de som eksisterer i sykepengegrunnlaget.
    // i tillegg må vi lage én tidslinje per inntektskilde som ikke er en del av sykepengegrunnlaget
    // resultatet er én utbetalingstidslinje per arbeidsgiver som garantert dekker perioden ${vedtaksperiode.periode}, dog kan
    // andre arbeidsgivere dekke litt før/litt etter, avhengig av perioden til vedtaksperiodene som overlapper
    return request.alleInntektskilder.map { yrkesaktivitet ->
        val vedtaksperioder = request.perioderSomMåHensyntasVedBeregning
            .filter { it.dataForBeregning.yrkesaktivitet == yrkesaktivitet }
            .map { (vedtaksperiodeId) -> beregnetVedtaksperioder.single { it.vedtaksperiodeId == vedtaksperiodeId } }

        val beregendePerioderForInntektskilde = vedtaksperioder.map { it.periode }
        val uberegnedeDagerForArbeidsgiver = request.beregningsperiode.uten(beregendePerioderForInntektskilde)
        val ghostOgAndreInntektskilder = arbeidsdager(request, yrkesaktivitet, uberegnedeDagerForArbeidsgiver)

        BeregningResponse.BeregnetYrkesaktivitet(
            yrkesaktivitet = yrkesaktivitet,
            vedtaksperioder = vedtaksperioder,
            ghostOgAndreInntektskilder = ghostOgAndreInntektskilder
        )
    }
}

private fun beregnArbeidstaker(request: BeregningRequest, sykdomstidslinje: Sykdomstidslinje, dataForBeregning: BeregningRequest.VedtaksperiodeForBeregning.DataForBeregning.Arbeidstaker): Utbetalingstidslinje {
    val fastsattÅrsinntekt = request.fastsatteÅrsinntekter[dataForBeregning.yrkesaktivitet] ?: Inntekt.INGEN
    val inntektsjustering = request.inntektjusteringer[dataForBeregning.yrkesaktivitet] ?: Beløpstidslinje()

    return ArbeidstakerUtbetalingstidslinjeBuilderVedtaksperiode(
        arbeidsgiverperiode = dataForBeregning.arbeidsgiverperiode,
        dagerNavOvertarAnsvar = dataForBeregning.dagerNavOvertarAnsvar,
        refusjonstidslinje = dataForBeregning.refusjonstidslinje,
        fastsattÅrsinntekt = fastsattÅrsinntekt,
        inntektjusteringer = inntektsjustering
    ).result(sykdomstidslinje)
}

private fun beregnSelvstendig(request: BeregningRequest, sykdomstidslinje: Sykdomstidslinje, dataForBeregning: BeregningRequest.VedtaksperiodeForBeregning.DataForBeregning.Selvstendig): Utbetalingstidslinje {
    val fastsattÅrsinntekt = request.selvstendigNæringsdrivende ?: Inntekt.INGEN
    return SelvstendigUtbetalingstidslinjeBuilderVedtaksperiode(fastsattÅrsinntekt).result(sykdomstidslinje, dataForBeregning.ventetid)
}

private fun arbeidsdager(request: BeregningRequest, yrkesaktivitet: Yrkesaktivitet, perioderMedArbeid: List<Periode>) = perioderMedArbeid.map { periode ->
    val fastsattÅrsinntekt = when (yrkesaktivitet) {
        is Yrkesaktivitet.Arbeidstaker -> request.fastsatteÅrsinntekter[yrkesaktivitet]
        Yrkesaktivitet.Selvstendig -> request.selvstendigNæringsdrivende
    } ?: Inntekt.INGEN
    val inntektsjustering = request.inntektjusteringer[yrkesaktivitet] ?: Beløpstidslinje()

    with(Utbetalingstidslinje.Builder()) {
        periode.forEach { dato ->
            if (dato.erHelg()) addFridag(dato, Økonomi.ikkeBetalt())
            else addArbeidsdag(
                dato = dato,
                økonomi = Økonomi.ikkeBetalt(
                    aktuellDagsinntekt = fastsattÅrsinntekt,
                    inntektjustering = (inntektsjustering[dato] as? Beløpsdag)?.beløp ?: Inntekt.INGEN
                ),
            )
        }
        build()
    }
}
