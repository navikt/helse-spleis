package no.nav.helse.dto.serialisering

import no.nav.helse.dto.InntektDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class InntektsdataUtDto(
    val hendelseId: UUID,
    val dato: LocalDate,
    val beløp: InntektDto,
    val tidsstempel: LocalDateTime
)
