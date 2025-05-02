package no.nav.helse.dto.serialisering

import java.time.LocalDateTime
import no.nav.helse.dto.FeriepengerendringskodeDto
import no.nav.helse.dto.FeriepengerfagområdeDto

data class FeriepengeoppdragUtDto(
    val mottaker: String,
    val fagområde: FeriepengerfagområdeDto,
    val linjer: List<FeriepengeutbetalingslinjeUtDto>,
    val fagsystemId: String,
    val endringskode: FeriepengerendringskodeDto,
    val tidsstempel: LocalDateTime
)
