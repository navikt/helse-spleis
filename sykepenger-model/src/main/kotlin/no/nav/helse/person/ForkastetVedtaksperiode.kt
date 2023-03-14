package no.nav.helse.person

import java.util.UUID
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.person.Vedtaksperiode.Companion.iderMedUtbetaling
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
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

        internal fun harNyereForkastetPeriode(forkastede: Iterable<ForkastetVedtaksperiode>, hendelse: SykdomstidslinjeHendelse) =
            Vedtaksperiode.harNyereForkastetPeriode(forkastede.perioder(), hendelse)

        internal fun forlengerForkastet(forkastede: Iterable<ForkastetVedtaksperiode>, hendelse: SykdomstidslinjeHendelse) =
            Vedtaksperiode.forlengerForkastet(forkastede.perioder(), hendelse)

        internal fun harKortGapTilForkastet(forkastede: Iterable<ForkastetVedtaksperiode>, hendelse: SykdomstidslinjeHendelse) =
            Vedtaksperiode.harKortGapTilForkastet(forkastede.perioder(), hendelse)

        internal fun arbeidsgiverperiodeFor(
            person: Person,
            forkastede: List<ForkastetVedtaksperiode>,
            organisasjonsnummer: String,
            sykdomstidslinje: Sykdomstidslinje,
            subsumsjonObserver: SubsumsjonObserver?
        ): List<Arbeidsgiverperiode> = Vedtaksperiode.arbeidsgiverperiodeFor(
            person,
            forkastede.perioder(),
            organisasjonsnummer,
            sykdomstidslinje,
            subsumsjonObserver
        )

        internal fun sjekkOmOverlapperMedForkastet(forkastede: Iterable<ForkastetVedtaksperiode>, inntektsmelding: Inntektsmelding) =
            Vedtaksperiode.sjekkOmOverlapperMedForkastet(forkastede.perioder(), inntektsmelding)

        internal fun List<ForkastetVedtaksperiode>.iderMedUtbetaling(utbetalingId: UUID) =
            map { it.vedtaksperiode }.iderMedUtbetaling(utbetalingId)

    }
}
