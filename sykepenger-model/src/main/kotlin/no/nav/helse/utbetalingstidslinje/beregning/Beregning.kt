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

/**
 * Steg 1: Lage utbetalingstidslinje-representasjon for hver vedtaksperiode og yrkesaktivitet, og sette inntekt/refusjon på dagene
 * Steg 2: Filtrere utbetalingstidslinjene (avvise dager)
 * Steg 3: Beregne utbetaling
 */
fun beregnUtbetalinger(request: BeregningRequest): BeregningResponse {
    val beregnetVedtaksperioder = beregnVedtaksperioder(request)
    return BeregningResponse(
        yrkesaktiviteter = beregnetVedtaksperioder
    )
}

private fun beregnVedtaksperioder(request: BeregningRequest): List<BeregningResponse.BeregnetYrkesaktivitet> {
    val alleInntektsjusteringer = request
        .yrkesaktiviteter
        .associateBy({ it.yrkesaktivitet }) {
            it
                .perioder
                .fold(Beløpstidslinje()) { result, beregningsperiode -> result + beregningsperiode.inntektsjusteringer }
        }
    return request.yrkesaktiviteter.map { yrkesaktivitet ->
        val vedtaksperioder = yrkesaktivitet.perioder
            .filterIsInstance<BeregningRequest.VedtaksperiodeForBeregning>()
            .map { beregningsperiode -> beregnVedtaksperiode(beregningsperiode, alleInntektsjusteringer) }

        val ghosts = yrkesaktivitet.perioder
            .filterIsInstance<BeregningRequest.Ghostperiode>()
            .map { beregningsperiode -> beregnGhost(beregningsperiode.periode, beregningsperiode.inntekt, beregningsperiode.inntektsjusteringer)
        }
        val andreInntektskilder = yrkesaktivitet.perioder
            .filterIsInstance<BeregningRequest.AnnenInntektsperiode>()
            .map { beregningsperiode -> beregnGhost(beregningsperiode.periode, Inntekt.INGEN, beregningsperiode.inntektsjusteringer)
        }

        BeregningResponse.BeregnetYrkesaktivitet(
            yrkesaktivitet = yrkesaktivitet.yrkesaktivitet,
            vedtaksperioder = vedtaksperioder,
            ghostOgAndreInntektskilder = ghosts + andreInntektskilder
        )
    }
}

private fun beregnVedtaksperiode(vedtaksperiode: BeregningRequest.VedtaksperiodeForBeregning, alleInntektsjusteringer: Map<Yrkesaktivitet, Beløpstidslinje>): BeregningResponse.BeregnetVedtaksperiode {
    val utbetalingstidslinje = when (vedtaksperiode.dataForBeregning) {
        is BeregningRequest.VedtaksperiodeForBeregning.DataForBeregning.Arbeidstaker -> beregnArbeidstaker(vedtaksperiode.sykdomstidslinje, vedtaksperiode.inntekt, vedtaksperiode.inntektsjusteringer, vedtaksperiode.dataForBeregning)
        is BeregningRequest.VedtaksperiodeForBeregning.DataForBeregning.Selvstendig -> beregnSelvstendig(vedtaksperiode.sykdomstidslinje, vedtaksperiode.inntekt, vedtaksperiode.dataForBeregning)
    }
    return BeregningResponse.BeregnetVedtaksperiode(
        vedtaksperiodeId = vedtaksperiode.vedtaksperiodeId,
        utbetalingstidslinje = utbetalingstidslinje,
        inntektjusteringer = alleInntektsjusteringer
    )
}

private fun beregnGhost(ghostperiode: Periode, inntekt: Inntekt, inntektsjustering: Beløpstidslinje): Utbetalingstidslinje {
    // nå vi må lage en ghost-tidslinje per arbeidsgiver for de som eksisterer i sykepengegrunnlaget.
    // i tillegg må vi lage én tidslinje per inntektskilde som ikke er en del av sykepengegrunnlaget
    // resultatet er én utbetalingstidslinje per arbeidsgiver som garantert dekker perioden ${vedtaksperiode.periode}, dog kan
    // andre arbeidsgivere dekke litt før/litt etter, avhengig av perioden til vedtaksperiodene som overlapper
    return with(Utbetalingstidslinje.Builder()) {
        ghostperiode.forEach { dato ->
            if (dato.erHelg()) addFridag(dato, Økonomi.ikkeBetalt())
            else addArbeidsdag(
                dato = dato,
                økonomi = Økonomi.ikkeBetalt(
                    aktuellDagsinntekt = inntekt,
                    inntektjustering = (inntektsjustering[dato] as? Beløpsdag)?.beløp ?: Inntekt.INGEN
                ),
            )
        }
        build()
    }
}

private fun beregnArbeidstaker(sykdomstidslinje: Sykdomstidslinje, fastsattÅrsinntekt: Inntekt?, inntektsjustering: Beløpstidslinje, dataForBeregning: BeregningRequest.VedtaksperiodeForBeregning.DataForBeregning.Arbeidstaker): Utbetalingstidslinje {
    return ArbeidstakerUtbetalingstidslinjeBuilderVedtaksperiode(
        arbeidsgiverperiode = dataForBeregning.arbeidsgiverperiode,
        dagerNavOvertarAnsvar = dataForBeregning.dagerNavOvertarAnsvar,
        refusjonstidslinje = dataForBeregning.refusjonstidslinje,
        fastsattÅrsinntekt = fastsattÅrsinntekt ?: Inntekt.INGEN,
        inntektjusteringer = inntektsjustering
    ).result(sykdomstidslinje)
}

private fun beregnSelvstendig(sykdomstidslinje: Sykdomstidslinje, selvstendigNæringsdrivende: Inntekt?, dataForBeregning: BeregningRequest.VedtaksperiodeForBeregning.DataForBeregning.Selvstendig): Utbetalingstidslinje {
    return SelvstendigUtbetalingstidslinjeBuilderVedtaksperiode(selvstendigNæringsdrivende ?: Inntekt.INGEN).result(sykdomstidslinje, dataForBeregning.ventetid)
}
