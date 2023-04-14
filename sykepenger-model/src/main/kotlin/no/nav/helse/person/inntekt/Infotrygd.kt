package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
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

    override fun overstyres(ny: Inntektsopplysning) = this

    override fun omregnetÅrsinntekt(): Inntekt = beløp

    override fun erSamme(other: Inntektsopplysning): Boolean {
        if (other !is Infotrygd) return false
        return this.dato == other.dato && this.beløp == other.beløp
    }
}