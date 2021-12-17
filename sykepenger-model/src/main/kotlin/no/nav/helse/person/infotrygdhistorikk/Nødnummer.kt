package no.nav.helse.person.infotrygdhistorikk

import no.nav.helse.person.IAktivitetslogg

internal class Nødnummer private constructor(private val nødnumre: Set<String>) {
    internal companion object {
        val Sykepenger = Nødnummer(setOf(
            "973626108",
            "971278420",
            "971248106",
            "973774670",
            "971278439",
            "971373032",
            "871400172", // Kun ment for registrering av hyre ved fiskere kategori 17
            "973626116",
            "873764252",
            "973695061",
            "973934554",
            "973713752",
            "971255366",
            "974810018",
            "871256322",
            "974858517",
            "973540017",
            "973739360",
            "971364254",
            "973542524",
            "971315563"
        ))
    }

    internal fun valider(aktivitetslogg: IAktivitetslogg, orgnummer: String) {
        if (orgnummer !in nødnumre) return
        aktivitetslogg.error("Det er registrert bruk av på nødnummer")
    }
}
