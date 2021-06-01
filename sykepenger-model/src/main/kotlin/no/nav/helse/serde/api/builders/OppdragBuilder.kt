package no.nav.helse.serde.api.builders

import no.nav.helse.utbetalingslinjer.Oppdrag

internal class OppdragBuilder: BuilderState() {
    private lateinit var fagsystemId: String

    internal fun build() = fagsystemId

    override fun preVisitArbeidsgiverOppdrag(oppdrag: Oppdrag) {
        fagsystemId = oppdrag.fagsystemId()
    }

    override fun postVisitArbeidsgiverOppdrag(oppdrag: Oppdrag) {
        popState()
    }
}
