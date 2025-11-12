package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Avsender.ARBEIDSGIVER
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.nesteDag
import no.nav.helse.person.EventBus
import no.nav.helse.person.Person
import no.nav.helse.person.Sykmeldingsperioder
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.inntekt.ArbeidstakerFaktaavklartInntekt
import no.nav.helse.person.inntekt.Arbeidstakerinntektskilde
import no.nav.helse.person.inntekt.Inntektsdata
import no.nav.helse.økonomi.Inntekt

class InntektFraInntektsmelding(
    override val behandlingsporing: Behandlingsporing.Yrkesaktivitet.Arbeidstaker,
    meldingsreferanseId: MeldingsreferanseId,
    mottatt: LocalDateTime,
    arbeidsgiverperioder: List<Periode>,
    førsteFraværsdag: LocalDate?,
    beregnetInntekt: Inntekt,
) : Hendelse {

    override val metadata = HendelseMetadata(
        meldingsreferanseId = meldingsreferanseId,
        avsender = ARBEIDSGIVER,
        innsendt = mottatt,
        registrert = mottatt,
        automatiskBehandling = false
    )

    internal val faktaavklartInntekt = faktaavklartInntekt(
        meldingsreferanseId = meldingsreferanseId,
        mottatt = mottatt,
        arbeidsgiverperioder = arbeidsgiverperioder,
        førsteFraværsdag = førsteFraværsdag,
        beregnetInntekt = beregnetInntekt
    )

    internal fun håndtert(eventBus: EventBus, vedtaksperiodeId: UUID) {
        eventBus.emitInntektsmeldingHåndtert(
            meldingsreferanseId =  metadata.meldingsreferanseId.id,
            vedtaksperiodeId = vedtaksperiodeId,
            organisasjonsnummer = behandlingsporing.organisasjonsnummer
        )
    }

    internal fun ikkeHåndtert(eventBus: EventBus, aktivitetslogg: IAktivitetslogg, person: Person, forkastede: List<Periode>, sykmeldingsperioder: Sykmeldingsperioder) {
        val relevanteSykmeldingsperioder = relevanteSykmeldingsperioder(sykmeldingsperioder.perioder())
        val overlapperMedForkastet = overlapperMed(forkastede)
        if (relevanteSykmeldingsperioder.isNotEmpty() && !overlapperMedForkastet) {
            eventBus.emitInntektsmeldingFørSøknadEvent(
                meldingsreferanseId = metadata.meldingsreferanseId.id,
                yrkesaktivitetssporing = behandlingsporing
            )
            return aktivitetslogg.info("Inntektsmelding før søknad - er relevant for sykmeldingsperioder $relevanteSykmeldingsperioder")
        }

        aktivitetslogg.info("Inntektsmelding ikke håndtert")
        eventBus.emitInntektsmeldingIkkeHåndtert(
            meldingsreferanseId = metadata.meldingsreferanseId,
            organisasjonsnummer = behandlingsporing.organisasjonsnummer,
            speilrelatert = speilrelatert(person)
        )
    }

    private val perioderViTrorInntektsmeldingenPrøverÅSiNoeOm = listOfNotNull(førsteFraværsdag?.somPeriode()).plus(arbeidsgiverperioder).grupperSammenhengendePerioder()

    private fun relevanteSykmeldingsperioder(sykmeldingsperioder: List<Periode>) = sykmeldingsperioder.filter { sykmeldingsperiode ->
        perioderViTrorInntektsmeldingenPrøverÅSiNoeOm.any { periodeViTrorInntektsmeldingenPrøverÅSiNoeOm ->
            (Periode.mellom(sykmeldingsperiode, periodeViTrorInntektsmeldingenPrøverÅSiNoeOm)?.count() ?: 0) < 18L
        }
    }

    private fun overlapperMed(forkastedePerioder: List<Periode>) = forkastedePerioder.any { forkastetPeriode ->
        perioderViTrorInntektsmeldingenPrøverÅSiNoeOm.any { periodeViTrorInntektsmeldingenPrøverÅSiNoeOm ->
            forkastetPeriode.overlapperMed(periodeViTrorInntektsmeldingenPrøverÅSiNoeOm)
        }
    }

    private fun speilrelatert(person: Person) = person.speilrelatert(*perioderViTrorInntektsmeldingenPrøverÅSiNoeOm.toTypedArray())

    internal companion object {
        fun faktaavklartInntekt(
            meldingsreferanseId: MeldingsreferanseId,
            mottatt: LocalDateTime,
            arbeidsgiverperioder: List<Periode>,
            førsteFraværsdag: LocalDate?,
            beregnetInntekt: Inntekt
        ): ArbeidstakerFaktaavklartInntekt {
            val grupperteArbeidsgiverperioder = arbeidsgiverperioder.grupperSammenhengendePerioder()

            return ArbeidstakerFaktaavklartInntekt(
                id = UUID.randomUUID(),
                inntektsdata = Inntektsdata(
                    hendelseId = meldingsreferanseId,
                    dato = when (førsteFraværsdag != null && (grupperteArbeidsgiverperioder.isEmpty() || førsteFraværsdag > grupperteArbeidsgiverperioder.last().endInclusive.nesteDag)) {
                        true -> førsteFraværsdag
                        false -> grupperteArbeidsgiverperioder.maxOf { it.start }
                    },
                    beløp = beregnetInntekt,
                    tidsstempel = mottatt
                ),
                inntektsopplysningskilde = Arbeidstakerinntektskilde.Arbeidsgiver
            )
        }
    }
}
