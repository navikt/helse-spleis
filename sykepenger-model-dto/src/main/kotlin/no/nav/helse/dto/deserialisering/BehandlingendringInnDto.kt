package no.nav.helse.dto.deserialisering

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
    val inntektsendringer: BeløpstidslinjeDto,
    val skjæringstidspunkt: LocalDate,
    val skjæringstidspunkter: List<LocalDate>,
    val arbeidsgiverperiode: ArbeidsgiverperiodeavklaringDto,
    val egenmeldingsdager: List<PeriodeDto>,
    val dagerNavOvertarAnsvar: List<PeriodeDto>,
    val maksdatoresultat: MaksdatoresultatInnDto,
    val inntektjusteringer: Map<InntektskildeDto, BeløpstidslinjeDto>,
    val faktaavklartInntekt: SelvstendigFaktaavklartInntektInnDto?,
    val ventetid: PeriodeDto?
)
