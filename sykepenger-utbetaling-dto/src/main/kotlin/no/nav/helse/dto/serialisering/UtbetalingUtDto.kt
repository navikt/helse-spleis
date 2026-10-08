package no.nav.helse.dto.serialisering

import no.nav.helse.dto.PeriodeDto
import no.nav.helse.dto.UtbetalingTilstandDto
import no.nav.helse.dto.UtbetalingVurderingDto
import no.nav.helse.dto.UtbetalingtypeDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class UtbetalingUtDto(
    val id: UUID,
    val korrelasjonsId: UUID,
    val periode: PeriodeDto,
    val utbetalingstidslinje: UtbetalingstidslinjeUtDto,
    val arbeidsgiverOppdrag: OppdragUtDto,
    val personOppdrag: OppdragUtDto,
    val tidsstempel: LocalDateTime,
    val tilstand: UtbetalingTilstandDto,
    val type: UtbetalingtypeDto,
    val maksdato: LocalDate,
    val forbrukteSykedager: Int?,
    val gjenståendeSykedager: Int?,
    val annulleringer: List<UUID>,
    val vurdering: UtbetalingVurderingDto?,
    val overføringstidspunkt: LocalDateTime?,
    val avstemmingsnøkkel: Long?,
    val avsluttet: LocalDateTime?,
    val oppdatert: LocalDateTime,
)
