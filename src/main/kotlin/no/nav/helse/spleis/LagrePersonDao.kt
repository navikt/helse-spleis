package no.nav.helse.spleis

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.person.Person
import no.nav.helse.person.PersonObserver
import javax.sql.DataSource

class LagrePersonDao(private val dataSource: DataSource,
                     private val probe: PostgresProbe = PostgresProbe): PersonObserver {

    override fun personEndret(personEndretEvent: PersonObserver.PersonEndretEvent) {
        lagrePerson(personEndretEvent.aktørId, personEndretEvent.memento)
    }

    private fun lagrePerson(aktørId: String, memento: Person.Memento) {
        using(sessionOf(dataSource)) { session ->
            session.run(queryOf("INSERT INTO person (aktor_id, data) VALUES (?, (to_json(?::json)))", aktørId, memento.toString()).asExecute)
        }.also {
            PostgresProbe.personSkrevetTilDb()
        }
    }

}
