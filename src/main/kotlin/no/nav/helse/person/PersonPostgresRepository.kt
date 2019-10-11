package no.nav.helse.person

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.person.domain.Person
import javax.sql.DataSource

class PersonPostgresRepository(private val dataSource: DataSource) : PersonRepository {


    override fun hentPerson(aktørId: String): Person? {
        return using(sessionOf(dataSource)) { session ->
            session.run(queryOf("SELECT data FROM person WHERE aktor_id = ? ORDER BY id DESC LIMIT 1", aktørId).map {
                Person.fromJson(it.string("data"))
            }.asSingle)
        }
    }

    override fun lagrePerson(person: Person) {
        using(sessionOf(dataSource)) { session ->
            session.run(queryOf("INSERT INTO person (aktor_id, data) VALUES (?, (to_json(?::json)))", person.aktørId, person.toJson()).asExecute)
        }
    }


}
