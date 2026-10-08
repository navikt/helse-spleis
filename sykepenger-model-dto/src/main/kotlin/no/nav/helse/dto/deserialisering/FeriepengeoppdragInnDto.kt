package no.nav.helse.dto.deserialisering

import no.nav.helse.dto.FeriepengerendringskodeDto
import no.nav.helse.dto.FeriepengerfagområdeDto
import java.time.LocalDateTime

data class FeriepengeoppdragInnDto(
    val mottaker: String,
    val fagområde: FeriepengerfagområdeDto,
    val linjer: List<FeriepengeutbetalingslinjeInnDto>,
    val fagsystemId: String,
    val endringskode: FeriepengerendringskodeDto,
    val tidsstempel: LocalDateTime,
)
