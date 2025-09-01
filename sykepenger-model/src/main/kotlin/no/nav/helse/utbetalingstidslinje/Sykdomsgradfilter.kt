package no.nav.helse.utbetalingstidslinje

import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.etterlevelse.UtbetalingstidslinjeBuilder.Companion.subsumsjonsformat
import no.nav.helse.etterlevelse.`§ 8-13 ledd 1`
import no.nav.helse.etterlevelse.`§ 8-13 ledd 2`
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioderMedHensynTilHelg
import no.nav.helse.person.MinimumSykdomsgradsvurdering
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_17
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_4
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Companion.avvisteDager
import no.nav.helse.økonomi.Prosentdel

internal class Sykdomsgradfilter(
    private val minimumSykdomsgradsvurdering: MinimumSykdomsgradsvurdering,
    private val subsumsjonslogg: Subsumsjonslogg,
    private val aktivitetslogg: IAktivitetslogg
) : UtbetalingstidslinjerFilter {

    override fun filter(
        arbeidsgivere: List<Arbeidsgiverberegning>,
        vedtaksperiode: Periode
    ): List<Arbeidsgiverberegning> {
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
        val avvistePerioder = minimumSykdomsgradsvurdering.fjernDagerSomSkalUtbetalesLikevel(tentativtAvvistePerioder)
        if (!avvistePerioder.containsAll(tentativtAvvistePerioder)) {
            aktivitetslogg.varsel(RV_VV_17)
        }

        val avvisteTidslinjer = oppdaterte.avvis(avvistePerioder, Begrunnelse.MinimumSykdomsgrad)

        val tidslinjerForSubsumsjon = arbeidsgivere.map { it.samletVedtaksperiodetidslinje }.subsumsjonsformat()
        subsumsjonslogg.logg(`§ 8-13 ledd 2`(vedtaksperiode, tidslinjerForSubsumsjon, Prosentdel.GRENSE.toDouble(), avvistePerioder))
        val avvisteDager = avvisteDager(avvisteTidslinjer.map { it.samletVedtaksperiodetidslinje }, vedtaksperiode, Begrunnelse.MinimumSykdomsgrad)
        val harAvvisteDager = avvisteDager.isNotEmpty()
        `§ 8-13 ledd 1`(vedtaksperiode, avvisteDager.map { it.dato }.grupperSammenhengendePerioderMedHensynTilHelg(), tidslinjerForSubsumsjon).forEach {
            subsumsjonslogg.logg(it)
        }
        if (harAvvisteDager) aktivitetslogg.varsel(RV_VV_4)
        else aktivitetslogg.info("Ingen avviste dager på grunn av 20 % samlet sykdomsgrad-regel for denne perioden")
        return avvisteTidslinjer
    }
}
