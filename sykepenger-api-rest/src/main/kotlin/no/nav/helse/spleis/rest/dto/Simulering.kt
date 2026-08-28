package no.nav.helse.spleis.rest.dto

import java.time.LocalDate

data class ApiSimuleringsdetaljer(
    val faktiskFom: LocalDate,
    val faktiskTom: LocalDate,
    val konto: String,
    val belop: Int,
    val tilbakeforing: Boolean,
    val sats: Double,
    val typeSats: String,
    val antallSats: Int,
    val uforegrad: Int,
    val klassekode: String,
    val klassekodeBeskrivelse: String,
    val utbetalingstype: String,
    val refunderesOrgNr: String
)

data class ApiSimuleringsutbetaling(
    val utbetalesTilId: String,
    val utbetalesTilNavn: String,
    val forfall: LocalDate,
    val feilkonto: Boolean,
    val detaljer: List<ApiSimuleringsdetaljer>
)

data class ApiSimuleringsperiode(
    val fom: LocalDate,
    val tom: LocalDate,
    val utbetalinger: List<ApiSimuleringsutbetaling>
)

data class ApiSimulering(
    val totalbelop: Int,
    val perioder: List<ApiSimuleringsperiode>
)
