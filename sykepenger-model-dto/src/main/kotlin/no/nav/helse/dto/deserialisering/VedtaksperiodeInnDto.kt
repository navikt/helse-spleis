package no.nav.helse.dto.deserialisering

import no.nav.helse.dto.VedtaksperiodetilstandDto
import java.time.LocalDateTime
import java.util.UUID

data class VedtaksperiodeInnDto(
    val id: UUID,
    val tilstand: VedtaksperiodetilstandDto,
    val behandlinger: BehandlingerInnDto,
    val opprettet: LocalDateTime,
    val oppdatert: LocalDateTime,
)
