package no.nav.helse.utbetalingstidslinje

import kotlin.collections.component1
import kotlin.collections.component2
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder

internal class MaksimumSykepengedagerfilter(
    private val maksdatoberegning: Maksdatoberegning
) : UtbetalingstidslinjerFilter {

    override fun filter(
        arbeidsgivere: List<Arbeidsgiverberegning>,
        vedtaksperiode: Periode
    ): List<Arbeidsgiverberegning> {
        val vurderinger = maksdatoberegning.beregn(arbeidsgivere)

        /** går gjennom alle maksdato-sakene og avslår dager. EGENTLIG er det nok å avslå dagene
         *  fra sisteVurdering, men det er noen enhetstester som tester veldig lange
         *  tidslinjer og de forventer at alle maksdatodager avslås, uavhengig av maksdatosak
         */
        val begrunnelser = vurderinger
            .flatMap { maksdatosak ->
                maksdatosak.begrunnelser.map { (begrunnelse, dato) -> dato to begrunnelse }
            }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })

        val avvisteTidslinjer = begrunnelser.entries.fold(arbeidsgivere) { result, (begrunnelse, dager) ->
            result.avvis(dager.grupperSammenhengendePerioder(), begrunnelse)
        }

        return avvisteTidslinjer
    }
}
