package no.nav.helse.dto

data class ArbeidsgiverperiodeavklaringDto(
    val ferdigAvklart: Boolean,
    val dager: List<PeriodeDto>
)
