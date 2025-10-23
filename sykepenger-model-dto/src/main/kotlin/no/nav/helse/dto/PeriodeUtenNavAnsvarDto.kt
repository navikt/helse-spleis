package no.nav.helse.dto

data class PeriodeUtenNavAnsvarDto(
    val omsluttendePeriode: PeriodeDto,
    val dagerUtenAnsvar: List<PeriodeDto>,
    val ferdigAvklart: Boolean
)
