package no.nav.helse.person

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Sykmelding

internal class ForkastetVedtaksperiode(
    private val vedtaksperiode: Vedtaksperiode,
    private val forkastetÅrsak: ForkastetÅrsak
) {
    internal fun accept(visitor: ArbeidsgiverVisitor) {
        visitor.preVisitForkastetPeriode(vedtaksperiode, forkastetÅrsak)
        vedtaksperiode.accept(visitor)
        visitor.postVisitForkastetPeriode(vedtaksperiode, forkastetÅrsak)
    }

    internal companion object {
        private fun Iterable<ForkastetVedtaksperiode>.perioder() = map { it.vedtaksperiode }

        internal fun overlapperMedForkastet(forkastede: Iterable<ForkastetVedtaksperiode>, sykmelding: Sykmelding) {
            Vedtaksperiode.overlapperMedForkastet(forkastede.perioder(), sykmelding)
        }

        internal fun overlapperMedForkastet(forkastede: Iterable<ForkastetVedtaksperiode>, inntektsmelding: Inntektsmelding) {
            Vedtaksperiode.overlapperMedForkastet(forkastede.perioder(), inntektsmelding)
        }

        internal fun finnForkastetSykeperiodeRettFør(forkastede: Iterable<ForkastetVedtaksperiode>, other: Vedtaksperiode) =
            forkastede.perioder().firstOrNull { vedtaksperiode -> vedtaksperiode.erSykeperiodeRettFør(other) }

        internal fun harPeriodeEtter(forkastede: Iterable<ForkastetVedtaksperiode>, other: Vedtaksperiode) =
            forkastede.perioder().any { vedtaksperiode -> vedtaksperiode.starterEtter(other) }

        internal fun finnForrigeAvsluttaPeriode(
            forkastede: Iterable<ForkastetVedtaksperiode>,
            vedtaksperiode: Vedtaksperiode
        ) = Vedtaksperiode.finnForrigeAvsluttaPeriode(forkastede.perioder(), vedtaksperiode)
    }
}
