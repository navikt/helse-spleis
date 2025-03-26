package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode

data class Arbeidsgiverberegning(
    val orgnummer: String,
    val vedtaksperioder: List<Vedtaksperiodeberegning>,
    val ghostOgØvrig: List<Utbetalingstidslinje>
) {
    private val vedtaksperiodeutbetalingstidslinjer = vedtaksperioder.map { it.utbetalingstidslinje }
    private val samletGhostOgØvrig = ghostOgØvrig.fold(Utbetalingstidslinje(), Utbetalingstidslinje::plus)
    val samletVedtaksperiodetidslinje = vedtaksperiodeutbetalingstidslinjer.fold(Utbetalingstidslinje(), Utbetalingstidslinje::plus)
    val samletTidslinje = vedtaksperiodeutbetalingstidslinjer.fold(samletGhostOgØvrig, Utbetalingstidslinje::plus)

    fun avvis(perioder: List<Periode>, begrunnelse: Begrunnelse) = copy(
        vedtaksperioder = vedtaksperioder.map { vedtaksperiode ->
            vedtaksperiode.copy(
                utbetalingstidslinje = vedtaksperiode.utbetalingstidslinje.avvis(perioder, listOf(begrunnelse))
            )
        }
    )
}

fun List<Arbeidsgiverberegning>.avvis(perioder: List<Periode>, begrunnelse: Begrunnelse): List<Arbeidsgiverberegning> {
    return this
        .map { arbeidsgiver ->
            arbeidsgiver.avvis(perioder, begrunnelse)
        }
}
