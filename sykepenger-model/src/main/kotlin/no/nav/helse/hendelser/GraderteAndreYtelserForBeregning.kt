package no.nav.helse.hendelser

import java.time.LocalDate

data class GraderteAndreYtelserForBeregning(
    val graderteAndreYtelserForBeregningPeriodeList: List<GraderteAndreYtelserForBeregningPeriode>,
    val graderteAndreYtelserType: GraderteAndreYtelserType
) {

    data class GraderteAndreYtelserForBeregningPeriode(
        val fom: LocalDate,
        val tom: LocalDate,
        val grad: Int
    ) {
        internal fun tilPeriode() = fom til tom
    }
}

enum class GraderteAndreYtelserType {
    FORELDREPENGER,
    SVANGERSKAPSPENGER,
    OMSORGSPENGER,
    PLEIEPENGER,
    OPPLARINGSPENGER,
}


