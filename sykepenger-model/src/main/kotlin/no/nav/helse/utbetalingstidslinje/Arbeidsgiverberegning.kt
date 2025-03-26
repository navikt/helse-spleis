package no.nav.helse.utbetalingstidslinje

data class Arbeidsgiverberegning(
    val orgnummer: String,
    val vedtaksperioder: List<Vedtaksperiodeberegning>,
    val ghostOgAndreInntektskilder: List<Utbetalingstidslinje>
) {
    private val samletVedtaksperiodetidslinje = vedtaksperioder.map { it.utbetalingstidslinje }
    private val samletGhostOgØvrig = ghostOgAndreInntektskilder.fold(Utbetalingstidslinje(), Utbetalingstidslinje::plus)
    val samletTidslinje = samletVedtaksperiodetidslinje.fold(samletGhostOgØvrig, Utbetalingstidslinje::plus)
}
