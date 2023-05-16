package no.nav.helse.person

import java.util.UUID
import no.nav.helse.person.Vedtaksperiode.Companion.iderMedUtbetaling
import no.nav.helse.person.Vedtaksperiode.Companion.slåSammenForkastedeSykdomstidslinjer
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse

internal class ForkastetVedtaksperiode(
    private val vedtaksperiode: Vedtaksperiode
) {
    internal fun accept(visitor: ArbeidsgiverVisitor) {
        visitor.preVisitForkastetPeriode(vedtaksperiode)
        vedtaksperiode.accept(visitor)
        visitor.postVisitForkastetPeriode(vedtaksperiode)
    }

    fun erVedtaksperiodeRettFør(vedtaksperiode: Vedtaksperiode) =
        this.vedtaksperiode.erVedtaksperiodeRettFør(vedtaksperiode)

    fun forventerInntektHensyntarForkastede() = this.vedtaksperiode.forventerInntektHensyntarForkastede()

    internal companion object {
        private fun Iterable<ForkastetVedtaksperiode>.perioder() = map { it.vedtaksperiode }

        internal fun harNyereForkastetPeriode(forkastede: Iterable<ForkastetVedtaksperiode>, hendelse: SykdomstidslinjeHendelse) =
            Vedtaksperiode.harNyereForkastetPeriode(forkastede.perioder(), hendelse)

        internal fun forlengerForkastet(forkastede: Iterable<ForkastetVedtaksperiode>, hendelse: SykdomstidslinjeHendelse, vedtaksperiode: Vedtaksperiode) =
            Vedtaksperiode.forlengerForkastet(forkastede.perioder(), hendelse, vedtaksperiode)

        internal fun Iterable<ForkastetVedtaksperiode>.slåSammenSykdomstidslinjer() = perioder().slåSammenForkastedeSykdomstidslinjer()

        internal fun harKortGapTilForkastet(forkastede: Iterable<ForkastetVedtaksperiode>, hendelse: SykdomstidslinjeHendelse, vedtaksperiode: Vedtaksperiode) =
            Vedtaksperiode.harKortGapTilForkastet(forkastede.perioder(), hendelse, vedtaksperiode)

        internal fun List<ForkastetVedtaksperiode>.iderMedUtbetaling(utbetalingId: UUID) =
            map { it.vedtaksperiode }.iderMedUtbetaling(utbetalingId)

    }
}
