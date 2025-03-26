package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.økonomi.Inntekt

internal class MaksimumUtbetalingFilter(
    private val sykepengegrunnlagBegrenset6G: Inntekt,
    private val er6GBegrenset: Boolean,
    private val aktivitetslogg: IAktivitetslogg
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
        if (er6GBegrenset)
            aktivitetslogg.info("Redusert utbetaling minst én dag på grunn av inntekt over 6G")
        else
            aktivitetslogg.info("Utbetaling har ikke blitt redusert på grunn av 6G")
        return betalteTidslinjer
    }
}
