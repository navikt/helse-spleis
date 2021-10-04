package no.nav.helse

class Fødselsnummer(private val value: String) {
    override fun toString() = value
    override fun hashCode(): Int = value.hashCode()
    override fun equals(other: Any?) = other is Fødselsnummer && this.value == other.value
    fun somLong() = value.toLong()
}

fun String.somFødselsnummer() = Fødselsnummer(this)
