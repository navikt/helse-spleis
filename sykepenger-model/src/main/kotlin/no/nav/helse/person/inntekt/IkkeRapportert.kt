package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.util.*
import no.nav.helse.dto.deserialisering.InntektsopplysningInnDto
import no.nav.helse.dto.serialisering.InntektsopplysningUtDto

internal class IkkeRapportert(id: UUID, inntektsdata: Inntektsdata) : SkatteopplysningSykepengegrunnlag(id, inntektsdata) {
    internal constructor(dato: LocalDate, hendelseId: UUID) : this(UUID.randomUUID(), Inntektsdata.ingen(hendelseId, dato))

    override fun erSamme(other: Inntektsopplysning): Boolean {
        return other is IkkeRapportert && this.inntektsdata.funksjoneltLik(other.inntektsdata)
    }

    override fun dto() =
        InntektsopplysningUtDto.IkkeRapportertDto(id, inntektsdata.dto())

    internal companion object {
        fun gjenopprett(dto: InntektsopplysningInnDto.IkkeRapportertDto) =
            IkkeRapportert(
                id = dto.id,
                inntektsdata = Inntektsdata.gjenopprett(dto.inntektsdata)
            )
    }
}
