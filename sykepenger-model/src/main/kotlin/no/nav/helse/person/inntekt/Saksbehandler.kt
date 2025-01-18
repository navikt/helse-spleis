package no.nav.helse.person.inntekt

import java.util.*
import no.nav.helse.dto.deserialisering.InntektsopplysningInnDto
import no.nav.helse.dto.serialisering.InntektsopplysningUtDto

class Saksbehandler internal constructor(
    id: UUID,
    inntektsdata: Inntektsdata,
    val overstyrtInntekt: Inntektsopplysning
) : Inntektsopplysning(id, inntektsdata) {

    fun kopierMed(overstyrtInntekt: Inntektsopplysning) =
        Saksbehandler(id, inntektsdata, overstyrtInntekt)

    override fun dto() =
        InntektsopplysningUtDto.SaksbehandlerDto(
            id = id,
            inntektsdata = inntektsdata.dto(),
            overstyrtInntekt = overstyrtInntekt.dto().id
        )

    internal companion object {
        fun gjenopprett(dto: InntektsopplysningInnDto.SaksbehandlerDto, inntekter: Map<UUID, Inntektsopplysning>) =
            Saksbehandler(
                id = dto.id,
                inntektsdata = Inntektsdata.gjenopprett(dto.inntektsdata),
                overstyrtInntekt = inntekter.getValue(dto.overstyrtInntekt)
            )
    }
}
