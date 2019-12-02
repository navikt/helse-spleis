package no.nav.helse.sak

interface ArbeidstakerHendelse {
    fun aktørId(): String
    fun fødselsnummer(): String
    fun organisasjonsnummer(): String
    fun kanBehandles() = true
}
