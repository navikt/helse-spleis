package no.nav.helse.person.inntekt

import no.nav.helse.dto.deserialisering.InntektsdataInnDto
import no.nav.helse.dto.serialisering.InntektsdataUtDto
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.hendelser.MeldingsreferanseId

data class Inntektsdata(
    val hendelseId: MeldingsreferanseId,
    val dato: LocalDate,
    val beløp: Inntekt,
    val tidsstempel: LocalDateTime
) {

    fun funksjoneltLik(other: Inntektsdata) =
        this.dato == other.dato && this.beløp == other.beløp

    fun dto() = InntektsdataUtDto(
        hendelseId = hendelseId.dto(),
        dato = dato,
        beløp = beløp.dto(),
        tidsstempel = tidsstempel
    )

    companion object {
        fun ingen(hendelseId: MeldingsreferanseId, dato: LocalDate, tidsstempel: LocalDateTime = LocalDateTime.now()) = Inntektsdata(
            hendelseId = hendelseId,
            dato = dato,
            beløp = Inntekt.Companion.INGEN,
            tidsstempel = tidsstempel
        )

        fun gjenopprett(dto: InntektsdataInnDto) = Inntektsdata(
            hendelseId = MeldingsreferanseId.gjenopprett(dto.hendelseId),
            dato = dto.dato,
            beløp = Inntekt.Companion.gjenopprett(dto.beløp),
            tidsstempel = dto.tidsstempel
        )
    }
}
