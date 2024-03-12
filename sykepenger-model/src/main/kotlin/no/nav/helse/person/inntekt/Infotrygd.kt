package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.InntektsopplysningDto
import no.nav.helse.økonomi.Inntekt

internal class Infotrygd(
    id: UUID,
    dato: LocalDate,
    hendelseId: UUID,
    beløp: Inntekt,
    tidsstempel: LocalDateTime
) : Inntektsopplysning(id, hendelseId, dato, beløp, tidsstempel) {
    override fun accept(visitor: InntektsopplysningVisitor) {
        visitor.visitInfotrygd(this, id, dato, hendelseId, beløp, tidsstempel)
    }

    override fun kanOverstyresAv(ny: Inntektsopplysning) = false
    override fun blirOverstyrtAv(ny: Inntektsopplysning): Inntektsopplysning {
        throw IllegalStateException("Infotrygd kan ikke bli overstyrt")
    }

    override fun erSamme(other: Inntektsopplysning): Boolean {
        if (other !is Infotrygd) return false
        return this.dato == other.dato && this.beløp == other.beløp
    }

    override fun dto() =
        InntektsopplysningDto.InfotrygdDto(id, hendelseId, dato, beløp.dto(), tidsstempel)

    internal companion object {
        fun gjenopprett(dto: InntektsopplysningDto.InfotrygdDto) =
            Infotrygd(
                id = dto.id,
                hendelseId = dto.hendelseId,
                dato = dto.dato,
                beløp = Inntekt.gjenopprett(dto.beløp),
                tidsstempel = dto.tidsstempel
            )
    }
}