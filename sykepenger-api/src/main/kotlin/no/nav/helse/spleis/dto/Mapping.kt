package no.nav.helse.spleis.dto

import no.nav.helse.person.Person
import no.nav.helse.serde.api.dto.PersonDTO
import no.nav.helse.serde.api.serializePersonForSpeil
import no.nav.helse.spleis.dao.HendelseDao


internal fun håndterPerson(fødselsnummer: Long, person: Person, hendelseDao: HendelseDao): PersonDTO {
    val hendelser = hendelseDao.hentHendelser(fødselsnummer)
    return serializePersonForSpeil(person, hendelser)
}