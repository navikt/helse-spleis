package no.nav.helse.person.inntekt

import java.util.*
import no.nav.helse.dto.deserialisering.InntektsopplysningInnDto
import no.nav.helse.dto.serialisering.InntektsopplysningUtDto

internal class Infotrygd(id: UUID, inntektsdata: Inntektsdata) : Inntektsopplysning(id, inntektsdata) {

    override fun erSamme(other: Inntektsopplysning): Boolean {
        if (other !is Infotrygd) return false
        return this.inntektsdata.funksjoneltLik(other.inntektsdata)
    }

    override fun dto() =
        InntektsopplysningUtDto.InfotrygdDto(id, inntektsdata.dto())

    internal companion object {
        fun gjenopprett(dto: InntektsopplysningInnDto.InfotrygdDto) =
            Infotrygd(
                id = dto.id,
                inntektsdata = Inntektsdata.gjenopprett(dto.inntektsdata)
            )
    }
}
