package no.nav.helse.dto

sealed interface SelvstendigOpptjeningDto {
    data object Oppfylt : SelvstendigOpptjeningDto

    data object IkkeOppfylt : SelvstendigOpptjeningDto

    data object IkkeVurdert : SelvstendigOpptjeningDto
}
