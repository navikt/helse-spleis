package no.nav.helse.dto.deserialisering

import java.time.LocalDateTime
import no.nav.helse.dto.FeriepengerendringskodeDto
import no.nav.helse.dto.FeriepengerfagområdeDto

data class FeriepengeoppdragInnDto(
    val mottaker: String,
    val fagområde: FeriepengerfagområdeDto,
    val linjer: List<FeriepengeutbetalingslinjeInnDto>,
    val fagsystemId: String,
    val endringskode: FeriepengerendringskodeDto,
    val tidsstempel: LocalDateTime
)
