package no.nav.helse.dto.serialisering

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.EndringIRefusjonDto
import no.nav.helse.dto.InntektDto
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.dto.PeriodeDto

data class RefusjonUtDto(
    val meldingsreferanseId: UUID,
    val førsteFraværsdag: LocalDate?,
    val arbeidsgiverperioder: List<PeriodeDto>,
    val beløp: InntektDto?,
    val sisteRefusjonsdag: LocalDate?,
    val endringerIRefusjon: List<EndringIRefusjonDto>,
    val tidsstempel: LocalDateTime
)