package no.nav.helse.hendelser

import no.nav.helse.utbetalingstidslinje.Arbeidsgiverberegning.Inntektskilde.Yrkesaktivitet
import java.time.LocalDate

data class GraderteAndreYtelserForBeregning(
    val graderteAndreYtelserForBeregningPeriodeList: List<GraderteAndreYtelserForBeregningPeriode>,
    val graderteAndreYtelserType: GraderteAndreYtelserType,
    val yrkesaktivitet: Yrkesaktivitet,
) {
    companion object {
        fun yrkesaktivitet(verdi: String): Yrkesaktivitet =
            when (val normalisert = verdi.uppercase()) {
                "SELVSTENDIG" -> Yrkesaktivitet.Selvstendig
                "FRILANS" -> Yrkesaktivitet.Frilans
                else -> {
                    require(normalisert.matches(Organisasjonsnummer)) { "Ukjent yrkesaktivitet for graderte andre ytelser: $verdi" }
                    Yrkesaktivitet.Arbeidstaker(normalisert)
                }
            }

        private val Organisasjonsnummer = "\\d{9}".toRegex()
    }

    data class GraderteAndreYtelserForBeregningPeriode(
        val fom: LocalDate,
        val tom: LocalDate,
        val grad: Int,
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
