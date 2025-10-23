package no.nav.helse.dto

data class DagerUtenNavAnsvaravklaringDto(
    val ferdigAvklart: Boolean,
    val dager: List<PeriodeDto>
)
