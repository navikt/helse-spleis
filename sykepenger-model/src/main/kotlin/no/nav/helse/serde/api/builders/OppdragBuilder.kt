package no.nav.helse.serde.api.builders

import no.nav.helse.utbetalingslinjer.Oppdrag

internal class OppdragBuilder : BuilderState() {
    private lateinit var arbeidsgiverFagsystemId: String
    private lateinit var personFagsystemId: String

    internal fun arbeidsgiverFagsystemId() = arbeidsgiverFagsystemId

    internal fun personFagsystemId() = personFagsystemId

    override fun preVisitArbeidsgiverOppdrag(oppdrag: Oppdrag) {
        arbeidsgiverFagsystemId = oppdrag.fagsystemId()
    }

    override fun preVisitPersonOppdrag(oppdrag: Oppdrag) {
        personFagsystemId = oppdrag.fagsystemId()
    }

    override fun postVisitPersonOppdrag(oppdrag: Oppdrag) {
        popState()
    }
}
