package no.nav.helse.person.domain

import no.nav.helse.hendelse.PersonHendelse

class UtenforOmfangException(message: String, private val event: PersonHendelse) : RuntimeException(message)
