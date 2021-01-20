package no.nav.helse.person

import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.utbetalingstidslinje.Historie
import java.time.LocalDate

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

        internal fun finnForkastedeTilstøtende(forkastede: Iterable<ForkastetVedtaksperiode>, vedtaksperiode: Vedtaksperiode) =
            forkastede.perioder().firstOrNull { other -> other.erSykeperiodeRettFør(vedtaksperiode) }

        internal fun harPeriodeEtter(forkastede: Iterable<ForkastetVedtaksperiode>, vedtaksperiode: Vedtaksperiode) =
            forkastede.perioder().any { other -> other.starterEtter(vedtaksperiode) }

        internal fun finnForrigeAvsluttaPeriode(
            forkastede: Iterable<ForkastetVedtaksperiode>,
            vedtaksperiode: Vedtaksperiode,
            referanse: LocalDate,
            historie: Historie
        ) = Vedtaksperiode.finnForrigeAvsluttaPeriode(forkastede.perioder(), vedtaksperiode, referanse, historie)
    }
}
