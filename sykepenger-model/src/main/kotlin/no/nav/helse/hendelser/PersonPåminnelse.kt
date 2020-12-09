package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.PersonHendelse
import java.util.*

class PersonPåminnelse(
    meldingsreferanseId: UUID,
    private val aktørId: String,
    private val fødselsnummer: String
) : PersonHendelse(meldingsreferanseId, Aktivitetslogg()) {

    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
}
