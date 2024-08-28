package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.deserialisering.InntektsopplysningInnDto
import no.nav.helse.dto.serialisering.InntektsopplysningUtDto
import no.nav.helse.økonomi.Inntekt

internal class InntektFraSøknad(
    id: UUID,
    dato: LocalDate,
    hendelseId: UUID,
    beløp: Inntekt,
    tidsstempel: LocalDateTime
) : Inntektsopplysning(id, hendelseId, dato, beløp, tidsstempel) {

    override fun accept(visitor: InntektsopplysningVisitor) {
        visitor.visitInntektFraSøknad(this, id, dato, hendelseId, beløp, tidsstempel)
    }

    override fun blirOverstyrtAv(ny: Inntektsopplysning): Inntektsopplysning {
        return ny.overstyrer(this)
    }

    override fun erSamme(other: Inntektsopplysning): Boolean {
        if (other !is InntektFraSøknad) return false
        return this.dato == other.dato && this.beløp == other.beløp
    }

    override fun dto(): InntektsopplysningUtDto {
        return InntektsopplysningUtDto.InntektFraSøknadDto(id, hendelseId, dato, beløp.dto(), tidsstempel)
    }

    internal companion object {
        fun gjenopprett(dto: InntektsopplysningInnDto.InntektFraSøknadDto) =
            InntektFraSøknad(
                id = dto.id,
                hendelseId = dto.hendelseId,
                dato = dto.dato,
                beløp = Inntekt.gjenopprett(dto.beløp),
                tidsstempel = dto.tidsstempel
            )
    }

}