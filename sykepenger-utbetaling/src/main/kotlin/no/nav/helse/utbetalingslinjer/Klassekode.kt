package no.nav.helse.utbetalingslinjer

enum class Klassekode(val verdi: String) {
    RefusjonIkkeOpplysningspliktig(verdi = "SPREFAG-IOP"),
    RefusjonFeriepengerIkkeOpplysningspliktig(verdi = "SPREFAGFER-IOP"),
    SykepengerArbeidstakerOrdinær(verdi = "SPATORD"),
    SykepengerArbeidstakerFeriepenger(verdi = "SPATFER");

    companion object {
        private val map = values().associateBy(Klassekode::verdi)
        fun from(verdi: String) = requireNotNull(map[verdi]) { "Støtter ikke klassekode: $verdi" }
    }
}
