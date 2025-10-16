package no.nav.helse.dto

data class ArbeidsgiverperioderesultatDto(
    val omsluttendePeriode: PeriodeDto,
    val arbeidsgiverperiode: List<PeriodeDto>,
    val ferdigAvklart: Boolean
)
