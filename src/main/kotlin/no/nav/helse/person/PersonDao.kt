package no.nav.helse.person

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.person.domain.Person
import no.nav.helse.person.domain.PersonObserver
import no.nav.helse.person.domain.SakskompleksObserver.StateChangeEvent
import javax.sql.DataSource

class PersonDao(private val dataSource: DataSource) : PersonObserver {
    fun finnEllerOpprettPerson(aktørId: String): Person = (finnPerson(aktørId)
            ?: opprettPerson(aktørId)).also { person ->
        person.addObserver(this)
    }

    private fun opprettPerson(aktørId: String): Person {
        val person = Person(aktørId = aktørId)

        using(sessionOf(dataSource)) { session ->
            session.run(queryOf("INSERT INTO person (aktor_id, data) VALUES (?, (to_json(?::json)))", aktørId, person.toJson()).asUpdate)
        }
        return person
    }

    private fun finnPerson(aktørId: String): Person? {
        return using(sessionOf(dataSource)) { session ->
            session.run(queryOf("SELECT data FROM person WHERE aktor_id = ?", aktørId).map {
                Person.fromJson(it.string("data"))
            }.asSingle)
        }
    }

    fun oppdaterPerson(person: Person) {
        using(sessionOf(dataSource)) { session ->
            session.run(queryOf("UPDATE person (aktor_id, data) SET VALUES (?, (to_json(?::json))) WHERE aktor_id = ?", person.toJson(), person.aktørId).asUpdate)
        }
    }

    override fun sakskompleksChanged(event: StateChangeEvent) {}

    override fun personEndret(person: Person) {
        oppdaterPerson(person = person)
    }
}
