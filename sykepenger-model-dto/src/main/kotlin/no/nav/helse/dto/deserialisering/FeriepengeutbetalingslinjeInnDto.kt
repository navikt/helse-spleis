package no.nav.helse.dto.deserialisering

import no.nav.helse.dto.FeriepengerendringskodeDto
import no.nav.helse.dto.FeriepengerklassekodeDto
import java.time.LocalDate

class FeriepengeutbetalingslinjeInnDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val beløp: Int,
    val refFagsystemId: String?,
    val delytelseId: Int,
    val refDelytelseId: Int?,
    val endringskode: FeriepengerendringskodeDto,
    val klassekode: FeriepengerklassekodeDto,
    val datoStatusFom: LocalDate?,
)
