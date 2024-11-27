package no.nav.helse.dto.deserialisering

import no.nav.helse.dto.PeriodeDto
import no.nav.helse.dto.UtbetalingTilstandDto
import no.nav.helse.dto.UtbetalingVurderingDto
import no.nav.helse.dto.UtbetalingtypeDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class UtbetalingInnDto(
    val id: UUID,
    val korrelasjonsId: UUID,
    val periode: PeriodeDto,
    val utbetalingstidslinje: UtbetalingstidslinjeInnDto,
    val arbeidsgiverOppdrag: OppdragInnDto,
    val personOppdrag: OppdragInnDto,
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
