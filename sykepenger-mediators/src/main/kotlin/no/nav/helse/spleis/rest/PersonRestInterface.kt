package no.nav.helse.spleis.rest

import no.nav.helse.person.Person
import no.nav.helse.spleis.db.PersonRepository
import no.nav.helse.spleis.db.UtbetalingsreferanseRepository

internal class PersonRestInterface(
    private val personRepository: PersonRepository,
    private val utbetalingsreferanseRepository: UtbetalingsreferanseRepository
) {

    fun hentSak(aktørId: String): Person? = personRepository.hentPersonAktørId(aktørId)

    fun hentSakForUtbetaling(utbetalingsreferanse: String): Person? {
        return utbetalingsreferanseRepository.hentUtbetaling(utbetalingsreferanse)?.let {
            personRepository.hentPersonAktørId(it.aktørId)
        }
    }
}
