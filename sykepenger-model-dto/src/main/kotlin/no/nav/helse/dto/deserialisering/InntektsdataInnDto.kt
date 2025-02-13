package no.nav.helse.dto.deserialisering

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.dto.MeldingsreferanseDto

data class InntektsdataInnDto(
    val hendelseId: MeldingsreferanseDto,
    val dato: LocalDate,
    val beløp: InntektbeløpDto.MånedligDouble,
    val tidsstempel: LocalDateTime
)
