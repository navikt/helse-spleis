package no.nav.helse.person

import java.time.LocalDate
import java.util.UUID

class Revurderingseventyr private constructor(private val hvorfor: List<RevurderingÅrsak>) {
    private constructor(hvorfor: RevurderingÅrsak): this(listOf(hvorfor))

    private infix fun Revurderingseventyr.skyldes(årsak: RevurderingÅrsak) = hvorfor.size == 1 && hvorfor.first() == årsak
    private infix fun Revurderingseventyr.ikkeSkyldes(årsak: RevurderingÅrsak) = !hvorfor.contains(årsak)

    internal companion object {
        fun nyPeriode() = Revurderingseventyr(RevurderingÅrsak.NY_PERIODE)
        fun arbeidsforhold() = Revurderingseventyr(RevurderingÅrsak.ARBEIDSFORHOLD)
        fun korrigertSøknad() = Revurderingseventyr(RevurderingÅrsak.KORRIGERT_SØKNAD)
        fun sykdomstidslinje() = Revurderingseventyr(RevurderingÅrsak.SYKDOMSTIDSLINJE)
        fun inntekt() = Revurderingseventyr(RevurderingÅrsak.INNTEKT)
        fun arbeidsgiverperiode() = Revurderingseventyr(RevurderingÅrsak.ARBEIDSGIVERPERIODE)
    }

    private val vedtaksperioder = mutableListOf<UUID>()

    internal fun inngåIRevurdering(vedtaksperiodeId: UUID) = vedtaksperioder.add(vedtaksperiodeId)

    internal fun kanStarteRevurderingIDefaultTilstand(
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

    internal fun kanStarteRevurderingIAvventerInntektsmeldingEllerHistorikk(
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

    internal fun sendRevurderingIgangsattEvent(
        person: Person,
        initertAvVedtaksperiode: UUID,
        skjæringstidspunkt: LocalDate,
        kilde: PersonHendelse,
    ) {
        if (vedtaksperioder.isEmpty()) {
            return kilde.info("Sendte ikke ut et revurdering-igangsatt-event fordi ingen vedtaksperioder inngikk i revurderingen")
        }
        person.sendRevurderingIgangsattEvent(
            PersonObserver.RevurderingIgangsattEvent(
                revurderingsÅrsak = hvorfor.map { it.name },
                berørtePerioder = vedtaksperioder,
                kilde = kilde.meldingsreferanseId(),
                initiertAvVedtaksperiode = initertAvVedtaksperiode,
                skjæringstidspunkt = skjæringstidspunkt
            )
        )
    }

    private enum class RevurderingÅrsak {
        ARBEIDSGIVERPERIODE, INNTEKT, SYKDOMSTIDSLINJE, NY_PERIODE, ARBEIDSFORHOLD, KORRIGERT_SØKNAD
    }

}