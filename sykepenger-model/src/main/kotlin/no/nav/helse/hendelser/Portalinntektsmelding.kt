package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Avsender.ARBEIDSGIVER
import no.nav.helse.hendelser.Periode.Companion.periode
import no.nav.helse.person.Person
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.Vedtaksperiode.Companion.finn
import no.nav.helse.person.Vedtaksperiode.Companion.påvirkerArbeidsgiverperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_26
import no.nav.helse.økonomi.Inntekt
import org.slf4j.LoggerFactory

class Portalinntektsmelding(
    private val meldingsreferanseId: UUID,
    private val refusjon: Inntektsmelding.Refusjon,
    private val orgnummer: String,
    private val førsteFraværsdag: LocalDate?, // TODO: Denne skulle vi jo fjerne, men inntil videre brukes den jo dessverre
    private val inntektsdato: LocalDate?, // TODO: Denne kan fjernes på sikt. brukes bare til logging. Hvorfor er den optional når den angivelig alltid settes på portalinntektsmeldinger?
    private val beregnetInntekt: Inntekt,
    private val arbeidsgiverperioder: List<Periode>,
    private val arbeidsforholdId: String?,
    private val begrunnelseForReduksjonEllerIkkeUtbetalt: String?,
    private val harOpphørAvNaturalytelser: Boolean,
    private val harFlereInntektsmeldinger: Boolean,
    private val avsendersystem: Inntektsmelding.Avsendersystem, // TODO: Fjern meg
    private val vedtaksperiodeId: UUID,
    private val mottatt: LocalDateTime
) : Hendelse {
    private lateinit var vedtaksperiode: Vedtaksperiode
    internal fun initaliser(vedtaksperioder: List<Vedtaksperiode>, person: Person, aktivitetslogg: IAktivitetslogg): Boolean {
        val vedtaksperiode = vedtaksperioder.finn(vedtaksperiodeId)

        if (vedtaksperiode == null && arbeidsgiverperioder.isEmpty()) {
            aktivitetslogg.info("Portalinntektsmelding som treffer forkastet periode uten arbeidsgiverperiode oppgitt. Har ingen datoer å gå på. Antar at vi har periode innenfor 16 dager")
            inntektsmeldingIkkeHåndtert(aktivitetslogg, person, harPeriodeInnenfor16Dager = true)
            return false
        }

        if (vedtaksperiode == null) {
            val arbeidsgiverperioden = checkNotNull(arbeidsgiverperioder.periode())
            inntektsmeldingIkkeHåndtert(aktivitetslogg, person, harPeriodeInnenfor16Dager = vedtaksperioder.påvirkerArbeidsgiverperiode(arbeidsgiverperioden))
            return false
        }

        this.vedtaksperiode = vedtaksperiode
        return true
    }

    internal fun somInntektsmelding(vedtaksperiodeFom: LocalDate, skjæringstidspunkt: LocalDate) = Inntektsmelding(
        meldingsreferanseId = meldingsreferanseId,
        refusjon = refusjon,
        orgnummer = orgnummer,
        førsteFraværsdag = vedtaksperiodeFom,
        inntektsdato = skjæringstidspunkt,
        beregnetInntekt = beregnetInntekt,
        arbeidsgiverperioder = arbeidsgiverperioder,
        arbeidsforholdId = arbeidsforholdId,
        begrunnelseForReduksjonEllerIkkeUtbetalt = begrunnelseForReduksjonEllerIkkeUtbetalt,
        harOpphørAvNaturalytelser = harOpphørAvNaturalytelser,
        harFlereInntektsmeldinger = harFlereInntektsmeldinger,
        avsendersystem = avsendersystem,
        vedtaksperiodeId = vedtaksperiodeId,
        mottatt = mottatt
    )

    internal fun somInntektsmelding(): Inntektsmelding {
        val skjæringstidspunkt = vedtaksperiode.skjæringstidspunkt
        val inntektsmelding = somInntektsmelding(
            vedtaksperiodeFom = vedtaksperiode.periode().start,
            skjæringstidspunkt = skjæringstidspunkt
        )

        if (inntektsdato != null && vedtaksperiode.skjæringstidspunkt != inntektsdato) {
            "Inntekt lagres på en annen dato enn oppgitt i portalinntektsmelding for inntektsmeldingId ${metadata.meldingsreferanseId}. Inntektsmelding oppga inntektsdato $inntektsdato, men inntekten ble lagret på skjæringstidspunkt $skjæringstidspunkt".let {
                logger.info(it)
                sikkerlogg.info(it)
            }
        }

        return inntektsmelding
    }

    internal fun somDagerFraInntektsmelding() = DagerFraInntektsmelding(
        arbeidsgiverperioder = arbeidsgiverperioder,
        førsteFraværsdag = vedtaksperiode.periode().start,
        mottatt = mottatt,
        begrunnelseForReduksjonEllerIkkeUtbetalt = begrunnelseForReduksjonEllerIkkeUtbetalt,
        avsendersystem = avsendersystem,
        harFlereInntektsmeldinger = harFlereInntektsmeldinger,
        harOpphørAvNaturalytelser = harOpphørAvNaturalytelser,
        hendelse = this
    )

    private fun inntektsmeldingIkkeHåndtert(aktivitetslogg: IAktivitetslogg, person: Person, harPeriodeInnenfor16Dager: Boolean) {
        aktivitetslogg.funksjonellFeil(RV_IM_26)
        aktivitetslogg.info("Inntektsmelding ikke håndtert")
        person.emitInntektsmeldingIkkeHåndtert(meldingsreferanseId, orgnummer, harPeriodeInnenfor16Dager)
    }

    override val behandlingsporing = Behandlingsporing.Arbeidsgiver(orgnummer)
    override val metadata = HendelseMetadata(meldingsreferanseId, ARBEIDSGIVER, mottatt, mottatt, false)

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
        private val logger = LoggerFactory.getLogger(Portalinntektsmelding::class.java)
    }
}