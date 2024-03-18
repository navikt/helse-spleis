package no.nav.helse.dto.deserialisering

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.dto.InntektbeløpDto

data class InfotrygdInntektsopplysningInnDto(
    val orgnummer: String,
    val sykepengerFom: LocalDate,
    val inntekt: InntektbeløpDto.MånedligDouble,
    val refusjonTilArbeidsgiver: Boolean,
    val refusjonTom: LocalDate?,
    val lagret: LocalDateTime?
)