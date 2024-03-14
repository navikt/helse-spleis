package no.nav.helse.dto.deserialisering

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.GenerasjonTilstandDto
import no.nav.helse.dto.GenerasjonkildeDto

data class GenerasjonInnDto(
    val id: UUID,
    val tilstand: GenerasjonTilstandDto,
    val endringer: List<GenerasjonEndringInnDto>,
    val vedtakFattet: LocalDateTime?,
    val avsluttet: LocalDateTime?,
    val kilde: GenerasjonkildeDto,
)