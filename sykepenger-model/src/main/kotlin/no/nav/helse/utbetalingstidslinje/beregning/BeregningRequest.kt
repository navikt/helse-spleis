package no.nav.helse.utbetalingstidslinje.beregning

import java.util.*
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.økonomi.Inntekt

data class BeregningRequest(
    val perioderSomMåHensyntasVedBeregning: List<VedtaksperiodeForBeregning>,
    val fastsatteÅrsinntekter: Map<Yrkesaktivitet.Arbeidstaker, Inntekt>,
    val selvstendigNæringsdrivende: Inntekt?,
    val inntektjusteringer: Map<Yrkesaktivitet, Beløpstidslinje>,
) {
    val alleInntektskilder = setOfNotNull(selvstendigNæringsdrivende?.let { Yrkesaktivitet.Selvstendig }) +
        fastsatteÅrsinntekter.keys +
        inntektjusteringer.keys +
        perioderSomMåHensyntasVedBeregning.map { it.dataForBeregning.yrkesaktivitet }

    val beregningsperiode = perioderSomMåHensyntasVedBeregning
        .map { it.periode }
        .reduce(Periode::plus)

    data class VedtaksperiodeForBeregning(
        val vedtaksperiodeId: UUID,
        val sykdomstidslinje: Sykdomstidslinje,
        val dataForBeregning: DataForBeregning
    ) {
        val periode = checkNotNull(sykdomstidslinje.periode()) { "sykdomstidslinjen er tom" }

        sealed interface DataForBeregning {
            val yrkesaktivitet get() = when (this) {
                is Arbeidstaker -> Yrkesaktivitet.Arbeidstaker(organisasjonsnummer)
                is Selvstendig -> Yrkesaktivitet.Selvstendig
            }

            data class Arbeidstaker(
                val organisasjonsnummer: String,
                val arbeidsgiverperiode: List<Periode>,
                val dagerNavOvertarAnsvar: List<Periode>,
                val refusjonstidslinje: Beløpstidslinje
            ) : DataForBeregning
            data class Selvstendig(val ventetid: Periode): DataForBeregning
        }
    }
}
