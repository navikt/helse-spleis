package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode

data class Arbeidsgiverberegning(
    val yrkesaktivitet: Yrkesaktivitet,
    val vedtaksperioder: List<Vedtaksperiodeberegning>,
    val ghostOgAndreInntektskilder: List<Utbetalingstidslinje>
) {
    private val vedtaksperiodeutbetalingstidslinjer = vedtaksperioder.map { it.utbetalingstidslinje }
    private val samletGhostOgAndreInntektskilder = ghostOgAndreInntektskilder.fold(Utbetalingstidslinje(), Utbetalingstidslinje::plus)
    val samletVedtaksperiodetidslinje = vedtaksperiodeutbetalingstidslinjer.fold(Utbetalingstidslinje(), Utbetalingstidslinje::plus)
    val samletTidslinje = vedtaksperiodeutbetalingstidslinjer.fold(samletGhostOgAndreInntektskilder, Utbetalingstidslinje::plus)

    internal fun avvis(perioder: List<Periode>, begrunnelse: Begrunnelse) = copy(
        vedtaksperioder = vedtaksperioder.map { vedtaksperiode ->
            vedtaksperiode.copy(
                utbetalingstidslinje = vedtaksperiode.utbetalingstidslinje.avvis(perioder, begrunnelse)
            )
        }
    )

    sealed interface Yrkesaktivitet {
        data class Arbeidstaker(val organisasjonsnummer: String) : Yrkesaktivitet
        data object Selvstendig : Yrkesaktivitet
        data object Frilans : Yrkesaktivitet
        data object Arbeidsledig : Yrkesaktivitet
    }
}

internal fun List<Arbeidsgiverberegning>.avvis(perioder: List<Periode>, begrunnelse: Begrunnelse): List<Arbeidsgiverberegning> {
    return this
        .map { arbeidsgiver ->
            arbeidsgiver.avvis(perioder, begrunnelse)
        }
}
