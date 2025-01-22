package no.nav.helse.person.inntekt

import java.util.*
import no.nav.helse.dto.deserialisering.SaksbehandlerInnDto
import no.nav.helse.dto.serialisering.SaksbehandlerUtDto

internal data class Saksbehandler(
    val id: UUID,
    val inntektsdata: Inntektsdata
) {
    fun dto() =
        SaksbehandlerUtDto(
            id = id,
            inntektsdata = inntektsdata.dto()
        )

    internal companion object {
        fun gjenopprett(dto: SaksbehandlerInnDto): Saksbehandler {
            return Saksbehandler(
                id = dto.id,
                inntektsdata = Inntektsdata.gjenopprett(dto.inntektsdata)
            )
        }
    }
}
