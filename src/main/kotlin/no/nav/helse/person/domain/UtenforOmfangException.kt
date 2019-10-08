package no.nav.helse.person.domain

class UtenforOmfangException(message: String, private val event: Sykdomshendelse) : RuntimeException(message)
