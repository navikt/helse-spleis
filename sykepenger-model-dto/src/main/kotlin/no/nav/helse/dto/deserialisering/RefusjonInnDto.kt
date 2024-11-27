package no.nav.helse.dto.deserialisering

import no.nav.helse.dto.EndringIRefusjonDto
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.dto.PeriodeDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class RefusjonInnDto(
    val meldingsreferanseId: UUID,
    val førsteFraværsdag: LocalDate?,
    val arbeidsgiverperioder: List<PeriodeDto>,
    val beløp: InntektbeløpDto.MånedligDouble?,
    val sisteRefusjonsdag: LocalDate?,
    val endringerIRefusjon: List<EndringIRefusjonDto>,
    val tidsstempel: LocalDateTime
)
