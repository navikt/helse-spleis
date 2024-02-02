package no.nav.helse.spleis.dao

import javax.sql.DataSource
import kotliquery.Query
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.serde.SerialisertPerson

internal class PersonDao(private val dataSource: () -> DataSource) {
    fun hentPersonFraFnr(fødselsnummer: Long) =
        hentPerson(queryOf("SELECT data FROM person WHERE id = (SELECT person_id FROM person_alias WHERE fnr=:fnr);", mapOf(
            "fnr" to fødselsnummer
        )))

    fun hentFødselsnummer(aktørId: Long) =
        sessionOf(dataSource()).use { session ->
            session.run(queryOf("SELECT fnr FROM person WHERE aktor_id = ?;", aktørId).map {
                it.long("fnr")
            }.asList)
        }.singleOrNullOrThrow()

    private fun hentPerson(query: Query) =
        sessionOf(dataSource())
            .use { session ->
                session.run(query.map { SerialisertPerson(it.string("data")) }.asList)
            }
            .singleOrNullOrThrow()
            ?.also { PostgresProbe.personLestFraDb() }

    private fun <R> Collection<R>.singleOrNullOrThrow() =
        if (size < 2) this.firstOrNull()
        else throw IllegalStateException("Listen inneholder mer enn ett element!")

}
