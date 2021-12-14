package no.nav.helse.person

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.person.Vedtaksperiode.Companion.ER_ELLER_HAR_VÆRT_AVSLUTTET
import no.nav.helse.person.Vedtaksperiode.Companion.iderMedUtbetaling
import java.util.*

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

        internal fun Iterable<ForkastetVedtaksperiode>.harAvsluttedePerioder() = this.perioder().any(ER_ELLER_HAR_VÆRT_AVSLUTTET)

        internal fun overlapperMedForkastet(forkastede: Iterable<ForkastetVedtaksperiode>, sykmelding: Sykmelding) {
            Vedtaksperiode.overlapperMedForkastet(forkastede.perioder(), sykmelding)
        }

        internal fun sjekkOmOverlapperMedForkastet(forkastede: Iterable<ForkastetVedtaksperiode>, inntektsmelding: Inntektsmelding) =
            Vedtaksperiode.sjekkOmOverlapperMedForkastet(forkastede.perioder(), inntektsmelding)

        internal fun trimTidligereBehandletDager(aktive: Iterable<Vedtaksperiode>, forkastede: Iterable<ForkastetVedtaksperiode>, inntektsmelding: Inntektsmelding) {
            Vedtaksperiode.trimTidligereBehandletDager(aktive, forkastede.perioder(), inntektsmelding)
        }

        internal fun finnForkastetSykeperiodeRettFør(forkastede: Iterable<ForkastetVedtaksperiode>, other: Vedtaksperiode) =
            forkastede.perioder().firstOrNull { vedtaksperiode -> vedtaksperiode.erSykeperiodeRettFør(other) }

        internal fun finnForrigeAvsluttaPeriode(
            forkastede: Iterable<ForkastetVedtaksperiode>,
            vedtaksperiode: Vedtaksperiode
        ) = Vedtaksperiode.finnForrigeAvsluttaPeriode(forkastede.perioder(), vedtaksperiode)

        internal fun List<ForkastetVedtaksperiode>.iderMedUtbetaling(utbetalingId: UUID) =
            map { it.vedtaksperiode }.iderMedUtbetaling(utbetalingId)

    }
}
