package no.nav.helse.dto.deserialisering

import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.dto.MeldingsreferanseDto
import java.time.LocalDate
import java.time.LocalDateTime

data class InntektsdataInnDto(
    val hendelseId: MeldingsreferanseDto,
    val dato: LocalDate,
    val beløp: InntektbeløpDto.MånedligDouble,
    val tidsstempel: LocalDateTime,
)
