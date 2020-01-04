package no.nav.helse.person

class UtenforOmfangException(message: String, private val event: ArbeidstakerHendelse) : RuntimeException(message)
