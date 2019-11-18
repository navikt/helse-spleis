package no.nav.helse.person

interface ArbeidstakerHendelse {
    fun aktørId(): String
    fun organisasjonsnummer(): String
    fun kanBehandles() = true
}
