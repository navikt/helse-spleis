package no.nav.helse.spleis.dao

import javax.sql.DataSource
import kotliquery.Query
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.serde.SerialisertPerson

internal class PersonDao(private val dataSource: DataSource) {
    fun hentPersonFraFnr(fødselsnummer: Long) =
        hentPerson(queryOf("SELECT data FROM person WHERE fnr = ? ORDER BY id DESC LIMIT 1", fødselsnummer))

    fun hentFødselsnummer(aktørId: Long) =
        sessionOf(dataSource).use { session ->
            session.run(queryOf("SELECT fnr FROM unike_person WHERE aktor_id = ? LIMIT 1;", aktørId)
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
