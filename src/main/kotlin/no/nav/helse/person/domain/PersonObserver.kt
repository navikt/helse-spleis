package no.nav.helse.person.domain

interface PersonObserver : SakskompleksObserver {
    data class PersonEndretEvent(val aktørId: String,
                                 val sykdomshendelse: PersonHendelse,
                                 val memento: Person.Memento)

    fun personEndret(personEndretEvent: PersonEndretEvent)
}
