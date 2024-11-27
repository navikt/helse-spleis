package no.nav.helse.dto.serialisering

import no.nav.helse.dto.EndringskodeDto
import no.nav.helse.dto.FagområdeDto
import no.nav.helse.dto.OppdragstatusDto
import no.nav.helse.dto.SimuleringResultatDto
import java.time.LocalDateTime

data class OppdragUtDto(
    val mottaker: String,
    val fagområde: FagområdeDto,
    val linjer: List<UtbetalingslinjeUtDto>,
    val fagsystemId: String,
    val endringskode: EndringskodeDto,
    val nettoBeløp: Int,
    val stønadsdager: Int,
    val totalbeløp: Int,
    val overføringstidspunkt: LocalDateTime?,
    val avstemmingsnøkkel: Long?,
    val status: OppdragstatusDto?,
    val tidsstempel: LocalDateTime,
    val erSimulert: Boolean,
    val simuleringsResultat: SimuleringResultatDto?,
)
