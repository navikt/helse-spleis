package no.nav.helse.spleis.db

import no.nav.helse.person.Person

internal interface PersonRepository {

    fun hentPerson(fødselsnummer: String): Person?
    @Deprecated("Oppslag på aktørId skal bort til fordel for fnr", ReplaceWith("PersonRepository.hentPerson(fnr)"))
    fun hentPersonAktørId(aktørId: String): Person?

}
