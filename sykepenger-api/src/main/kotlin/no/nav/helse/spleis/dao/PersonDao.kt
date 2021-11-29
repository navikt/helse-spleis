package no.nav.helse.spleis.dao

import kotliquery.Query
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.serde.SerialisertPerson
import javax.sql.DataSource

internal class PersonDao(private val dataSource: DataSource) {
    fun hentPersonFraFnr(fødselsnummer: Long) =
        hentPerson(queryOf("SELECT data FROM person WHERE fnr = ? ORDER BY opprettet DESC LIMIT 1", fødselsnummer))

    fun hentFødselsnummer(aktørId: Long) =
        sessionOf(dataSource).use { session ->
            session.run(queryOf("SELECT fnr FROM person WHERE aktor_id = ? ORDER BY opprettet DESC LIMIT 1;", aktørId)
                .map { it.long("fnr") }
                .asSingle)
        }

    private fun hentPerson(query: Query) =
        sessionOf(dataSource).use { session ->
            session.run(query.map {
                SerialisertPerson(it.string("data"))
            }.asSingle)
        }?.also {
            PostgresProbe.personLestFraDb()
        }

}
