package no.nav.helse.person

import no.nav.helse.behov.Behov
import java.util.*

interface SakskompleksObserver {
    data class StateChangeEvent(val id: UUID,
                                val aktørId: String,
                                val currentState: Sakskompleks.TilstandType,
                                val previousState: Sakskompleks.TilstandType,
                                val sykdomshendelse: ArbeidstakerHendelse,
                                val currentMemento: Sakskompleks.Memento,
                                val previousMemento: Sakskompleks.Memento)

    data class UtbetalingEvent(val sakskompleksId: UUID,
                               val aktørId: String,
                               val organisasjonsnummer: String,
                               val utbetalingsreferanse: String)


    fun sakskompleksEndret(event: StateChangeEvent) {}

    fun sakskompleksTilUtbetaling(event: UtbetalingEvent) {}

    fun sakskompleksTrengerLøsning(event: Behov) {}

}
