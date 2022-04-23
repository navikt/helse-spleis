package no.nav.helse.person

import java.util.UUID
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Vedtaksperiode.Companion.ER_ELLER_HAR_VÆRT_AVSLUTTET
import no.nav.helse.person.Vedtaksperiode.Companion.iderMedUtbetaling
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode

internal class ForkastetVedtaksperiode(
    private val vedtaksperiode: Vedtaksperiode,
    private val forkastetÅrsak: ForkastetÅrsak
) {
    internal fun accept(visitor: ArbeidsgiverVisitor) {
        visitor.preVisitForkastetPeriode(vedtaksperiode, forkastetÅrsak)
        vedtaksperiode.accept(visitor)
        visitor.postVisitForkastetPeriode(vedtaksperiode, forkastetÅrsak)
    }

    private fun håndterInnteksmeldingReplay(
        person: Person,
        inntektsmelding: Inntektsmelding,
    ): Boolean {
        if (inntektsmelding.erRelevant(vedtaksperiode.periode())) {
            person.sendOppgaveEvent(inntektsmelding)
            return true
        }
        return false
    }

    internal companion object {
        private fun Iterable<ForkastetVedtaksperiode>.perioder() = map { it.vedtaksperiode }

        internal fun Iterable<ForkastetVedtaksperiode>.harAvsluttedePerioder() = this.perioder().any(ER_ELLER_HAR_VÆRT_AVSLUTTET)

        internal fun Iterable<ForkastetVedtaksperiode>.håndterInntektsmeldingReplay(
            person: Person,
            inntektsmelding: Inntektsmelding,
            vedtaksperiodeId: UUID
        ) = finnForkastetVedtaksperiode(vedtaksperiodeId)?.håndterInnteksmeldingReplay(person, inntektsmelding) ?: false

        private fun Iterable<ForkastetVedtaksperiode>.finnForkastetVedtaksperiode(vedtaksperiodeId: UUID): ForkastetVedtaksperiode? =
            firstOrNull { it.vedtaksperiode.harId(vedtaksperiodeId) }

        internal fun overlapperMedForkastet(forkastede: Iterable<ForkastetVedtaksperiode>, hendelse: SykdomstidslinjeHendelse) {
            Vedtaksperiode.overlapperMedForkastet(forkastede.perioder(), hendelse)
        }
        internal fun forlengerForkastet(forkastede: Iterable<ForkastetVedtaksperiode>, hendelse: SykdomstidslinjeHendelse) {
            Vedtaksperiode.forlengerForkastet(forkastede.perioder(), hendelse)
        }

        internal fun arbeidsgiverperiodeFor(
            person: Person,
            sykdomshistorikkId: UUID,
            forkastede: List<ForkastetVedtaksperiode>,
            organisasjonsnummer: String,
            sykdomstidslinje: Sykdomstidslinje,
            periode: Periode,
            subsumsjonObserver: SubsumsjonObserver
        ): List<Arbeidsgiverperiode> = Vedtaksperiode.arbeidsgiverperiodeFor(person, sykdomshistorikkId, forkastede.perioder(), organisasjonsnummer, sykdomstidslinje, periode, subsumsjonObserver)

        internal fun sjekkOmOverlapperMedForkastet(forkastede: Iterable<ForkastetVedtaksperiode>, inntektsmelding: Inntektsmelding) =
            Vedtaksperiode.sjekkOmOverlapperMedForkastet(forkastede.perioder(), inntektsmelding)

        internal fun finnForkastetSykeperiodeRettFør(forkastede: Iterable<ForkastetVedtaksperiode>, other: Vedtaksperiode) =
            forkastede.perioder().firstOrNull { vedtaksperiode -> vedtaksperiode.erVedtaksperiodeRettFør(other) }

        internal fun List<ForkastetVedtaksperiode>.iderMedUtbetaling(utbetalingId: UUID) =
            map { it.vedtaksperiode }.iderMedUtbetaling(utbetalingId)

    }
}
