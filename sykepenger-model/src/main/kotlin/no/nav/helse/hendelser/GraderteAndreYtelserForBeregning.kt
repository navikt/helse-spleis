package no.nav.helse.hendelser

import no.nav.helse.utbetalingstidslinje.Arbeidsgiverberegning.Inntektskilde.Yrkesaktivitet
import java.time.LocalDate
import java.util.*

data class GraderteAndreYtelserForBeregning(
    val yrkesaktivitet: Yrkesaktivitet,
    val versjon: UUID,
    val dager: List<Dag>,
) {
    companion object {
        fun yrkesaktivitet(
            yrkesaktivitetstype: String,
            organisasjonsnummer: String?,
        ): Yrkesaktivitet =
            when (yrkesaktivitetstype) {
                "ARBEIDSTAKER" -> {
                    require(organisasjonsnummer != null && organisasjonsnummer.matches(Organisasjonsnummer)) {
                        "Ugyldig organisasjonsnummer for arbeidstaker med graderte andre ytelser"
                    }
                    Yrkesaktivitet.Arbeidstaker(organisasjonsnummer)
                }
                "FRILANS" -> Yrkesaktivitet.Frilans
                "SELVSTENDIG" -> Yrkesaktivitet.Selvstendig
                "ARBEIDSLEDIG" -> Yrkesaktivitet.Arbeidsledig
                else -> throw IllegalArgumentException("Ukjent yrkesaktivitetstype for graderte andre ytelser: $yrkesaktivitetstype")
            }

        private val Organisasjonsnummer = "\\d{9}".toRegex()
    }

    data class Dag(
        val dato: LocalDate,
        val trekkFraSykdomsgrad: Int,
        val sykdomsgradFørFordeling: Int,
    ) {
        init {
            require(trekkFraSykdomsgrad in 0..100) { "Trekk fra sykdomsgrad må være mellom 0 og 100, var $trekkFraSykdomsgrad" }
            require(sykdomsgradFørFordeling in 0..100) { "Sykdomsgrad før fordeling må være mellom 0 og 100, var $sykdomsgradFørFordeling" }
        }
    }
}
