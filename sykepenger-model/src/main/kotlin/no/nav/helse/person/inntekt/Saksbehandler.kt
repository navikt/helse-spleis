package no.nav.helse.person.inntekt

import java.util.*
import no.nav.helse.dto.deserialisering.SaksbehandlerInnDto
import no.nav.helse.dto.serialisering.SaksbehandlerUtDto
import no.nav.helse.økonomi.Inntekt

internal data class Saksbehandler(
    val id: UUID,
    val inntektsdata: Inntektsdata
) {
    fun dto() =
        SaksbehandlerUtDto(
            id = id,
            inntektsdata = inntektsdata.dto()
        )

    fun view() = SaksbehandlerView(inntektsdata.hendelseId.id, inntektsdata.beløp)

    internal companion object {
        fun gjenopprett(dto: SaksbehandlerInnDto): Saksbehandler {
            return Saksbehandler(
                id = dto.id,
                inntektsdata = Inntektsdata.gjenopprett(dto.inntektsdata)
            )
        }
    }

    internal class SaksbehandlerView(val hendelseId: UUID, val beløp: Inntekt)
}
