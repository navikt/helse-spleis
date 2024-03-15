package no.nav.helse.dto.serialisering

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.GenerasjonTilstandDto
import no.nav.helse.dto.GenerasjonkildeDto

data class GenerasjonUtDto(
    val id: UUID,
    val tilstand: GenerasjonTilstandDto,
    val endringer: List<GenerasjonEndringUtDto>,
    val vedtakFattet: LocalDateTime?,
    val avsluttet: LocalDateTime?,
    val kilde: GenerasjonkildeDto,
)