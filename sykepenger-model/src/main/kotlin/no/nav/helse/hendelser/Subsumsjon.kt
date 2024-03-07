package no.nav.helse.hendelser

import no.nav.helse.dto.SubsumsjonDto

class Subsumsjon(
    val paragraf: String,
    val ledd: Int?,
    val bokstav: String?,
) {
    internal fun dto() = SubsumsjonDto(paragraf = paragraf, ledd = ledd, bokstav = bokstav)
}