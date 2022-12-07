package no.nav.helse.person

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.person.Revurderingseventyr.RevurderingÅrsak.ARBEIDSFORHOLD
import no.nav.helse.person.Revurderingseventyr.RevurderingÅrsak.ARBEIDSGIVEROPPLYSNINGER
import no.nav.helse.person.Revurderingseventyr.RevurderingÅrsak.ARBEIDSGIVERPERIODE
import no.nav.helse.person.Revurderingseventyr.RevurderingÅrsak.KORRIGERT_SØKNAD
import no.nav.helse.person.Revurderingseventyr.RevurderingÅrsak.NY_PERIODE
import no.nav.helse.person.Revurderingseventyr.RevurderingÅrsak.SYKDOMSTIDSLINJE

class Revurderingseventyr private constructor(
    private val hvorfor: RevurderingÅrsak,
    private val skjæringstidspunkt: LocalDate,
    private val periodeForEndring: Periode,
    private val ikkeRelevant: Revurderingseventyr.(otherPeriode: Periode) -> Boolean = { false },
    private val kanInngå: (hendelse: IAktivitetslogg) -> Boolean = { true },
    private val dersomInngått: (hendelse: IAktivitetslogg) -> Unit = { }
) {
    internal companion object {
        fun nyPeriode(skjæringstidspunkt: LocalDate, periodeForEndring: Periode, overstyrtForventerInntekt: Boolean) = Revurderingseventyr(
            NY_PERIODE,
            skjæringstidspunkt,
            periodeForEndring,
            // hvis endringen treffer en nyere nyopprettet periode, da trenger vi ikke bli med
            ikkeRelevant = { otherPeriode -> this.periodeForEndring.starterEtter(otherPeriode) },
            // orker ikke trigger revurdering dersom perioden er innenfor agp
            // TODO: dersom f.eks. Spesialist godkjenner revurderinger uten endringer automatisk så ville ikke det
            // lengre vært problematisk å opprette revurderinger i slike tilfeller
            kanInngå = { overstyrtForventerInntekt },
            dersomInngått = { hendelse ->
                hendelse.varsel(Varselkode.RV_OO_2)
                hendelse.info("Søknaden har trigget en revurdering fordi det er en tidligere eller overlappende periode")
            }
        )

        fun arbeidsforhold(skjæringstidspunkt: LocalDate) = Revurderingseventyr(ARBEIDSFORHOLD, skjæringstidspunkt, skjæringstidspunkt.somPeriode())
        fun korrigertSøknad(skjæringstidspunkt: LocalDate, periodeForEndring: Periode) = Revurderingseventyr(KORRIGERT_SØKNAD, skjæringstidspunkt, periodeForEndring)
        fun sykdomstidslinje(skjæringstidspunkt: LocalDate, periodeForEndring: Periode) = Revurderingseventyr(SYKDOMSTIDSLINJE, skjæringstidspunkt, periodeForEndring)
        fun arbeidsgiveropplysninger(skjæringstidspunkt: LocalDate) = Revurderingseventyr(ARBEIDSGIVEROPPLYSNINGER, skjæringstidspunkt, skjæringstidspunkt.somPeriode())
        fun arbeidsgiverperiode(skjæringstidspunkt: LocalDate, periodeForEndring: Periode) = Revurderingseventyr(ARBEIDSGIVERPERIODE, skjæringstidspunkt, periodeForEndring)
    }

    private val vedtaksperioder = mutableListOf<PersonObserver.RevurderingIgangsattEvent.VedtaksperiodeData>()

    internal fun inngåIRevurdering(hendelse: IAktivitetslogg, vedtaksperiode: Vedtaksperiode): Boolean {
        if (!kanInngå(hendelse)) return false
        dersomInngått(hendelse)
        inngå(vedtaksperiode)
        return true
    }
    internal fun inngåIRevurdering(hendelse: IAktivitetslogg, vedtaksperiode: Vedtaksperiode, periode: Periode): Boolean {
        if (periodeForEndring.starterEtter(periode)) return false
        return inngåIRevurdering(hendelse, vedtaksperiode)
    }

    private fun inngå(vedtaksperiode: Vedtaksperiode) = vedtaksperiode.inngåIRevurderingseventyret(vedtaksperioder)

    internal fun ikkeRelevant(periode: Periode, skjæringstidspunkt: LocalDate): Boolean {
        // om endringen gjelder et nyere skjæringstidspunkt så trenger vi ikke bryr oss
        if (this.skjæringstidspunkt > skjæringstidspunkt) return true
        return ikkeRelevant(this, periode)
    }

    internal fun sendRevurderingIgangsattEvent(person: Person) {
        if (vedtaksperioder.isEmpty()) return
        person.emitRevurderingIgangsattEvent(
            PersonObserver.RevurderingIgangsattEvent(
                årsak = hvorfor.name,
                berørtePerioder = vedtaksperioder.toList(),
                skjæringstidspunkt = skjæringstidspunkt,
                periodeForEndring = periodeForEndring
            )
        )
    }

    private enum class RevurderingÅrsak {
        ARBEIDSGIVERPERIODE, ARBEIDSGIVEROPPLYSNINGER, SYKDOMSTIDSLINJE, NY_PERIODE, ARBEIDSFORHOLD, KORRIGERT_SØKNAD
    }

}