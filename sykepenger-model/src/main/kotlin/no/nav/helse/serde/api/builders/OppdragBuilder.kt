package no.nav.helse.serde.api.builders

import no.nav.helse.hendelser.Simulering
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import java.time.LocalDateTime

internal class OppdragBuilder : BuilderState() {
    private lateinit var fagsystemId: String

    internal fun fagsystemId() = fagsystemId

    override fun preVisitOppdrag(
        oppdrag: Oppdrag,
        fagsystemId: String,
        totalBeløp: Int,
        nettoBeløp: Int,
        tidsstempel: LocalDateTime,
        endringskode: Endringskode,
        avstemmingsnøkkel: Long?,
        status: Oppdragstatus?,
        overføringstidspunkt: LocalDateTime?,
        simuleringsResultat: Simulering.SimuleringResultat?
    ) {
        this.fagsystemId = fagsystemId
    }

    override fun postVisitOppdrag(
        oppdrag: Oppdrag,
        fagsystemId: String,
        totalBeløp: Int,
        nettoBeløp: Int,
        tidsstempel: LocalDateTime,
        endringskode: Endringskode,
        avstemmingsnøkkel: Long?,
        status: Oppdragstatus?,
        overføringstidspunkt: LocalDateTime?,
        simuleringsResultat: Simulering.SimuleringResultat?
    ) {
        popState()
    }
}
