package no.nav.helse.dto.serialisering

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.ArbeidsgiverperiodeavklaringDto
import no.nav.helse.dto.ArbeidssituasjonDto
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
    val inntektsendringer: BeløpstidslinjeDto,
    val arbeidsgiverperiode: ArbeidsgiverperiodeavklaringDto,
    val dagerNavOvertarAnsvar: List<PeriodeDto>,
    val egenmeldingsdager: List<PeriodeDto>,
    val maksdatoresultat: MaksdatoresultatUtDto,
    val inntektjusteringer: Map<InntektskildeDto, BeløpstidslinjeDto>,
    val faktaavklartInntekt: SelvstendigFaktaavklartInntektUtDto?,
    val ventetid: PeriodeDto?
)
