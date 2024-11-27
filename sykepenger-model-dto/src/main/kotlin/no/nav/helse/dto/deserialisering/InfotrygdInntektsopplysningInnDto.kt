package no.nav.helse.dto.deserialisering

import no.nav.helse.dto.InntektbeløpDto
import java.time.LocalDate
import java.time.LocalDateTime

data class InfotrygdInntektsopplysningInnDto(
    val orgnummer: String,
    val sykepengerFom: LocalDate,
    val inntekt: InntektbeløpDto.MånedligDouble,
    val refusjonTilArbeidsgiver: Boolean,
    val refusjonTom: LocalDate?,
    val lagret: LocalDateTime?
)
