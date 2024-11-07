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
    private val inntektsdato: LocalDate?, // TODO: Denne kan fjernes på sikt. brukes bare til logging
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

    internal fun somInntektsmelding(vedtaksperioder: List<Vedtaksperiode>, person: Person, aktivitetslogg: IAktivitetslogg): Inntektsmelding? {
        val vedtaksperioden = vedtaksperioder.finn(vedtaksperiodeId)

        if (vedtaksperioden == null && arbeidsgiverperioder.isEmpty()) {
            aktivitetslogg.info("Portalinntektsmelding som treffer forkastet periode uten arbeidsgiverperiode oppgitt. Har ingen datoer å gå på. Antar at vi har periode innenfor 16 dager")
            inntektsmeldingIkkeHåndtert(aktivitetslogg, person, harPeriodeInnenfor16Dager = true)
            return null
        }

        if (vedtaksperioden == null) {
            val arbeidsgiverperioden = checkNotNull(arbeidsgiverperioder.periode())
            inntektsmeldingIkkeHåndtert(aktivitetslogg, person, harPeriodeInnenfor16Dager = vedtaksperioder.påvirkerArbeidsgiverperiode(arbeidsgiverperioden))
            return null
        }

        val inntektsmelding = Inntektsmelding(
            meldingsreferanseId = meldingsreferanseId,
            refusjon = refusjon,
            orgnummer = orgnummer,
            førsteFraværsdag = vedtaksperioden.periode().start,
            inntektsdato = vedtaksperioden.skjæringstidspunkt,
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

        if (vedtaksperioden.skjæringstidspunkt != inntektsdato) {
            "Inntekt lagres på en annen dato enn oppgitt i portalinntektsmelding for inntektsmeldingId ${metadata.meldingsreferanseId}. Inntektsmelding oppga inntektsdato $inntektsdato, men inntekten ble lagret på skjæringstidspunkt ${vedtaksperioden.skjæringstidspunkt}".let {
                logger.info(it)
                sikkerlogg.info(it)
            }
        }

        return inntektsmelding
    }

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