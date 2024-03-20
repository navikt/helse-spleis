package no.nav.helse.dto.deserialisering

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.VedtaksperiodetilstandDto

data class VedtaksperiodeInnDto(
    val id: UUID,
    var tilstand: VedtaksperiodetilstandDto,
    val behandlinger: BehandlingerInnDto,
    val opprettet: LocalDateTime,
    var oppdatert: LocalDateTime
)