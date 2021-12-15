package no.nav.helse.person.infotrygdhistorikk

import no.nav.helse.person.IAktivitetslogg

internal class Nødnummer private constructor(private val nødnumre: Set<String>) {
    internal companion object {
        val Sykepenger = Nødnummer(setOf(
            "973626108",
            "971278420",
            "971248106",
            "973774670",
            "971278439"
        ))
    }

    internal fun valider(aktivitetslogg: IAktivitetslogg, orgnummer: String) {
        if (orgnummer !in nødnumre) return
        aktivitetslogg.error("Det er registrert bruk av på nødnummer i inntekt eller utbetaling")
    }
}
