package no.nav.helse.person

import java.time.LocalDate
import no.nav.helse.hendelser.Hendelseinfo
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.SkjønnsmessigFastsettelse
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.inntektsmelding.DagerFraInntektsmelding
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.person.PersonObserver.OverstyringIgangsatt.VedtaksperiodeData
import no.nav.helse.person.Revurderingseventyr.RevurderingÅrsak.Arbeidsforhold
import no.nav.helse.person.Revurderingseventyr.RevurderingÅrsak.Arbeidsgiveropplysninger
import no.nav.helse.person.Revurderingseventyr.RevurderingÅrsak.Arbeidsgiverperiode
import no.nav.helse.person.Revurderingseventyr.RevurderingÅrsak.Grunnbeløpsregulering
import no.nav.helse.person.Revurderingseventyr.RevurderingÅrsak.KorrigertInntektsmeldingArbeidsgiverperiode
import no.nav.helse.person.Revurderingseventyr.RevurderingÅrsak.KorrigertInntektsmeldingInntektsopplysninger
import no.nav.helse.person.Revurderingseventyr.RevurderingÅrsak.KorrigertSøknad
import no.nav.helse.person.Revurderingseventyr.RevurderingÅrsak.NyPeriode
import no.nav.helse.person.Revurderingseventyr.RevurderingÅrsak.Reberegning
import no.nav.helse.person.Revurderingseventyr.RevurderingÅrsak.SkjønssmessigFastsettelse
import no.nav.helse.person.Revurderingseventyr.RevurderingÅrsak.Sykdomstidslinje
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.Companion.varsel
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_4

import no.nav.helse.hendelser.Grunnbeløpsregulering as GrunnbeløpsreguleringHendelse

class Revurderingseventyr private constructor(
    private val hvorfor: RevurderingÅrsak,
    private val skjæringstidspunkt: LocalDate,
    private val periodeForEndring: Periode,
    private val hendelseinfo: Hendelseinfo
) {
    internal companion object {
        fun nyPeriode(søknad: Søknad, skjæringstidspunkt: LocalDate, periodeForEndring: Periode) = Revurderingseventyr(NyPeriode, skjæringstidspunkt, periodeForEndring, søknad)
        fun arbeidsforhold(overstyrArbeidsforhold: OverstyrArbeidsforhold, skjæringstidspunkt: LocalDate) = Revurderingseventyr(Arbeidsforhold, skjæringstidspunkt, skjæringstidspunkt.somPeriode(), overstyrArbeidsforhold)
        fun korrigertSøknad(søknad: Søknad, skjæringstidspunkt: LocalDate, periodeForEndring: Periode) = Revurderingseventyr(KorrigertSøknad, skjæringstidspunkt, periodeForEndring, søknad)
        fun reberegning(påminnelse: Påminnelse, skjæringstidspunkt: LocalDate, periodeForEndring: Periode) = Revurderingseventyr(Reberegning, skjæringstidspunkt, periodeForEndring, påminnelse)
        fun sykdomstidslinje(overstyrTidslinje: OverstyrTidslinje, skjæringstidspunkt: LocalDate, periodeForEndring: Periode) = Revurderingseventyr(Sykdomstidslinje, skjæringstidspunkt, periodeForEndring, overstyrTidslinje)
        fun arbeidsgiveropplysninger(hendelse: OverstyrArbeidsgiveropplysninger, skjæringstidspunkt: LocalDate, endringsdato: LocalDate) = Revurderingseventyr(Arbeidsgiveropplysninger, skjæringstidspunkt, endringsdato.somPeriode(), hendelse)
        fun skjønnsmessigFastsettelse(hendelse: SkjønnsmessigFastsettelse, skjæringstidspunkt: LocalDate, endringsdato: LocalDate) = Revurderingseventyr(SkjønssmessigFastsettelse, skjæringstidspunkt, endringsdato.somPeriode(), hendelse)
        fun arbeidsgiverperiode(hendelse: DagerFraInntektsmelding, skjæringstidspunkt: LocalDate, periodeForEndring: Periode) = Revurderingseventyr(Arbeidsgiverperiode, skjæringstidspunkt, periodeForEndring, hendelse)
        fun infotrygdendring(hendelse: Hendelseinfo, skjæringstidspunkt: LocalDate, periodeForEndring: Periode) = Revurderingseventyr(RevurderingÅrsak.Infotrygdendring, skjæringstidspunkt, periodeForEndring, hendelse)
        fun korrigertInntektsmeldingInntektsopplysninger(inntektsmelding: Inntektsmelding, skjæringstidspunkt: LocalDate, endringsdato: LocalDate) = Revurderingseventyr(KorrigertInntektsmeldingInntektsopplysninger, skjæringstidspunkt, endringsdato.somPeriode(), inntektsmelding)
        fun korrigertInntektsmeldingArbeidsgiverperiode(dager: DagerFraInntektsmelding, skjæringstidspunkt: LocalDate, periodeForEndring: Periode) = Revurderingseventyr(KorrigertInntektsmeldingArbeidsgiverperiode, skjæringstidspunkt, periodeForEndring, dager)
        fun grunnbeløpsregulering(hendelse: GrunnbeløpsreguleringHendelse, skjæringstidspunkt: LocalDate) = Revurderingseventyr(Grunnbeløpsregulering, skjæringstidspunkt, skjæringstidspunkt.somPeriode(), hendelse)

    }

    private val vedtaksperioder = mutableListOf<VedtaksperiodeData>()

    internal fun generasjonkilde() = hendelseinfo?.let { Generasjoner.Generasjonkilde(it) }

    internal fun inngåSomRevurdering(hendelse: IAktivitetslogg, vedtaksperiode: Vedtaksperiode, periode: Periode) =
        inngå(hendelse, vedtaksperiode, TypeEndring.REVURDERING, periode)

    internal fun inngåSomEndring(hendelse: IAktivitetslogg, vedtaksperiode: Vedtaksperiode, periode: Periode) =
        inngå(hendelse, vedtaksperiode, TypeEndring.ENDRING, periode)

    private fun inngå(hendelse: IAktivitetslogg, vedtaksperiode: Vedtaksperiode, typeEndring: TypeEndring, periode: Periode) {
        hvorfor.dersomInngått(hendelse, vedtaksperioder.isEmpty())
        vedtaksperiode.inngåIRevurderingseventyret(vedtaksperioder, typeEndring.name)
    }

    internal fun ikkeRelevant(skjæringstidspunkt: LocalDate, periode: Periode): Boolean {
        // om endringen gjelder et nyere skjæringstidspunkt så trenger vi ikke bryr oss
        return this.skjæringstidspunkt > skjæringstidspunkt || periodeForEndring.starterEtter(periode)
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

    internal fun hvisIkkeArbeidsgiverperiode(block: () -> Unit) {
        if (hvorfor == Arbeidsgiverperiode) return
        block()
    }

    private enum class TypeEndring {
        ENDRING,
        REVURDERING
    }

    private sealed interface RevurderingÅrsak {

        fun dersomInngått(hendelse: IAktivitetslogg, ingenAndrePåmeldt: Boolean) {}

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

        object Infotrygdendring: RevurderingÅrsak {
            override fun navn(): String {
                return "INFOTRYGDENDRING"
            }
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
        object Grunnbeløpsregulering : RevurderingÅrsak {
            override fun navn() = "GRUNNBELØPSREGULERING"
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

        object KorrigertInntektsmeldingInntektsopplysninger : RevurderingÅrsak {

            override fun dersomInngått(hendelse: IAktivitetslogg, ingenAndrePåmeldt: Boolean) {
                if (ingenAndrePåmeldt) hendelse.varsel(RV_IM_4, "Inngår i revurdering på grunn av korrigert inntektsmelding")
                hendelse.info("korrigert inntektsmelding trigget revurdering på grunn av inntektsopplysninger")
            }

            override fun navn() = "KORRIGERT_INNTEKTSMELDING_INNTEKTSOPPLYSNINGER"
        }

        object KorrigertInntektsmeldingArbeidsgiverperiode : RevurderingÅrsak {

            override fun dersomInngått(hendelse: IAktivitetslogg, ingenAndrePåmeldt: Boolean) {
                hendelse.info("korrigert inntektsmelding trigget revurdering på grunn av arbeidsgiverperiode")
            }

            override fun navn() = "KORRIGERT_INNTEKTSMELDING_ARBEIDSGIVERPERIODE"
        }

        object NyPeriode : RevurderingÅrsak {
            override fun navn() = "NY_PERIODE"
        }
    }

}