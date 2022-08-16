package no.nav.helse.spleis.db

import no.nav.helse.Personidentifikator
import no.nav.helse.serde.SerialisertPerson

internal interface PersonRepository {
    fun hentPerson(personidentifikator: Personidentifikator): SerialisertPerson?
}
