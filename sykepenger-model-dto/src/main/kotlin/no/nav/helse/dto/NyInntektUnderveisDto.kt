package no.nav.helse.dto

data class NyInntektUnderveisDto(
    val orgnummer: String,
    val beløpstidslinje: BeløpstidslinjeDto
)
