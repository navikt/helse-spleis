package no.nav.helse.person.inntekt

import java.util.UUID
import no.nav.helse.dto.deserialisering.ArbeidstakerFaktaavklartInntektInnDto
import no.nav.helse.dto.serialisering.ArbeidstakerFaktaavklartInntektUtDto
import no.nav.helse.økonomi.Inntekt

internal data class ArbeidstakerFaktaavklartInntekt(
    override val id: UUID,
    override val inntektsdata: Inntektsdata,
    val inntektsopplysningskilde: Arbeidstakerinntektskilde
) : FaktaavklartInntekt {

    internal fun sammeBeløpOgKilde(other: ArbeidstakerFaktaavklartInntekt) =
        this.inntektsdata.beløp == other.inntektsdata.beløp && this.inntektsopplysningskilde::class == other.inntektsopplysningskilde::class

    internal fun dto() = ArbeidstakerFaktaavklartInntektUtDto(
        id = this.id,
        inntektsdata = this.inntektsdata.dto(),
        inntektsopplysningskilde = this.inntektsopplysningskilde.dto()
    )

    internal companion object {
        internal fun gjenopprett(dto: ArbeidstakerFaktaavklartInntektInnDto) = ArbeidstakerFaktaavklartInntekt(
            id = dto.id,
            inntektsdata = Inntektsdata.gjenopprett(dto.inntektsdata),
            inntektsopplysningskilde = Arbeidstakerinntektskilde.gjenopprett(dto.inntektsopplysningskilde)
        )
    }

    internal fun view() = ArbeistakerFaktaavklartInntektView(inntektsdata.hendelseId.id, inntektsdata.beløp)

    internal class ArbeistakerFaktaavklartInntektView(override val hendelseId: UUID, override val beløp: Inntekt) : FaktaavklartInntektView
}
