package no.nav.helse.dto.deserialisering

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.EndringIRefusjonDto
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.dto.PeriodeDto

data class RefusjonInnDto(
    val meldingsreferanseId: UUID,
    val førsteFraværsdag: LocalDate?,
    val arbeidsgiverperioder: List<PeriodeDto>,
    val beløp: InntektbeløpDto.MånedligDouble?,
    val sisteRefusjonsdag: LocalDate?,
    val endringerIRefusjon: List<EndringIRefusjonDto>,
    val tidsstempel: LocalDateTime
)