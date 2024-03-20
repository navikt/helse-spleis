package no.nav.helse.dto.deserialisering

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.BehandlingtilstandDto
import no.nav.helse.dto.BehandlingkildeDto

data class BehandlingInnDto(
    val id: UUID,
    val tilstand: BehandlingtilstandDto,
    val endringer: List<BehandlingendringInnDto>,
    val vedtakFattet: LocalDateTime?,
    val avsluttet: LocalDateTime?,
    val kilde: BehandlingkildeDto,
)