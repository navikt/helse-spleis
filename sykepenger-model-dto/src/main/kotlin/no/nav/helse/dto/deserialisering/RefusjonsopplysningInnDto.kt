package no.nav.helse.dto.deserialisering

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.AvsenderDto
import no.nav.helse.dto.InntektbeløpDto

data class RefusjonsopplysningInnDto(
    val meldingsreferanseId: UUID,
    val fom: LocalDate,
    val tom: LocalDate?,
    val beløp: InntektbeløpDto.MånedligDouble,
    val avsender: AvsenderDto,
    val tidsstempel: LocalDateTime
)