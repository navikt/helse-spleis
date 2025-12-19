package no.nav.helse.person

import no.nav.helse.hendelser.til

internal data class VedtaksperioderMedSammeFørsteFraværsdag private constructor(
    val før: List<Vedtaksperiode>,
    val vedtaksperiode: Vedtaksperiode,
    val etter: List<Vedtaksperiode>
): Iterable<Vedtaksperiode> by listOf(før, listOf(vedtaksperiode), etter).flatten() {
    val periode get() = first().periode.start til last().periode.endInclusive

    companion object {
        fun finn(vedtaksperiode: Vedtaksperiode, alleVedtaksperioderPåYrkesaktivitet: List<Vedtaksperiode>): VedtaksperioderMedSammeFørsteFraværsdag {
            check(alleVedtaksperioderPåYrkesaktivitet.all { it.yrkesaktivitet === vedtaksperiode.yrkesaktivitet }) { "Du kan ikke finne første fraværsdag på tvers av yrkesaktiviteter, det heter skjæringstidspunkt!!" }
            val førsteFraværsdag = vedtaksperiode.førsteFraværsdag ?: return VedtaksperioderMedSammeFørsteFraværsdag(emptyList(), vedtaksperiode, emptyList())
            val (før, etter) = alleVedtaksperioderPåYrkesaktivitet.filterNot { it === vedtaksperiode }.filter { it.førsteFraværsdag == førsteFraværsdag }.partition { it.periode.endInclusive < vedtaksperiode.periode.start }
            return VedtaksperioderMedSammeFørsteFraværsdag(før, vedtaksperiode, etter)
        }
    }
}
