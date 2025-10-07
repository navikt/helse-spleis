package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.økonomi.Inntekt

internal class MaksimumUtbetalingFilter(
    private val sykepengegrunnlagBegrenset6G: Inntekt
) : UtbetalingstidslinjerFilter {
    override fun filter(
        arbeidsgivere: List<Arbeidsgiverberegning>,
        vedtaksperiode: Periode
    ): List<Arbeidsgiverberegning> {
        val betalteTidslinjer = Utbetalingstidslinje
            .betale(sykepengegrunnlagBegrenset6G, arbeidsgivere.map { it.samletTidslinje })
            .zip(arbeidsgivere) { beregnetTidslinje, arbeidsgiver ->
                arbeidsgiver.copy(
                    vedtaksperioder = arbeidsgiver.vedtaksperioder.map { vedtaksperiode ->
                        vedtaksperiode.copy(
                            utbetalingstidslinje = beregnetTidslinje.subset(vedtaksperiode.periode)
                        )
                    }
                )
            }
        return betalteTidslinjer
    }
}
