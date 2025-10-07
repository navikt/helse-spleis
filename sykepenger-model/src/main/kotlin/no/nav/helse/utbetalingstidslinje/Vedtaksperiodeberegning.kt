package no.nav.helse.utbetalingstidslinje

import java.util.*
import no.nav.helse.hendelser.Behandlingsporing.Yrkesaktivitet.Arbeidsledig
import no.nav.helse.hendelser.Behandlingsporing.Yrkesaktivitet.Arbeidsledig.somOrganisasjonsnummer
import no.nav.helse.hendelser.Behandlingsporing.Yrkesaktivitet.Arbeidstaker
import no.nav.helse.hendelser.Behandlingsporing.Yrkesaktivitet.Frilans
import no.nav.helse.hendelser.Behandlingsporing.Yrkesaktivitet.Selvstendig
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.inntekt.InntekterForBeregning
import no.nav.helse.person.inntekt.Inntektskilde
import no.nav.helse.økonomi.Inntekt

data class Vedtaksperiodeberegning(
    val vedtaksperiodeId: UUID,
    val utbetalingstidslinje: Utbetalingstidslinje,
    val inntekterForBeregning: Map<Inntektskilde, Beløpstidslinje>
) {
    val periode = utbetalingstidslinje.periode()
}

internal fun lagUtbetalingstidslinjePerArbeidsgiver(vedtaksperioder: List<Vedtaksperiode>, inntekterForBeregning: InntekterForBeregning): List<Arbeidsgiverberegning> {
    val utbetalingstidslinjer = vedtaksperioder
        .groupBy({ it.yrkesaktivitet }) { vedtaksperiode ->
            val (fastsattÅrsinntekt, inntektjusteringer) = inntekterForBeregning.tilBeregning(vedtaksperiode.yrkesaktivitet.yrkesaktivitetstype)
            val alleInntektjusteringer = inntekterForBeregning.inntektsjusteringer(vedtaksperiode.periode)
                .mapKeys { (yrkesaktivitet, _) -> Inntektskilde(yrkesaktivitet.somOrganisasjonsnummer) }
            lagUtbetalingstidslinjeForVedtaksperiode(vedtaksperiode, fastsattÅrsinntekt, inntektjusteringer, alleInntektjusteringer)
        }
        .map { (yrkesaktivitet, vedtaksperioder) ->
            Arbeidsgiverberegning(
                yrkesaktivitet = yrkesaktivitet.yrkesaktivitetstype,
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

private fun lagUtbetalingstidslinjeForVedtaksperiode(vedtaksperiode: Vedtaksperiode, inntekt: Inntekt, inntektjusteringer: Beløpstidslinje, alleInntektjusteringer: Map<Inntektskilde, Beløpstidslinje>): Vedtaksperiodeberegning {
    val utbetalingstidslinjeBuilder = when (val yrkesaktivitet = vedtaksperiode.yrkesaktivitet.yrkesaktivitetstype) {
        is Arbeidstaker -> vedtaksperiode.behandlinger.utbetalingstidslinjeBuilderForArbeidstaker(inntekt, inntektjusteringer)
        Selvstendig -> vedtaksperiode.behandlinger.utbetalingstidslinjeBuilderForSelvstendig(inntekt)

        Arbeidsledig,
        Frilans -> error("Forventer ikke å lage utbetalingstidslinje for ${yrkesaktivitet::class.simpleName}")
    }

    return Vedtaksperiodeberegning(
        vedtaksperiodeId = vedtaksperiode.id,
        utbetalingstidslinje = utbetalingstidslinjeBuilder.result(vedtaksperiode.sykdomstidslinje),
        inntekterForBeregning = alleInntektjusteringer
    )
}
