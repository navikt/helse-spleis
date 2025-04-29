package no.nav.helse.dto.deserialisering

import java.time.LocalDateTime
import no.nav.helse.dto.EndringskodeDto
import no.nav.helse.dto.FagområdeDto

data class FeriepengeoppdragInnDto(
    val mottaker: String,
    val fagområde: FagområdeDto,
    val linjer: List<FeriepengeutbetalingslinjeInnDto>,
    val fagsystemId: String,
    val endringskode: EndringskodeDto,
    val nettoBeløp: Int,
    val tidsstempel: LocalDateTime
)
