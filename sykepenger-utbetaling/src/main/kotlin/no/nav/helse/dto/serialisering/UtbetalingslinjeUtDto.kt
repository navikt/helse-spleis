package no.nav.helse.dto.serialisering

import java.time.LocalDate
import no.nav.helse.dto.EndringskodeDto
import no.nav.helse.dto.KlassekodeDto
import no.nav.helse.dto.SatstypeDto

data class UtbetalingslinjeUtDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val satstype: SatstypeDto,
    val beløp: Int?,
    val grad: Int?,
    val refFagsystemId: String?,
    val delytelseId: Int,
    val refDelytelseId: Int?,
    val endringskode: EndringskodeDto,
    val klassekode: KlassekodeDto,
    val datoStatusFom: LocalDate?
)