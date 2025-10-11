package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.utenPerioder

internal class Sykdomsgradfilter(
    private val perioderMedMinimumSykdomsgradVurdertOK: Set<Periode>
) : UtbetalingstidslinjerFilter {

    override fun filter(arbeidsgivere: List<Arbeidsgiverberegning>): List<Arbeidsgiverberegning> {
        val oppdaterte = Utbetalingstidslinje.totalSykdomsgrad(arbeidsgivere.map { it.samletTidslinje })
            .zip(arbeidsgivere) { beregnetTidslinje, arbeidsgiver ->
                arbeidsgiver.copy(
                    vedtaksperioder = arbeidsgiver.vedtaksperioder.map { vedtaksperiodeberegning ->
                        vedtaksperiodeberegning.copy(
                            utbetalingstidslinje = beregnetTidslinje.subset(vedtaksperiodeberegning.periode)
                        )
                    },
                    ghostOgAndreInntektskilder = arbeidsgiver.ghostOgAndreInntektskilder.map {
                        beregnetTidslinje.subset(it.periode())
                    }
                )
            }

        val tentativtAvvistePerioder = Utbetalingsdag.dagerUnderGrensen(oppdaterte.map { it.samletVedtaksperiodetidslinje })
        val avvistePerioder = tentativtAvvistePerioder.utenPerioder(perioderMedMinimumSykdomsgradVurdertOK)

        val avvisteTidslinjer = oppdaterte.avvis(avvistePerioder, Begrunnelse.MinimumSykdomsgrad)

        return avvisteTidslinjer
    }
}
