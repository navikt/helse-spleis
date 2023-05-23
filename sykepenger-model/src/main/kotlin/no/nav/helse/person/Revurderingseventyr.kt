package no.nav.helse.person

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.person.PersonObserver.OverstyringIgangsatt.VedtaksperiodeData
import no.nav.helse.person.Revurderingseventyr.RevurderingÅrsak.*
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.Companion.varsel
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_4


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
        fun reberegning(skjæringstidspunkt: LocalDate, periodeForEndring: Periode) = Revurderingseventyr(Reberegning, skjæringstidspunkt, periodeForEndring)
        fun sykdomstidslinje(skjæringstidspunkt: LocalDate, periodeForEndring: Periode) = Revurderingseventyr(Sykdomstidslinje, skjæringstidspunkt, periodeForEndring)
        fun arbeidsgiveropplysninger(skjæringstidspunkt: LocalDate, endringsdato: LocalDate) = Revurderingseventyr(Arbeidsgiveropplysninger, skjæringstidspunkt, endringsdato.somPeriode())
        fun skjønnsmessigFastsettelse(skjæringstidspunkt: LocalDate, endringsdato: LocalDate) = Revurderingseventyr(SkjønssmessigFastsettelse, skjæringstidspunkt, endringsdato.somPeriode())
        fun arbeidsgiverperiode(skjæringstidspunkt: LocalDate, periodeForEndring: Periode) = Revurderingseventyr(Arbeidsgiverperiode, skjæringstidspunkt, periodeForEndring)
        fun korrigertInntektsmelding(skjæringstidspunkt: LocalDate, endringsdato: LocalDate) = Revurderingseventyr(KorrigertInntektsmelding, skjæringstidspunkt, endringsdato.somPeriode())
    }

    private val vedtaksperioder = mutableListOf<VedtaksperiodeData>()

    internal fun inngåSomRevurdering(hendelse: IAktivitetslogg, vedtaksperiode: Vedtaksperiode, periode: Periode): Boolean {
        if (periodeForEndring.starterEtter(periode)) return false
        return inngåSomRevurdering(hendelse, vedtaksperiode)
    }
    internal fun inngåSomRevurdering(hendelse: IAktivitetslogg, vedtaksperiode: Vedtaksperiode) =
        inngå(hendelse, vedtaksperiode, TypeEndring.REVURDERING)

    internal fun inngåSomEndring(hendelse: IAktivitetslogg, vedtaksperiode: Vedtaksperiode) =
        inngå(hendelse, vedtaksperiode, TypeEndring.ENDRING)

    private fun inngå(hendelse: IAktivitetslogg, vedtaksperiode: Vedtaksperiode, typeEndring: TypeEndring) : Boolean {
        if (!hvorfor.kanInngå(hendelse)) return false
        hvorfor.dersomInngått(hendelse)
        vedtaksperiode.inngåIRevurderingseventyret(vedtaksperioder, typeEndring.name)
        return true
    }

    internal fun ikkeRelevant(periode: Periode, skjæringstidspunkt: LocalDate): Boolean {
        // om endringen gjelder et nyere skjæringstidspunkt så trenger vi ikke bryr oss
        if (this.skjæringstidspunkt > skjæringstidspunkt) return true
        return hvorfor.ikkeRelevant(periodeForEndring, periode)
    }

    internal fun sendOverstyringIgangsattEvent(person: Person) {
        if (vedtaksperioder.isEmpty()) return
        hvorfor.emitOverstyringIgangsattEvent(person, vedtaksperioder.toList(), skjæringstidspunkt, periodeForEndring)
    }

    internal fun loggDersomKorrigerendeSøknad(hendelse: IAktivitetslogg, loggMelding: String) {
        if (hvorfor == KorrigertSøknad){
            hendelse.info(loggMelding)
        }
    }

    private enum class TypeEndring {
        ENDRING,
        REVURDERING
    }

    private sealed interface RevurderingÅrsak {

        fun ikkeRelevant(periodeForEndring: Periode, otherPeriode: Periode): Boolean = false
        fun kanInngå(hendelse: IAktivitetslogg): Boolean = true
        fun dersomInngått(hendelse: IAktivitetslogg) {}

        fun emitOverstyringIgangsattEvent(person: Person, vedtaksperioder: List<VedtaksperiodeData>, skjæringstidspunkt: LocalDate, periodeForEndring: Periode) {
            person.emitOverstyringIgangsattEvent(
                PersonObserver.OverstyringIgangsatt(
                    årsak = navn(),
                    berørtePerioder = vedtaksperioder,
                    skjæringstidspunkt = skjæringstidspunkt,
                    periodeForEndring = periodeForEndring
                )
            )
        }

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

        object SkjønssmessigFastsettelse : RevurderingÅrsak {
            override fun navn() = "SKJØNNSMESSIG_FASTSETTELSE"
        }

        object Arbeidsforhold : RevurderingÅrsak {
            override fun navn() = "ARBEIDSFORHOLD"
        }

        object Reberegning : RevurderingÅrsak {
            override fun emitOverstyringIgangsattEvent(
                person: Person,
                vedtaksperioder: List<VedtaksperiodeData>,
                skjæringstidspunkt: LocalDate,
                periodeForEndring: Periode
            ) { /* trenger ikke fortelle om en reberegning */ }

            override fun navn() = "REBEREGNING"
        }

        object KorrigertSøknad : RevurderingÅrsak {
            override fun navn() = "KORRIGERT_SØKNAD"
        }

        object KorrigertInntektsmelding : RevurderingÅrsak {

            override fun dersomInngått(hendelse: IAktivitetslogg) {
                hendelse.varsel(RV_IM_4, "Inngår i revurdering på grunn av korrigert inntektsmelding")
                hendelse.info("korrigert inntektsmelding trigget revurdering")
            }

            override fun navn() = "KORRIGERT_INNTEKTSMELDING"
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