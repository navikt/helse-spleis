package no.nav.helse.utbetalingslinjer

import java.time.LocalDate

data class OppdragDetaljer(
    val fagsystemId: String,
    val fagområde: String,
    val mottaker: String,
    val nettoBeløp: Int,
    val stønadsdager: Int,
    val fom: LocalDate,
    val tom: LocalDate,
    val linjer: List<LinjeDetaljer>,
) {
    data class LinjeDetaljer(
        val fom: LocalDate,
        val tom: LocalDate,
        val sats: Int,
        val grad: Double,
        val stønadsdager: Int,
        val totalbeløp: Int,
        val statuskode: String?,
    )
}
