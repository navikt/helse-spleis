package no.nav.helse.dto.serialisering

import no.nav.helse.dto.FeriepengerendringskodeDto
import no.nav.helse.dto.FeriepengerfagområdeDto
import java.time.LocalDateTime

data class FeriepengeoppdragUtDto(
    val mottaker: String,
    val fagområde: FeriepengerfagområdeDto,
    val linjer: List<FeriepengeutbetalingslinjeUtDto>,
    val fagsystemId: String,
    val endringskode: FeriepengerendringskodeDto,
    val tidsstempel: LocalDateTime,
    val totalbeløp: Int,
)
