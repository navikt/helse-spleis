package no.nav.helse.person

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.person.domain.Person
import javax.sql.DataSource

class PersonPostgresRepository(private val dataSource: DataSource) : PersonRepository {

    override fun hentPerson(aktørId: String): Person? = hentPersonJson(aktørId)?.let { Person.fromJson(it) }

    override fun hentPersonJson(aktørId: String): String? {
        return using(sessionOf(dataSource)) { session ->
            session.run(queryOf("SELECT data FROM person WHERE aktor_id = ? ORDER BY id DESC LIMIT 1", aktørId).map {
                it.string("data")
            }.asSingle)
        }
    }

}
