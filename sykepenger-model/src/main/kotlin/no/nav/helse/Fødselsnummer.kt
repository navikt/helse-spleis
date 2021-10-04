package no.nav.helse

class Fødselsnummer private constructor(private val value: String) {
    override fun toString() = value
    override fun hashCode(): Int = value.hashCode()
    override fun equals(other: Any?) = other is Fødselsnummer && this.value == other.value
    fun somLong() = value.toLong()

    companion object {
        fun tilFødselsnummer(fnr: String): Fødselsnummer {
            if (fnr.length == 11 && alleTegnErSiffer(fnr)) return Fødselsnummer(fnr)
            else throw RuntimeException("$fnr er ikke et gyldig fødselsnummer")
        }
        private fun alleTegnErSiffer(string: String) = string.matches(Regex("\\d*"))
    }
}

fun String.somFødselsnummer() = Fødselsnummer.tilFødselsnummer(this)
