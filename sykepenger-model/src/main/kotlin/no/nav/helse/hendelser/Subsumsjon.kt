package no.nav.helse.hendelser

import no.nav.helse.memento.SubsumsjonMemento

class Subsumsjon(
    val paragraf: String,
    val ledd: Int?,
    val bokstav: String?,
) {
    internal fun memento() = SubsumsjonMemento(paragraf = paragraf, ledd = ledd, bokstav = bokstav)
}