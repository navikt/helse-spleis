package no.nav.helse.person.inntekt

import no.nav.helse.dto.deserialisering.InntektsopplysningInnDto
import no.nav.helse.dto.serialisering.InntektsopplysningUtDto
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class IkkeRapportert(
    id: UUID,
    hendelseId: UUID,
    dato: LocalDate,
    tidsstempel: LocalDateTime,
) : AvklarbarSykepengegrunnlag(id, hendelseId, dato, Inntekt.INGEN, tidsstempel) {
    internal constructor(
        dato: LocalDate,
        hendelseId: UUID,
        tidsstempel: LocalDateTime,
    ) : this(UUID.randomUUID(), hendelseId, dato, tidsstempel)

    override fun kanOverstyresAv(ny: Inntektsopplysning) = true

    override fun blirOverstyrtAv(ny: Inntektsopplysning): Inntektsopplysning = ny.overstyrer(this)

    override fun erSamme(other: Inntektsopplysning): Boolean = other is IkkeRapportert && this.dato == other.dato

    override fun dto() = InntektsopplysningUtDto.IkkeRapportertDto(id, hendelseId, dato, tidsstempel)

    internal companion object {
        fun gjenopprett(dto: InntektsopplysningInnDto.IkkeRapportertDto) =
            IkkeRapportert(
                id = dto.id,
                hendelseId = dto.hendelseId,
                dato = dto.dato,
                tidsstempel = dto.tidsstempel,
            )
    }
}
