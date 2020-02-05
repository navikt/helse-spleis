package no.nav.helse.spleis.db

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.person.Person
import no.nav.helse.serde.parsePerson
import no.nav.helse.spleis.PostgresProbe
import javax.sql.DataSource

private const val REQUIRED_SCHEMA_VERSION: Int = 4

class PersonPostgresRepository(
    private val dataSource: DataSource,
    private val probe: PostgresProbe = PostgresProbe
) : PersonRepository {

    override fun hentPerson(aktørId: String): Person? {
        return using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT data FROM person WHERE aktor_id = ? AND skjema_versjon >= ? ORDER BY id DESC LIMIT 1",
                    aktørId,
                    REQUIRED_SCHEMA_VERSION
                ).map {
                    it.string("data")
                }.asSingle
            )
        }?.let {
            parsePerson(it)
        }?.also {
            probe.personLestFraDb()
        }
    }

}
