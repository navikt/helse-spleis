package no.nav.helse.person

import java.time.LocalDate
import no.nav.helse.hendelser.Periode

class Revurderingseventyr private constructor(private val hvorfor: RevurderingÅrsak) {

    private infix fun Revurderingseventyr.skyldes(årsak: RevurderingÅrsak) = hvorfor == årsak
    private infix fun Revurderingseventyr.ikkeSkyldes(årsak: RevurderingÅrsak) = !skyldes(årsak)

    internal companion object {
        fun nyPeriode() = Revurderingseventyr(RevurderingÅrsak.NY_PERIODE)
        fun arbeidsforhold() = Revurderingseventyr(RevurderingÅrsak.ARBEIDSFORHOLD)
        fun korrigertSøknad() = Revurderingseventyr(RevurderingÅrsak.KORRIGERT_SØKNAD)
        fun sykdomstidslinje() = Revurderingseventyr(RevurderingÅrsak.SYKDOMSTIDSLINJE)
        fun arbeidsgiveropplysninger() = Revurderingseventyr(RevurderingÅrsak.ARBEIDSGIVEROPPLYSNINGER)
        fun arbeidsgiverperiode() = Revurderingseventyr(RevurderingÅrsak.ARBEIDSGIVERPERIODE)
    }

    private val vedtaksperioder = mutableMapOf<String, MutableList<PersonObserver.RevurderingIgangsattEvent.VedtaksperiodeData>>()

    internal fun inngåIRevurdering(
        vedtaksperiode: Vedtaksperiode,
        hendelse: IAktivitetslogg,
        skjæringstidspunkt: LocalDate,
        overstyrtSkjæringstidspunkt: LocalDate,
        overstyrtPeriode: Periode,
        skalInngåIRevurdering: () -> Boolean,
        dersomInngått: () -> Unit = {},
        doAnyway: () -> Unit = {}
    ) {
        // om endringen gjelder et nyere skjæringstidspunkt så trenger vi ikke bryr oss
        if (overstyrtSkjæringstidspunkt > skjæringstidspunkt) return
        // hvis endringen treffer samme skjæringstidspunkt, men en nyere nyopprettet periode, da trenger vi ikke bli med
        if (nySenerePeriodePåSammeSkjæringstidspunkt(vedtaksperiode.periode(), overstyrtPeriode, skjæringstidspunkt, overstyrtSkjæringstidspunkt)) return
        vedtaksperiode.kontekst(hendelse)

        if (!skalInngåIRevurdering()) return doAnyway()
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

    internal fun skalPåvirkeIAvventerInntektsmeldingEllerHistorikk(
        overstyrt: Vedtaksperiode,
        vedtaksperiode: Vedtaksperiode,
        hendelse: IAktivitetslogg,
        harNødvendigOpplysningerFraArbeidsgiver: Boolean
    ): Boolean {
        // perioden er seg selv, og har også bedt om replay av IM. Replay av IM
        // vil muligens sørge for at perioden går videre. Om vi gikk videre herfra ville perioden endt opp
        // med warning om "flere inntektsmeldinger mottatt" siden perioden ville stått i AvventerBlokkerende
        if (overstyrt === vedtaksperiode) return false
        // bare reager på nye perioder, slik at vi ikke reagerer på revurdering igangsatt av en AUU-periode ved
        // mottak av IM
        if (this ikkeSkyldes RevurderingÅrsak.NY_PERIODE) return false
        if (!harNødvendigOpplysningerFraArbeidsgiver) return false
        hendelse.info("Som følge av out of order-periode har vi nødvendige opplysninger fra arbeidsgiver")
        return true
    }

    private fun nySenerePeriodePåSammeSkjæringstidspunkt(
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
        initiertAvArbeidsgiver: String,
        initiertAvVedtaksperiode: PersonObserver.RevurderingIgangsattEvent.VedtaksperiodeData,
        kilde: PersonHendelse,
    ) {
        if (vedtaksperioder.isEmpty()) return
        else {
            person.emitRevurderingIgangsattEvent(
                PersonObserver.RevurderingIgangsattEvent(
                    revurderingsÅrsak = hvorfor.name,
                    kilde = kilde.meldingsreferanseId(),
                    initiertAvArbeidsgiver = initiertAvArbeidsgiver,
                    initiertAvVedtaksperiode = initiertAvVedtaksperiode,
                    berørtePerioder = vedtaksperioder.toMap(),
                )
            )
        }
    }

    private enum class RevurderingÅrsak {
        ARBEIDSGIVERPERIODE, ARBEIDSGIVEROPPLYSNINGER, SYKDOMSTIDSLINJE, NY_PERIODE, ARBEIDSFORHOLD, KORRIGERT_SØKNAD
    }

}