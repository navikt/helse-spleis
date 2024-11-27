package no.nav.helse.dto.serialisering

import no.nav.helse.dto.InntektDto
import java.time.LocalDate
import java.time.LocalDateTime

data class InfotrygdInntektsopplysningUtDto(
    val orgnummer: String,
    val sykepengerFom: LocalDate,
    val inntekt: InntektDto,
    val refusjonTilArbeidsgiver: Boolean,
    val refusjonTom: LocalDate?,
    val lagret: LocalDateTime?,
)
