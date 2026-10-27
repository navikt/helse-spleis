package no.nav.helse.dto.deserialisering

import no.nav.helse.dto.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class BehandlingendringInnDto(
    val id: UUID,
    val tidsstempel: LocalDateTime,
    val sykmeldingsperiode: PeriodeDto,
    val periode: PeriodeDto,
    val arbeidssituasjon: ArbeidssituasjonDto,
    val vilkårsgrunnlagId: UUID?,
    val utbetalingId: UUID?,
    val dokumentsporing: DokumentsporingDto,
    val sykdomstidslinje: SykdomstidslinjeDto,
    val utbetalingstidslinje: UtbetalingstidslinjeInnDto,
    val refusjonstidslinje: BeløpstidslinjeDto,
    val skjæringstidspunkt: LocalDate,
    val skjæringstidspunkter: List<LocalDate>,
    val dagerUtenNavAnsvar: DagerUtenNavAnsvaravklaringDto,
    val egenmeldingsdager: List<PeriodeDto>,
    val dagerNavOvertarAnsvar: List<PeriodeDto>,
    val avslagstidslinje: AvslagstidslinjeDto,
    val maksdatoresultat: MaksdatoresultatInnDto,
    val inntektjusteringer: Map<InntektskildeDto, BeløpstidslinjeDto>,
    val faktaavklartInntekt: FaktaavklartInntektInnDto?,
    val korrigertInntekt: SaksbehandlerInnDto?,
    val beregningId: UUID,
    val graderteAndreYtelserId: UUID?,
)
