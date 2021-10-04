package no.nav.helse

class Organisasjonsnummer private constructor(private val value: String) {
    override fun toString() = value
    override fun hashCode(): Int = value.hashCode()
    override fun equals(other: Any?) = other is Organisasjonsnummer && this.value == other.value
    fun somLong() = value.toLong()

    companion object {
        fun tilOrganisasjonsnummer(fnr: String): Organisasjonsnummer {
            if (fnr.length == 9 && alleTegnErSiffer(fnr)) return Organisasjonsnummer(fnr)
            else throw RuntimeException("$fnr er ikke et gyldig fødselsnummer")
        }
        private fun alleTegnErSiffer(string: String) = string.matches(Regex("\\d*"))
    }
}

fun String.somOrganisasjonsnummer() = Organisasjonsnummer.tilOrganisasjonsnummer(this)
