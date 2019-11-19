package no.nav.helse.sak

class UtenforOmfangException(message: String, private val event: ArbeidstakerHendelse) : RuntimeException(message)
