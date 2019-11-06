package no.nav.helse.person.domain

interface ArbeidstakerHendelse {
    fun aktørId(): String
    fun organisasjonsnummer(): String
    fun kanBehandles() = true
}
