package no.nav.helse.person.domain

import no.nav.helse.hendelse.DokumentMottattHendelse

class UtenforOmfangException(message: String, private val event: DokumentMottattHendelse) : RuntimeException(message)
