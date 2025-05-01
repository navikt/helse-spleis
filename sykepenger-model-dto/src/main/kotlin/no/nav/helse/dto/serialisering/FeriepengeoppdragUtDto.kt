package no.nav.helse.dto.serialisering

import java.time.LocalDateTime
import no.nav.helse.dto.EndringskodeDto
import no.nav.helse.dto.FagområdeDto

data class FeriepengeoppdragUtDto(
    val mottaker: String,
    val fagområde: FagområdeDto,
    val linjer: List<FeriepengeutbetalingslinjeUtDto>,
    val fagsystemId: String,
    val endringskode: EndringskodeDto,
    val tidsstempel: LocalDateTime
)
