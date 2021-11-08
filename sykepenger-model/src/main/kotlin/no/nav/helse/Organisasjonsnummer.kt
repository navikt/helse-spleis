package no.nav.helse

class Organisasjonsnummer private constructor(private val value: String) {
    override fun toString() = value
    override fun hashCode(): Int = value.hashCode()
    override fun equals(other: Any?) = other is Organisasjonsnummer && this.value == other.value
    fun somLong() = value.toLong()

    companion object {
        fun tilOrganisasjonsnummer(orgnummer: String): Organisasjonsnummer {
            if (orgnummer.length == 9 && alleTegnErSiffer(orgnummer)) return Organisasjonsnummer(orgnummer)
            else throw RuntimeException("$orgnummer er ikke et gyldig organisasjonsnummer")
        }
        private fun alleTegnErSiffer(string: String) = string.matches(Regex("\\d*"))
    }
}

fun String.somOrganisasjonsnummer() = Organisasjonsnummer.tilOrganisasjonsnummer(this)
