package no.nav.helse.spleis

import no.nav.helse.person.Person

internal class PersonRestInterface(
    private val personRepository: PersonRepository,
    private val utbetalingsreferanseRepository: UtbetalingsreferanseRepository
) {

    fun hentSak(aktørId: String): Person? = personRepository.hentPerson(aktørId)

    fun hentSakForUtbetaling(utbetalingsreferanse: String): Person? {
        return utbetalingsreferanseRepository.hentUtbetaling(utbetalingsreferanse)?.let {
            personRepository.hentPerson(it.aktørId)
        }
    }
}
