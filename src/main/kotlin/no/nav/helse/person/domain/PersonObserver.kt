package no.nav.helse.person.domain

import no.nav.helse.hendelse.PersonHendelse

interface PersonObserver : SakskompleksObserver {
    data class PersonEndretEvent(val aktørId: String,
                                 val sykdomshendelse: PersonHendelse,
                                 val memento: Person.Memento)

    fun personEndret(personEndretEvent: PersonEndretEvent)
}
