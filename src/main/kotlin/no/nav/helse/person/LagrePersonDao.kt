package no.nav.helse.person

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.person.domain.Person
import no.nav.helse.person.domain.PersonObserver
import javax.sql.DataSource

class LagrePersonDao(private val dataSource: DataSource): PersonObserver {

    override fun personEndret(personEndretEvent: PersonObserver.PersonEndretEvent) {
        lagrePerson(personEndretEvent.aktørId, personEndretEvent.memento)
    }

    private fun lagrePerson(aktørId: String, memento: Person.Memento) {
        using(sessionOf(dataSource)) { session ->
            session.run(queryOf("INSERT INTO person (aktor_id, data) VALUES (?, (to_json(?::json)))", aktørId, memento.toString()).asExecute)
        }
    }

}
