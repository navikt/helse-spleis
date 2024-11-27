package no.nav.helse.spleis.graphql.dto

import java.time.LocalDate

data class GraphQLSimuleringsdetaljer(
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
    val refunderesOrgNr: String,
)

data class GraphQLSimuleringsutbetaling(
    val utbetalesTilId: String,
    val utbetalesTilNavn: String,
    val forfall: LocalDate,
    val feilkonto: Boolean,
    val detaljer: List<GraphQLSimuleringsdetaljer>,
)

data class GraphQLSimuleringsperiode(
    val fom: LocalDate,
    val tom: LocalDate,
    val utbetalinger: List<GraphQLSimuleringsutbetaling>,
)

data class GraphQLSimulering(val totalbelop: Int, val perioder: List<GraphQLSimuleringsperiode>)
