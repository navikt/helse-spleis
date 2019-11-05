package no.nav.helse.person.domain

interface PersonHendelse {
    fun aktørId(): String
    fun organisasjonsnummer(): String?
    fun kanBehandles() = true
}
