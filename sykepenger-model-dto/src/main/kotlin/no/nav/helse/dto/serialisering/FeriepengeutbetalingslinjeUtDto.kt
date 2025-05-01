package no.nav.helse.dto.serialisering

import java.time.LocalDate
import no.nav.helse.dto.EndringskodeDto
import no.nav.helse.dto.KlassekodeDto

data class FeriepengeutbetalingslinjeUtDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val beløp: Int,
    val refFagsystemId: String?,
    val delytelseId: Int,
    val refDelytelseId: Int?,
    val endringskode: EndringskodeDto,
    val klassekode: KlassekodeDto,
    val datoStatusFom: LocalDate?,
    val statuskode: String?
)
