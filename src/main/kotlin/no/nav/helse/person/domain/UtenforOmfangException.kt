package no.nav.helse.person.domain

import no.nav.helse.hendelse.Sykdomshendelse

class UtenforOmfangException(message: String, private val event: Sykdomshendelse) : RuntimeException(message)
