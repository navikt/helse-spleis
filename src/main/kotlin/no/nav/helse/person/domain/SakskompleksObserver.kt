package no.nav.helse.person.domain

import no.nav.helse.hendelse.Sykdomshendelse
import java.util.*

interface SakskompleksObserver {
    data class StateChangeEvent(val id: UUID,
                                val aktørId: String,
                                val currentState: Sakskompleks.TilstandType,
                                val previousState: Sakskompleks.TilstandType,
                                val sykdomshendelse: Sykdomshendelse,
                                val currentMemento: Sakskompleks.Memento,
                                val previousMemento: Sakskompleks.Memento)

    enum class NeedType {
        TRENGER_SYKEPENGEHISTORIKK
    }

    data class NeedEvent(val sakskompleksId: UUID,
                         val aktørId: String,
                         val organisasjonsnummer: String,
                         val type: NeedType)

    fun sakskompleksChanged(event: StateChangeEvent) {}
    fun sakskompleksHasNeed(event: NeedEvent) {}
}
