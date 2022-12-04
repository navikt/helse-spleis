package no.nav.helse.person

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.somPeriode

class Revurderingseventyr private constructor(
    private val hvorfor: RevurderingÅrsak,
    private val skjæringstidspunkt: LocalDate,
    private val periodeForEndring: Periode
) {

    private infix fun Revurderingseventyr.skyldes(årsak: RevurderingÅrsak) = hvorfor == årsak

    internal companion object {
        fun nyPeriode(skjæringstidspunkt: LocalDate, periodeForEndring: Periode) = Revurderingseventyr(RevurderingÅrsak.NY_PERIODE, skjæringstidspunkt, periodeForEndring)
        fun arbeidsforhold(skjæringstidspunkt: LocalDate) = Revurderingseventyr(RevurderingÅrsak.ARBEIDSFORHOLD, skjæringstidspunkt, skjæringstidspunkt.somPeriode())
        fun korrigertSøknad(skjæringstidspunkt: LocalDate, periodeForEndring: Periode) = Revurderingseventyr(RevurderingÅrsak.KORRIGERT_SØKNAD, skjæringstidspunkt, periodeForEndring)
        fun sykdomstidslinje(skjæringstidspunkt: LocalDate, periodeForEndring: Periode) = Revurderingseventyr(RevurderingÅrsak.SYKDOMSTIDSLINJE, skjæringstidspunkt, periodeForEndring)
        fun arbeidsgiveropplysninger(skjæringstidspunkt: LocalDate) = Revurderingseventyr(RevurderingÅrsak.ARBEIDSGIVEROPPLYSNINGER, skjæringstidspunkt, skjæringstidspunkt.somPeriode())
        fun arbeidsgiverperiode(skjæringstidspunkt: LocalDate, periodeForEndring: Periode) = Revurderingseventyr(RevurderingÅrsak.ARBEIDSGIVERPERIODE, skjæringstidspunkt, periodeForEndring)
    }

    private val vedtaksperioder = mutableListOf<PersonObserver.RevurderingIgangsattEvent.VedtaksperiodeData>()

    internal fun inngåIRevurdering(
        vedtaksperiode: Vedtaksperiode,
        skalInngåIRevurdering: () -> Boolean,
        dersomInngått: () -> Unit = {}
    ) {
        if (!skalInngåIRevurdering()) return
        inngå(vedtaksperiode).also {
            dersomInngått()
        }
    }

    private fun inngå(vedtaksperiode: Vedtaksperiode) = vedtaksperiode.inngåIRevurderingseventyret(vedtaksperioder)

    internal fun kanInngåIRevurdering(
        hendelse: IAktivitetslogg,
        overstyrtForventerInntekt: Boolean
    ): Boolean {
        if (this skyldes RevurderingÅrsak.NY_PERIODE) {
            // orker ikke trigger revurdering dersom perioden er innenfor agp
            // TODO: dersom f.eks. Spesialist godkjenner revurderinger uten endringer automatisk så ville ikke det
            // lengre vært problematisk å opprette revurderinger i slike tilfeller
            if (!overstyrtForventerInntekt) return false
            hendelse.varsel(Varselkode.RV_OO_2)
            hendelse.info("Søknaden har trigget en revurdering fordi det er en tidligere eller overlappende periode")
        }
        return true
    }

    internal fun nySenerePeriodePåSammeSkjæringstidspunkt(
        periode: Periode,
        overstyrtPeriode: Periode,
        skjæringstidspunkt: LocalDate,
        overstyrtSkjæringstidspunkt: LocalDate
    ): Boolean {
        val starterEtter = overstyrtPeriode.starterEtter(periode)
        val sammeSkjæringstidspunkt = overstyrtSkjæringstidspunkt == skjæringstidspunkt
        return this skyldes RevurderingÅrsak.NY_PERIODE && starterEtter && sammeSkjæringstidspunkt
    }

    internal fun sendRevurderingIgangsattEvent(
        person: Person,
    ) {
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