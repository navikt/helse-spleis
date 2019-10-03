package no.nav.helse.sakskompleks.domain

import no.nav.helse.Event
import java.util.*

interface SakskompleksObserver {
    data class StateChangeEvent(val id: UUID,
                                val aktørId: String,
                                val currentState: Sakskompleks.TilstandType,
                                val previousState: Sakskompleks.TilstandType,
                                val eventType: Event.Type,
                                val currentMemento: Sakskompleks.Memento,
                                val previousMemento: Sakskompleks.Memento) {
    }

    fun sakskompleksChanged(event: StateChangeEvent)
}
