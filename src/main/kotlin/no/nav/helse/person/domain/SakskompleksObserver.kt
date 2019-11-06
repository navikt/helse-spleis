package no.nav.helse.person.domain

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


    fun sakskompleksEndret(event: StateChangeEvent) {}

    fun sakskompleksTrengerLøsning(event: Behov) {}

}
