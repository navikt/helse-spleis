package no.nav.helse.dto

import java.time.LocalDate

data class SimuleringResultatDto(
    val totalbeløp: Int,
    val perioder: List<SimulertPeriode>,
) {
    data class SimulertPeriode(
        val fom: LocalDate,
        val tom: LocalDate,
        val utbetalinger: List<SimulertUtbetaling>,
    )

    data class SimulertUtbetaling(
        val forfallsdato: LocalDate,
        val utbetalesTil: Mottaker,
        val feilkonto: Boolean,
        val detaljer: List<Detaljer>,
    )

    data class Detaljer(
        val fom: LocalDate,
        val tom: LocalDate,
        val konto: String,
        val beløp: Int,
        val klassekode: Klassekode,
        val uføregrad: Int,
        val utbetalingstype: String,
        val tilbakeføring: Boolean,
        val sats: Sats,
        val refunderesOrgnummer: String,
    )

    data class Sats(
        val sats: Double,
        val antall: Int,
        val type: String,
    )

    data class Klassekode(
        val kode: String,
        val beskrivelse: String,
    )

    data class Mottaker(
        val id: String,
        val navn: String,
    )
}
