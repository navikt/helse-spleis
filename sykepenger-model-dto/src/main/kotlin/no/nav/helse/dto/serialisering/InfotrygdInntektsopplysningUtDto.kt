package no.nav.helse.dto.serialisering

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.dto.InntektDto

data class InfotrygdInntektsopplysningUtDto(
    val orgnummer: String,
    val sykepengerFom: LocalDate,
    val inntekt: InntektDto,
    val refusjonTilArbeidsgiver: Boolean,
    val refusjonTom: LocalDate?,
    val lagret: LocalDateTime?
)