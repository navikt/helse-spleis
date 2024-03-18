package no.nav.helse.dto.serialisering

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.dto.InntektDto
import no.nav.helse.dto.InntektbeløpDto

data class RefusjonsopplysningUtDto(
    val meldingsreferanseId: UUID,
    val fom: LocalDate,
    val tom: LocalDate?,
    val beløp: InntektDto
)