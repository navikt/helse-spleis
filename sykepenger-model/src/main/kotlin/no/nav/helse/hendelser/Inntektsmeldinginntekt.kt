package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Avsender.ARBEIDSGIVER
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.nesteDag
import no.nav.helse.person.inntekt.ArbeidstakerFaktaavklartInntekt
import no.nav.helse.person.inntekt.Arbeidstakerinntektskilde
import no.nav.helse.person.inntekt.Inntektsdata
import no.nav.helse.økonomi.Inntekt

class Inntektsmeldinginntekt(
    override val behandlingsporing: Behandlingsporing,
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
