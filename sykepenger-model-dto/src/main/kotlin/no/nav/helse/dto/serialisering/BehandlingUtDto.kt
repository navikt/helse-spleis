package no.nav.helse.dto.serialisering

import no.nav.helse.dto.BehandlingkildeDto
import no.nav.helse.dto.BehandlingtilstandDto
import java.time.LocalDateTime
import java.util.UUID

data class BehandlingUtDto(
    val id: UUID,
    val tilstand: BehandlingtilstandDto,
    val endringer: List<BehandlingendringUtDto>,
    val vedtakFattet: LocalDateTime?,
    val avsluttet: LocalDateTime?,
    val kilde: BehandlingkildeDto,
)
