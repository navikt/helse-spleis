package no.nav.helse.person.domain

import no.nav.helse.hendelse.DokumentMottattHendelse

interface PersonObserver : SakskompleksObserver {
    data class PersonEndretEvent(val aktørId: String,
                                 val sykdomshendelse: DokumentMottattHendelse,
                                 val memento: Person.Memento)

    fun personEndret(personEndretEvent: PersonEndretEvent)
}
