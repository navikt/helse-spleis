package no.nav.helse.person.inntekt

import no.nav.helse.dto.deserialisering.SkjønnsmessigFastsattInnDto
import no.nav.helse.dto.serialisering.SkjønnsmessigFastsattUtDto
import java.util.*

internal data class SkjønnsmessigFastsatt(
    val id: UUID,
    val inntektsdata: Inntektsdata,
) {
    fun dto() =
        SkjønnsmessigFastsattUtDto(
            id = id,
            inntektsdata = inntektsdata.dto(),
        )

    internal companion object {
        fun gjenopprett(dto: SkjønnsmessigFastsattInnDto): SkjønnsmessigFastsatt =
            SkjønnsmessigFastsatt(
                id = dto.id,
                inntektsdata = Inntektsdata.gjenopprett(dto.inntektsdata),
            )
    }
}
