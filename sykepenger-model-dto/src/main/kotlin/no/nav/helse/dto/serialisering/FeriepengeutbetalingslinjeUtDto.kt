package no.nav.helse.dto.serialisering

import java.time.LocalDate
import no.nav.helse.dto.FeriepengerendringskodeDto
import no.nav.helse.dto.FeriepengerklassekodeDto

data class FeriepengeutbetalingslinjeUtDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val beløp: Int,
    val refFagsystemId: String?,
    val delytelseId: Int,
    val refDelytelseId: Int?,
    val endringskode: FeriepengerendringskodeDto,
    val klassekode: FeriepengerklassekodeDto,
    val datoStatusFom: LocalDate?,
    val statuskode: String?
)
