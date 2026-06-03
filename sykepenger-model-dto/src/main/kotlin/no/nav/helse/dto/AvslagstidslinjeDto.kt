package no.nav.helse.dto

data class AvslagstidslinjeDto(
    val perioder: List<AvslagstidslinjedagDto>
) {
    data class AvslagstidslinjedagDto(
        val begrunnelser: List<BegrunnelseDto>,
        val kilde: String,
        val periode: PeriodeDto
    )
}
