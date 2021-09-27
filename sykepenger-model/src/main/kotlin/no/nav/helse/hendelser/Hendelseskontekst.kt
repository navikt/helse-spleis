package no.nav.helse.hendelser

class Hendelseskontekst(
    private val kontekst: Map<String, String>
) {
    fun appendTo(setter: (String, String) -> Unit) = kontekst.forEach(setter)
}
