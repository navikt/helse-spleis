package no.nav.helse.spleis.db

import kotliquery.Query
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.Fødselsnummer
import no.nav.helse.serde.SerialisertPerson
import no.nav.helse.spleis.PostgresProbe
import javax.sql.DataSource

internal class PersonPostgresRepository(private val dataSource: DataSource) : PersonRepository {

    override fun hentPerson(fødselsnummer: Fødselsnummer) =
        hentPerson(queryOf("SELECT data FROM person WHERE fnr = ? ORDER BY id DESC LIMIT 1", fødselsnummer.toLong()))

    private fun hentPerson(query: Query) =
        using(sessionOf(dataSource)) { session ->
            session.run(query.map {
                SerialisertPerson(it.string("data"))
            }.asSingle)
        }?.also {
            PostgresProbe.personLestFraDb()
        }
}
