package no.nav.helse.dto.serialisering

import no.nav.helse.dto.AvsenderDto
import no.nav.helse.dto.InntektDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class RefusjonsopplysningUtDto(
    val meldingsreferanseId: UUID,
    val fom: LocalDate,
    val tom: LocalDate?,
    val beløp: InntektDto,
    val avsender: AvsenderDto,
    val tidsstempel: LocalDateTime,
)
