package no.nav.helse.dto.deserialisering

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.BehandlingkildeDto
import no.nav.helse.dto.BehandlingtilstandDto

data class BehandlingInnDto(
    val id: UUID,
    val tilstand: BehandlingtilstandDto,
    val endringer: List<BehandlingendringInnDto>,
    val vedtakFattet: LocalDateTime?,
    val avsluttet: LocalDateTime?,
    val kilde: BehandlingkildeDto,
)