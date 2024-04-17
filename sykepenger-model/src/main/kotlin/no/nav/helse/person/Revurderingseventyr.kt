package no.nav.helse.person

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.person.PersonObserver.OverstyringIgangsatt.VedtaksperiodeData
import no.nav.helse.person.Revurderingseventyr.RevurderingÅrsak.Annullering
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
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_RV_7


class Revurderingseventyr private constructor(
    private val hvorfor: RevurderingÅrsak,
    private val skjæringstidspunkt: LocalDate,
    private val periodeForEndring: Periode,
    private val hendelse: Hendelse
) : Hendelse by hendelse {

    internal companion object {
        fun nyPeriode(hendelse: Hendelse, skjæringstidspunkt: LocalDate, periodeForEndring: Periode) = Revurderingseventyr(NyPeriode, skjæringstidspunkt, periodeForEndring, hendelse)
        fun arbeidsforhold(hendelse: Hendelse, skjæringstidspunkt: LocalDate) = Revurderingseventyr(Arbeidsforhold, skjæringstidspunkt, skjæringstidspunkt.somPeriode(), hendelse)
        fun korrigertSøknad(hendelse: Hendelse, skjæringstidspunkt: LocalDate, periodeForEndring: Periode) = Revurderingseventyr(KorrigertSøknad, skjæringstidspunkt, periodeForEndring, hendelse)
        fun reberegning(hendelse: Hendelse, skjæringstidspunkt: LocalDate, periodeForEndring: Periode) = Revurderingseventyr(Reberegning, skjæringstidspunkt, periodeForEndring, hendelse)
        fun sykdomstidslinje(hendelse: Hendelse, skjæringstidspunkt: LocalDate, periodeForEndring: Periode) = Revurderingseventyr(Sykdomstidslinje, skjæringstidspunkt, periodeForEndring, hendelse)
        fun arbeidsgiveropplysninger(hendelse: Hendelse, skjæringstidspunkt: LocalDate, endringsdato: LocalDate) = Revurderingseventyr(Arbeidsgiveropplysninger, skjæringstidspunkt, endringsdato.somPeriode(), hendelse)
        fun skjønnsmessigFastsettelse(hendelse: Hendelse, skjæringstidspunkt: LocalDate, endringsdato: LocalDate) = Revurderingseventyr(SkjønssmessigFastsettelse, skjæringstidspunkt, endringsdato.somPeriode(), hendelse)
        fun arbeidsgiverperiode(hendelse: Hendelse, skjæringstidspunkt: LocalDate, periodeForEndring: Periode) = Revurderingseventyr(Arbeidsgiverperiode, skjæringstidspunkt, periodeForEndring, hendelse)
        fun infotrygdendring(hendelse: Hendelse, skjæringstidspunkt: LocalDate, periodeForEndring: Periode) = Revurderingseventyr(RevurderingÅrsak.Infotrygdendring, skjæringstidspunkt, periodeForEndring, hendelse)
        fun korrigertInntektsmeldingInntektsopplysninger(hendelse: Hendelse, skjæringstidspunkt: LocalDate, endringsdato: LocalDate) = Revurderingseventyr(KorrigertInntektsmeldingInntektsopplysninger, skjæringstidspunkt, endringsdato.somPeriode(), hendelse)
        fun korrigertInntektsmeldingArbeidsgiverperiode(hendelse: Hendelse, skjæringstidspunkt: LocalDate, periodeForEndring: Periode) = Revurderingseventyr(KorrigertInntektsmeldingArbeidsgiverperiode, skjæringstidspunkt, periodeForEndring, hendelse)
        fun grunnbeløpsregulering(hendelse: Hendelse, skjæringstidspunkt: LocalDate) = Revurderingseventyr(Grunnbeløpsregulering, skjæringstidspunkt, skjæringstidspunkt.somPeriode(), hendelse)
        fun annullering(hendelse: Hendelse, periode: Periode) = Revurderingseventyr(Annullering, periode.start, periode, hendelse)
    }

    private val vedtaksperioder = mutableListOf<VedtaksperiodeData>()

    internal fun inngåSomRevurdering(vedtaksperiode: Vedtaksperiode, periode: Periode) =
        inngå(vedtaksperiode, TypeEndring.REVURDERING, periode)

    internal fun inngåSomEndring(vedtaksperiode: Vedtaksperiode, periode: Periode) =
        inngå(vedtaksperiode, TypeEndring.ENDRING, periode)

    private fun inngå(vedtaksperiode: Vedtaksperiode, typeEndring: TypeEndring, periode: Periode) {
        hvorfor.dersomInngått(this, vedtaksperioder.isEmpty())
        vedtaksperiode.inngåIRevurderingseventyret(vedtaksperioder, typeEndring.name)
    }

    internal fun ikkeRelevant(periode: Periode): Boolean {
        return periodeForEndring.starterEtter(periode)
    }

    internal fun sendOverstyringIgangsattEvent(person: Person) {
        if (vedtaksperioder.isEmpty()) return
        hvorfor.emitOverstyringIgangsattEvent(person, vedtaksperioder.toList(), skjæringstidspunkt, periodeForEndring, hendelse.meldingsreferanseId())
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

        fun emitOverstyringIgangsattEvent(person: Person, vedtaksperioder: List<VedtaksperiodeData>, skjæringstidspunkt: LocalDate, periodeForEndring: Periode, meldingsreferanseId: UUID) {
            person.emitOverstyringIgangsattEvent(
                PersonObserver.OverstyringIgangsatt(
                    årsak = navn(),
                    berørtePerioder = vedtaksperioder,
                    skjæringstidspunkt = skjæringstidspunkt,
                    periodeForEndring = periodeForEndring,
                    meldingsreferanseId = meldingsreferanseId
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
        object Annullering : RevurderingÅrsak {
            override fun navn() = "ANNULLERING"
            override fun dersomInngått(hendelse: IAktivitetslogg, ingenAndrePåmeldt: Boolean) {
                hendelse.varsel(RV_RV_7)
            }
        }

        object Reberegning : RevurderingÅrsak {
            override fun emitOverstyringIgangsattEvent(
                person: Person,
                vedtaksperioder: List<VedtaksperiodeData>,
                skjæringstidspunkt: LocalDate,
                periodeForEndring: Periode,
                meldingsreferanseId: UUID
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