package no.nav.helse.person

import no.nav.helse.memento.ForkastetVedtaksperiodeMemento
import no.nav.helse.person.Vedtaksperiode.Companion.slåSammenForkastedeSykdomstidslinjer
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode

internal class ForkastetVedtaksperiode(
    private val vedtaksperiode: Vedtaksperiode
) {
    internal fun accept(visitor: ArbeidsgiverVisitor) {
        visitor.preVisitForkastetPeriode(vedtaksperiode)
        vedtaksperiode.accept(visitor)
        visitor.postVisitForkastetPeriode(vedtaksperiode)
    }

    internal companion object {
        private fun Iterable<ForkastetVedtaksperiode>.perioder() = map { it.vedtaksperiode }

        internal fun harNyereForkastetPeriode(forkastede: Iterable<ForkastetVedtaksperiode>, vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) =
            Vedtaksperiode.harNyereForkastetPeriode(forkastede.perioder(), vedtaksperiode, hendelse)

        internal fun harOverlappendeForkastetPeriode(forkastede: Iterable<ForkastetVedtaksperiode>, vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) =
            Vedtaksperiode.harOverlappendeForkastetPeriode(forkastede.perioder(), vedtaksperiode, hendelse)

        internal fun forlengerForkastet(forkastede: Iterable<ForkastetVedtaksperiode>, hendelse: IAktivitetslogg, vedtaksperiode: Vedtaksperiode) =
            Vedtaksperiode.forlengerForkastet(forkastede.perioder(), hendelse, vedtaksperiode)

        internal fun Iterable<ForkastetVedtaksperiode>.slåSammenSykdomstidslinjer(sykdomstidslinje: Sykdomstidslinje) = perioder().slåSammenForkastedeSykdomstidslinjer(sykdomstidslinje)

        internal fun harKortGapTilForkastet(forkastede: Iterable<ForkastetVedtaksperiode>, hendelse: IAktivitetslogg, vedtaksperiode: Vedtaksperiode) =
            Vedtaksperiode.harKortGapTilForkastet(forkastede.perioder(), hendelse, vedtaksperiode)

        internal fun hørerTilArbeidsgiverperiode(
            forkastede: List<ForkastetVedtaksperiode>,
            vedtaksperioder: List<Vedtaksperiode>,
            arbeidsgiverperiode: Arbeidsgiverperiode
        ): List<Vedtaksperiode> =
            (forkastede.map { it.vedtaksperiode } + vedtaksperioder)
                .sorted()
                .filter { arbeidsgiverperiode.hørerTil(it.periode()) }

    }

    internal fun memento() = ForkastetVedtaksperiodeMemento(vedtaksperiode.memento())
}
