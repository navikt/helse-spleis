package no.nav.helse.utbetalingstidslinje

import java.util.*
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Behandlingsporing.Yrkesaktivitet.Arbeidsledig.somOrganisasjonsnummer
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.inntekt.InntekterForBeregning
import no.nav.helse.person.inntekt.Inntektskilde
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.økonomi.Inntekt

data class Vedtaksperiodeberegning(
    val vedtaksperiodeId: UUID,
    val utbetalingstidslinje: Utbetalingstidslinje,
    val inntekterForBeregning: Map<Inntektskilde, Beløpstidslinje>
) {
    val periode = utbetalingstidslinje.periode()
}

internal data class UberegnetVedtaksperiode(
    val vedtaksperiodeId: UUID,
    val yrkesaktivitet: Behandlingsporing.Yrkesaktivitet,
    val periode: Periode,
    val sykdomstidslinje: Sykdomstidslinje,
    val utbetalingstidslinjeBuilder: UtbetalingstidslinjeBuilder
)

internal fun lagUtbetalingstidslinjePerArbeidsgiver(vedtaksperioder: List<UberegnetVedtaksperiode>, inntekterForBeregning: InntekterForBeregning): List<Arbeidsgiverberegning> {
    val utbetalingstidslinjer = vedtaksperioder
        .groupBy({ it.yrkesaktivitet }) { vedtaksperiode ->
            val (fastsattÅrsinntekt, inntektjusteringer) = inntekterForBeregning.tilBeregning(vedtaksperiode.yrkesaktivitet)
            val alleInntektjusteringer = inntekterForBeregning.inntektsjusteringer(vedtaksperiode.periode)
                .mapKeys { (yrkesaktivitet, _) -> Inntektskilde(yrkesaktivitet.somOrganisasjonsnummer) }
            lagUtbetalingstidslinjeForVedtaksperiode(vedtaksperiode, fastsattÅrsinntekt, inntektjusteringer, alleInntektjusteringer)
        }
        .map { (yrkesaktivitet, vedtaksperioder) ->
            Arbeidsgiverberegning(
                yrkesaktivitet = yrkesaktivitet,
                vedtaksperioder = vedtaksperioder,
                ghostOgAndreInntektskilder = emptyList()
            )
        }
    // nå vi må lage en ghost-tidslinje per arbeidsgiver for de som eksisterer i sykepengegrunnlaget.
    // i tillegg må vi lage én tidslinje per inntektskilde som ikke er en del av sykepengegrunnlaget
    // resultatet er én utbetalingstidslinje per arbeidsgiver som garantert dekker perioden ${vedtaksperiode.periode}, dog kan
    // andre arbeidsgivere dekke litt før/litt etter, avhengig av perioden til vedtaksperiodene som overlapper
    return inntekterForBeregning.hensyntattAlleInntektskilder(utbetalingstidslinjer)
}

private fun lagUtbetalingstidslinjeForVedtaksperiode(vedtaksperiode: UberegnetVedtaksperiode, inntekt: Inntekt, inntektjusteringer: Beløpstidslinje, alleInntektjusteringer: Map<Inntektskilde, Beløpstidslinje>): Vedtaksperiodeberegning {
    return Vedtaksperiodeberegning(
        vedtaksperiodeId = vedtaksperiode.vedtaksperiodeId,
        utbetalingstidslinje = vedtaksperiode.utbetalingstidslinjeBuilder.result(vedtaksperiode.sykdomstidslinje, inntekt, inntektjusteringer),
        inntekterForBeregning = alleInntektjusteringer
    )
}
