package no.nav.helse.utbetalingslinjer

import java.time.LocalDateTime
import no.nav.helse.dto.SimuleringResultatDto

interface OppdragVisitor: UtbetalingslinjeVisitor {
    fun preVisitOppdrag(
        oppdrag: Oppdrag,
        fagområde: Fagområde,
        fagsystemId: String,
        mottaker: String,
        nettoBeløp: Int,
        tidsstempel: LocalDateTime,
        endringskode: Endringskode,
        avstemmingsnøkkel: Long?,
        status: Oppdragstatus?,
        overføringstidspunkt: LocalDateTime?,
        erSimulert: Boolean,
        simuleringsResultat: SimuleringResultatDto?
    ) {
    }

    fun postVisitOppdrag(
        oppdrag: Oppdrag,
        fagområde: Fagområde,
        fagsystemId: String,
        mottaker: String,
        nettoBeløp: Int,
        tidsstempel: LocalDateTime,
        endringskode: Endringskode,
        avstemmingsnøkkel: Long?,
        status: Oppdragstatus?,
        overføringstidspunkt: LocalDateTime?,
        erSimulert: Boolean,
        simuleringsResultat: SimuleringResultatDto?
    ) {
    }
}