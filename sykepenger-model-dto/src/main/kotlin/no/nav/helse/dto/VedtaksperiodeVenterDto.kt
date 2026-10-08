package no.nav.helse.dto

import java.time.LocalDateTime
import java.util.*

data class VedtaksperiodeVenterDto(
    val ventetSiden: LocalDateTime,
    val venterTil: LocalDateTime,
    val venterPå: VenterPåDto,
)

data class VenterPåDto(
    val vedtaksperiodeId: UUID,
    val organisasjonsnummer: String,
    val venteårsak: VenteårsakDto,
)

data class VenteårsakDto(
    val hva: String,
    val hvorfor: String?,
)
