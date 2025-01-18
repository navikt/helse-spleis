package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.dto.deserialisering.InntektsopplysningInnDto
import no.nav.helse.dto.serialisering.InntektsopplysningUtDto
import no.nav.helse.hendelser.Subsumsjon
import no.nav.helse.økonomi.Inntekt

class Saksbehandler internal constructor(
    id: UUID,
    inntektsdata: Inntektsdata,
    val forklaring: String?,
    val subsumsjon: Subsumsjon?,
    val overstyrtInntekt: Inntektsopplysning?
) : Inntektsopplysning(id, inntektsdata) {

    constructor(dato: LocalDate, hendelseId: UUID, beløp: Inntekt, forklaring: String, subsumsjon: Subsumsjon?, tidsstempel: LocalDateTime) :
        this(UUID.randomUUID(), Inntektsdata(hendelseId, dato, beløp, tidsstempel), forklaring, subsumsjon, null)

    fun kopierMed(overstyrtInntekt: Inntektsopplysning) =
        Saksbehandler(id, inntektsdata, forklaring, subsumsjon, overstyrtInntekt)

    override fun dto() =
        InntektsopplysningUtDto.SaksbehandlerDto(
            id = id,
            inntektsdata = inntektsdata.dto(),
            forklaring = forklaring,
            subsumsjon = subsumsjon?.dto(),
            overstyrtInntekt = overstyrtInntekt!!.dto().id
        )

    internal companion object {
        fun gjenopprett(dto: InntektsopplysningInnDto.SaksbehandlerDto, inntekter: Map<UUID, Inntektsopplysning>) =
            Saksbehandler(
                id = dto.id,
                inntektsdata = Inntektsdata.gjenopprett(dto.inntektsdata),
                forklaring = dto.forklaring,
                subsumsjon = dto.subsumsjon?.let { Subsumsjon.gjenopprett(it) },
                overstyrtInntekt = inntekter.getValue(dto.overstyrtInntekt)
            )
    }
}
