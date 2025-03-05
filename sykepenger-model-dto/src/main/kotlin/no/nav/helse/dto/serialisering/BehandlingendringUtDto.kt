package no.nav.helse.dto.serialisering

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.BeløpstidslinjeDto
import no.nav.helse.dto.DokumentsporingDto
import no.nav.helse.dto.InntektskildeDto
import no.nav.helse.dto.PeriodeDto
import no.nav.helse.dto.SykdomstidslinjeDto
import no.nav.helse.dto.UtbetalingTilstandDto

data class BehandlingendringUtDto(
    val id: UUID,
    val tidsstempel: LocalDateTime,
    val sykmeldingsperiode: PeriodeDto,
    val periode: PeriodeDto,
    val vilkårsgrunnlagId: UUID?,
    val skjæringstidspunkt: LocalDate,
    val skjæringstidspunkter: List<LocalDate>,
    val utbetalingId: UUID?,
    val utbetalingstatus: UtbetalingTilstandDto?,
    val dokumentsporing: DokumentsporingDto,
    val sykdomstidslinje: SykdomstidslinjeDto,
    val utbetalingstidslinje: UtbetalingstidslinjeUtDto,
    val refusjonstidslinje: BeløpstidslinjeDto,
    val inntektsendringer: BeløpstidslinjeDto,
    val arbeidsgiverperioder: List<PeriodeDto>,
    val dagerNavOvertarAnsvar: List<PeriodeDto>,
    val maksdatoresultat: MaksdatoresultatUtDto,
    val inntekter: Map<InntektskildeDto, BeløpstidslinjeDto>
)
