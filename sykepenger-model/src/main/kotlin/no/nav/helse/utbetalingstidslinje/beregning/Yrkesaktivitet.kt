package no.nav.helse.utbetalingstidslinje.beregning

sealed interface Yrkesaktivitet {
    data class Arbeidstaker(val organisasjonsnummer: String) : Yrkesaktivitet
    data object Selvstendig : Yrkesaktivitet
}
