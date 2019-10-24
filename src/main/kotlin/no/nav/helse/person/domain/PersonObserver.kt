package no.nav.helse.person.domain

import no.nav.helse.hendelse.SykdomstidslinjeHendelse

interface PersonObserver : SakskompleksObserver {
    data class PersonEndretEvent(val aktørId: String,
                                 val sykdomshendelse: SykdomstidslinjeHendelse,
                                 val memento: Person.Memento)

    fun personEndret(personEndretEvent: PersonEndretEvent)
}
