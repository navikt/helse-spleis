package no.nav.helse.utbetalingslinjer

internal enum class Klassekode(internal val verdi: String) {
    RefusjonIkkeOpplysningspliktig(verdi = "SPREFAG-IOP");

    internal companion object {
        private val map = values().associateBy(Klassekode::verdi)
        fun from(verdi: String) = requireNotNull(map[verdi]) { "Støtter ikke klassekode: $verdi" }
    }
}
