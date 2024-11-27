package no.nav.helse.dto

sealed class AvsenderDto {
    data object SYKMELDT : AvsenderDto()

    data object ARBEIDSGIVER : AvsenderDto()

    data object SAKSBEHANDLER : AvsenderDto()

    data object SYSTEM : AvsenderDto()
}
