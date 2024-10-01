package no.nav.helse

@JvmInline
value class Personidentifikator(private val value: String) {
    override fun toString() = value
    fun toLong() =
        value.toLong()
}