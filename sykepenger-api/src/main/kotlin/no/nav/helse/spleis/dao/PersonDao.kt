package no.nav.helse.spleis.dao

import kotliquery.Query
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.serde.SerialisertPerson
import javax.sql.DataSource

internal class PersonDao(private val dataSource: DataSource) {
    fun hentPersonFraFnr(fødselsnummer: String) =
        hentPerson(queryOf("SELECT data FROM person WHERE fnr = ? ORDER BY id DESC LIMIT 1", fødselsnummer.toLong()))

    fun hentPersonFraAktørId(aktørId: String) =
        hentPerson(queryOf("SELECT data FROM person WHERE aktor_id = ? ORDER BY id DESC LIMIT 1", aktørId.toLong()))

    private fun hentPerson(query: Query) =
        using(sessionOf(dataSource)) { session ->
            session.run(query.map {
                SerialisertPerson(it.string("data"))
            }.asSingle)
        }?.also {
            PostgresProbe.personLestFraDb()
        }

}
