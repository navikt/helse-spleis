package no.nav.helse.dto

import java.time.LocalDateTime
import java.util.UUID

class LazyVedtaksperiodeVenterDto(private val evaluer: () -> VedtaksperiodeVenterDto?) {
    private var erEvaluert: Boolean = false
    private var evaluert: VedtaksperiodeVenterDto? = null
    val value get() = if (erEvaluert) evaluert else {
        evaluert = evaluer()
        erEvaluert = true
        evaluert
    }
    override fun equals(other: Any?) = other is LazyVedtaksperiodeVenterDto && other.value == this.value
    override fun hashCode() = value.hashCode()
}

data class VedtaksperiodeVenterDto(
    val ventetSiden: LocalDateTime,
    val venterTil: LocalDateTime,
    val venterPå: VenterPåDto,
)

data class VenterPåDto(
    val vedtaksperiodeId: UUID,
    val organisasjonsnummer: String,
    val venteårsak: VenteårsakDto
)

data class VenteårsakDto(
    val hva: String,
    val hvorfor: String?
)