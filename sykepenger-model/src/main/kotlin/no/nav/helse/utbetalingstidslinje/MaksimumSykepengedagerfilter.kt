package no.nav.helse.utbetalingstidslinje

import kotlin.collections.component1
import kotlin.collections.component2
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_9
import no.nav.helse.utbetalingstidslinje.Maksdatoberegning.Companion.TILSTREKKELIG_OPPHOLD_I_SYKEDAGER

internal class MaksimumSykepengedagerfilter(
    private val maksdatoberegning: Maksdatoberegning,
    private val aktivitetslogg: IAktivitetslogg
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

        val sisteVurdering = vurderinger.last()

        if (sisteVurdering.fremdelesSykEtterTilstrekkeligOpphold(vedtaksperiode, TILSTREKKELIG_OPPHOLD_I_SYKEDAGER)) {
            aktivitetslogg.funksjonellFeil(RV_VV_9)
        }
        if (sisteVurdering.harNåddMaks(vedtaksperiode))
            aktivitetslogg.info("Maks antall sykepengedager er nådd i perioden")
        else
            aktivitetslogg.info("Maksimalt antall sykedager overskrides ikke i perioden")

        return avvisteTidslinjer
    }
}
