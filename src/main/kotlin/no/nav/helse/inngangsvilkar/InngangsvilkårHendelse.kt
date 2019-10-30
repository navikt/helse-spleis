package no.nav.helse.inngangsvilkar

import no.nav.helse.behov.Behov
import no.nav.helse.hendelse.SakskompleksHendelse
import no.nav.helse.person.domain.PersonHendelse

class InngangsvilkårHendelse(private val behov: Behov): PersonHendelse, SakskompleksHendelse {

    override fun aktørId(): String {
        return behov["aktørId"]!!
    }

    override fun organisasjonsnummer(): String? {
        return behov["organisasjonsnummer"]
    }

    override fun sakskompleksId(): String {
        return behov["sakskompleksId"]!!
    }
}
