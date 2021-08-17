package no.nav.helse.person

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.person.Vedtaksperiode.Companion.iderMedUtbetaling
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
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

        internal fun overlapperMedForkastet(forkastede: Iterable<ForkastetVedtaksperiode>, hendelse: SykdomstidslinjeHendelse) {
            Vedtaksperiode.overlapperMedForkastet(forkastede.perioder(), hendelse)
        }

        internal fun overlapperMedForkastet(aktive: Iterable<Vedtaksperiode>, forkastede: Iterable<ForkastetVedtaksperiode>, inntektsmelding: Inntektsmelding) {
            Vedtaksperiode.overlapperMedForkastet(aktive, forkastede.perioder(), inntektsmelding)
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
