package no.nav.helse.dto.serialisering

import no.nav.helse.dto.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class BehandlingendringUtDto(
    val id: UUID,
    val tidsstempel: LocalDateTime,
    val sykmeldingsperiode: PeriodeDto,
    val periode: PeriodeDto,
    val arbeidssituasjon: ArbeidssituasjonDto,
    val vilkårsgrunnlagId: UUID?,
    val skjæringstidspunkt: LocalDate,
    val skjæringstidspunkter: List<LocalDate>,
    val utbetalingId: UUID?,
    val utbetalingstatus: UtbetalingTilstandDto?,
    val dokumentsporing: DokumentsporingDto,
    val sykdomstidslinje: SykdomstidslinjeDto,
    val utbetalingstidslinje: UtbetalingstidslinjeUtDto,
    val refusjonstidslinje: BeløpstidslinjeDto,
    val dagerUtenNavAnsvar: DagerUtenNavAnsvaravklaringDto,
    val dagerNavOvertarAnsvar: List<PeriodeDto>,
    val avslagstidslinje: AvslagstidslinjeDto,
    val egenmeldingsdager: List<PeriodeDto>,
    val maksdatoresultat: MaksdatoresultatUtDto,
    val inntektjusteringer: Map<InntektskildeDto, BeløpstidslinjeDto>,
    val faktaavklartInntekt: FaktaavklartInntektUtDto?,
    val korrigertInntekt: SaksbehandlerUtDto?,
    val beregningId: UUID,
    val graderteAndreYtelserId: UUID?,
)
