package no.nav.helse.dto.deserialisering

import no.nav.helse.dto.PeriodeDto
import no.nav.helse.dto.VedtaksperiodetilstandDto
import java.time.LocalDateTime
import java.util.UUID

data class VedtaksperiodeInnDto(
    val id: UUID,
    var tilstand: VedtaksperiodetilstandDto,
    val behandlinger: BehandlingerInnDto,
    val egenmeldingsperioder: List<PeriodeDto>,
    val opprettet: LocalDateTime,
    var oppdatert: LocalDateTime
)
