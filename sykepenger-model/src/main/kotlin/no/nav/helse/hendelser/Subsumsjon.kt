package no.nav.helse.hendelser

import no.nav.helse.dto.SubsumsjonDto

class Subsumsjon(
    val paragraf: String,
    val ledd: Int?,
    val bokstav: String?,
) {
    internal fun dto() = SubsumsjonDto(paragraf = paragraf, ledd = ledd, bokstav = bokstav)

    companion object {
        fun gjenopprett(dto: SubsumsjonDto) =
            Subsumsjon(
                paragraf = dto.paragraf,
                ledd = dto.ledd,
                bokstav = dto.bokstav
            )
    }
}