package no.nav.helse.spleis.db

import no.nav.helse.Fødselsnummer
import no.nav.helse.serde.SerialisertPerson

internal interface PersonRepository {
    fun hentPerson(fødselsnummer: Fødselsnummer): SerialisertPerson?
}
