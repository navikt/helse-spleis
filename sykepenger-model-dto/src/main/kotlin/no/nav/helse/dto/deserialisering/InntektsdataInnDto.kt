package no.nav.helse.dto.deserialisering

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.dto.InntektbeløpDto

data class InntektsdataInnDto(
    val hendelseId: UUID,
    val dato: LocalDate,
    val beløp: InntektbeløpDto.MånedligDouble,
    val tidsstempel: LocalDateTime
)
