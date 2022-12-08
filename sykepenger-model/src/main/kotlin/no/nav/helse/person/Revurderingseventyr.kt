package no.nav.helse.person

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.person.Revurderingseventyr.RevurderingÅrsak.Arbeidsforhold
import no.nav.helse.person.Revurderingseventyr.RevurderingÅrsak.Arbeidsgiveropplysninger
import no.nav.helse.person.Revurderingseventyr.RevurderingÅrsak.Arbeidsgiverperiode
import no.nav.helse.person.Revurderingseventyr.RevurderingÅrsak.KorrigertSøknad
import no.nav.helse.person.Revurderingseventyr.RevurderingÅrsak.NyPeriode
import no.nav.helse.person.Revurderingseventyr.RevurderingÅrsak.Sykdomstidslinje

class Revurderingseventyr private constructor(
    private val hvorfor: RevurderingÅrsak,
    private val skjæringstidspunkt: LocalDate,
    private val periodeForEndring: Periode
) {
    internal companion object {
        fun nyPeriode(skjæringstidspunkt: LocalDate, periodeForEndring: Periode, overstyrtForventerInntekt: Boolean) =
            Revurderingseventyr(NyPeriode(overstyrtForventerInntekt), skjæringstidspunkt, periodeForEndring)

        fun arbeidsforhold(skjæringstidspunkt: LocalDate) = Revurderingseventyr(Arbeidsforhold, skjæringstidspunkt, skjæringstidspunkt.somPeriode())
        fun korrigertSøknad(skjæringstidspunkt: LocalDate, periodeForEndring: Periode) = Revurderingseventyr(KorrigertSøknad, skjæringstidspunkt, periodeForEndring)
        fun sykdomstidslinje(skjæringstidspunkt: LocalDate, periodeForEndring: Periode) = Revurderingseventyr(Sykdomstidslinje, skjæringstidspunkt, periodeForEndring)
        fun arbeidsgiveropplysninger(skjæringstidspunkt: LocalDate) = Revurderingseventyr(Arbeidsgiveropplysninger, skjæringstidspunkt, skjæringstidspunkt.somPeriode())
        fun arbeidsgiverperiode(skjæringstidspunkt: LocalDate, periodeForEndring: Periode) = Revurderingseventyr(Arbeidsgiverperiode, skjæringstidspunkt, periodeForEndring)
    }

    private val vedtaksperioder = mutableListOf<PersonObserver.RevurderingIgangsattEvent.VedtaksperiodeData>()

    internal fun inngåSomRevurdering(hendelse: IAktivitetslogg, vedtaksperiode: Vedtaksperiode): Boolean {
        if (!hvorfor.kanInngå(hendelse)) return false
        hvorfor.dersomInngått(hendelse)
        inngå(vedtaksperiode, TypeEndring.REVURDERING)
        return true
    }

    internal fun inngåSomRevurdering(hendelse: IAktivitetslogg, vedtaksperiode: Vedtaksperiode, periode: Periode): Boolean {
        if (periodeForEndring.starterEtter(periode)) return false
        return inngåSomRevurdering(hendelse, vedtaksperiode)
    }

    internal fun inngåSomOverstyring(hendelse: IAktivitetslogg, vedtaksperiode: Vedtaksperiode): Boolean {
        if (!hvorfor.kanInngå(hendelse)) return false
        hvorfor.dersomInngått(hendelse)
        inngå(vedtaksperiode, TypeEndring.OVERSTYRING)
        return true
    }

    private fun inngå(vedtaksperiode: Vedtaksperiode, typeEndring: TypeEndring) =
        vedtaksperiode.inngåIRevurderingseventyret(vedtaksperioder, typeEndring.name)

    internal fun ikkeRelevant(periode: Periode, skjæringstidspunkt: LocalDate): Boolean {
        // om endringen gjelder et nyere skjæringstidspunkt så trenger vi ikke bryr oss
        if (this.skjæringstidspunkt > skjæringstidspunkt) return true
        return hvorfor.ikkeRelevant(periodeForEndring, periode)
    }

    internal fun sendRevurderingIgangsattEvent(person: Person) {
        if (vedtaksperioder.isEmpty()) return
        person.emitRevurderingIgangsattEvent(
            PersonObserver.RevurderingIgangsattEvent(
                årsak = hvorfor.navn(),
                berørtePerioder = vedtaksperioder.toList(),
                skjæringstidspunkt = skjæringstidspunkt,
                periodeForEndring = periodeForEndring
            )
        )
    }

    private enum class TypeEndring {
        OVERSTYRING,
        REVURDERING
    }

    private sealed interface RevurderingÅrsak {

        fun ikkeRelevant(periodeForEndring: Periode, otherPeriode: Periode): Boolean = false
        fun kanInngå(hendelse: IAktivitetslogg): Boolean = true
        fun dersomInngått(hendelse: IAktivitetslogg) {}

        fun navn(): String

        object Arbeidsgiverperiode : RevurderingÅrsak {
            override fun navn() = "ARBEIDSGIVERPERIODE"
        }

        object Sykdomstidslinje : RevurderingÅrsak {
            override fun navn() = "SYKDOMSTIDSLINJE"
        }

        object Arbeidsgiveropplysninger : RevurderingÅrsak {
            override fun navn() = "ARBEIDSGIVEROPPLYSNINGER"
        }

        object Arbeidsforhold : RevurderingÅrsak {
            override fun navn() = "ARBEIDSFORHOLD"
        }

        object KorrigertSøknad : RevurderingÅrsak {
            override fun navn() = "KORRIGERT_SØKNAD"
        }

        class NyPeriode(private val overstyrtForventerInntekt: Boolean) : RevurderingÅrsak {
            override fun ikkeRelevant(periodeForEndring: Periode, otherPeriode: Periode): Boolean {
                // hvis endringen treffer en nyere nyopprettet periode, da trenger vi ikke bli med
                return periodeForEndring.starterEtter(otherPeriode)
            }

            override fun kanInngå(hendelse: IAktivitetslogg): Boolean {
                // orker ikke trigger revurdering dersom perioden er innenfor agp
                // TODO: dersom f.eks. Spesialist godkjenner revurderinger uten endringer automatisk så ville ikke det
                // lengre vært problematisk å opprette revurderinger i slike tilfeller
                return overstyrtForventerInntekt
            }

            override fun dersomInngått(hendelse: IAktivitetslogg) {
                hendelse.varsel(Varselkode.RV_OO_2)
                hendelse.info("Søknaden har trigget en revurdering fordi det er en tidligere eller overlappende periode")
            }

            override fun navn() = "NY_PERIODE"
        }
    }

}