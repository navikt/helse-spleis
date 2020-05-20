package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.PersonHendelse

class RollbackDelete(
    private val aktørId: String,
    private val fødselsnummer: String,
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : PersonHendelse(aktivitetslogg) {

    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
}

class Rollback(
    private val aktørId: String,
    private val fødselsnummer: String,
    private val personVersjon: Long,
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : PersonHendelse(aktivitetslogg) {

    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    fun personVersjon() = personVersjon
}
