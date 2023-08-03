package no.nav.helse.person

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.person.PersonObserver.OverstyringIgangsatt.VedtaksperiodeData
import no.nav.helse.person.Revurderingseventyr.RevurderingÅrsak.Arbeidsforhold
import no.nav.helse.person.Revurderingseventyr.RevurderingÅrsak.Arbeidsgiveropplysninger
import no.nav.helse.person.Revurderingseventyr.RevurderingÅrsak.Arbeidsgiverperiode
import no.nav.helse.person.Revurderingseventyr.RevurderingÅrsak.KorrigertInntektsmelding
import no.nav.helse.person.Revurderingseventyr.RevurderingÅrsak.KorrigertSøknad
import no.nav.helse.person.Revurderingseventyr.RevurderingÅrsak.NyPeriode
import no.nav.helse.person.Revurderingseventyr.RevurderingÅrsak.Reberegning
import no.nav.helse.person.Revurderingseventyr.RevurderingÅrsak.SkjønssmessigFastsettelse
import no.nav.helse.person.Revurderingseventyr.RevurderingÅrsak.Sykdomstidslinje
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.Companion.varsel
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_4


class Revurderingseventyr private constructor(
    private val hvorfor: RevurderingÅrsak,
    private val skjæringstidspunkt: LocalDate,
    private val periodeForEndring: Periode
) {
    internal companion object {
        fun nyPeriode(skjæringstidspunkt: LocalDate, periodeForEndring: Periode) =
            Revurderingseventyr(NyPeriode, skjæringstidspunkt, periodeForEndring)

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
        return inngå(hendelse, vedtaksperiode, TypeEndring.REVURDERING, periode)
    }

    internal fun inngåSomEndring(hendelse: IAktivitetslogg, vedtaksperiode: Vedtaksperiode, periode: Periode) =
        inngå(hendelse, vedtaksperiode, TypeEndring.ENDRING, periode)

    private fun inngå(hendelse: IAktivitetslogg, vedtaksperiode: Vedtaksperiode, typeEndring: TypeEndring, periode: Periode) : Boolean {
        if (periodeForEndring.starterEtter(periode)) return false
        hvorfor.dersomInngått(hendelse)
        vedtaksperiode.inngåIRevurderingseventyret(vedtaksperioder, typeEndring.name)
        return true
    }

    internal fun ikkeRelevant(skjæringstidspunkt: LocalDate): Boolean {
        // om endringen gjelder et nyere skjæringstidspunkt så trenger vi ikke bryr oss
        return this.skjæringstidspunkt > skjæringstidspunkt
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

    internal fun kanIgangsetteOverstyringIAvventerInntektsmelding(hendelse: IAktivitetslogg) =
        hvorfor.kanIgangsetteOverstyringIAvventerInntektsmelding().also {
            if (!it) hendelse.info("Igangsetter ikke overstyring i AvventerInntektsmelding grunnet ${hvorfor::class.simpleName}") }

    private enum class TypeEndring {
        ENDRING,
        REVURDERING
    }

    private sealed interface RevurderingÅrsak {

        fun dersomInngått(hendelse: IAktivitetslogg) {}

        fun kanIgangsetteOverstyringIAvventerInntektsmelding() = true

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

        object NyPeriode : RevurderingÅrsak {
            override fun navn() = "NY_PERIODE"

            override fun kanIgangsetteOverstyringIAvventerInntektsmelding() = false
        }
    }

}