package no.nav.helse.dto.serialisering

import no.nav.helse.dto.InntektDto
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.dto.MeldingsreferanseDto

data class InntektsdataUtDto(
    val hendelseId: MeldingsreferanseDto,
    val dato: LocalDate,
    val beløp: InntektDto,
    val tidsstempel: LocalDateTime
)
