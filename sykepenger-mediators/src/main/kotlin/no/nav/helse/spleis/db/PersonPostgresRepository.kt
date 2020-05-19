package no.nav.helse.spleis.db

import kotliquery.Query
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.serde.SerialisertPerson
import no.nav.helse.spleis.PostgresProbe
import java.util.*
import javax.sql.DataSource

internal class PersonPostgresRepository(private val dataSource: DataSource) : PersonRepository {

    override fun hentPerson(fødselsnummer: String) =
        hentPerson(queryOf("SELECT data FROM person WHERE fnr = ? ORDER BY id DESC LIMIT 1", fødselsnummer))

    override fun hentVedtaksperiodeIder(personId: Long) =
        using(sessionOf(dataSource)) { session ->
            session.run(queryOf(
                """
                    SELECT DISTINCT vedtaksperiode ->> 'id' AS vedtaksperiode_id
                    FROM person,
                         json_array_elements(data -> 'arbeidsgivere') arbeidsgiver,
                         json_array_elements(arbeidsgiver -> 'vedtaksperioder') vedtaksperiode
                    WHERE id=?; """, personId
            ).map { UUID.fromString(it.string("vedtaksperiode_id")) }.asList)
        }

    override fun hentNyestePersonId(fødselsnummer: String) =
        using(sessionOf(dataSource)) { session ->
            session.run(queryOf("""SELECT max(id) AS id FROM person WHERE fnr=?;""", fødselsnummer)
                .map { it.long("id") }.asSingle)
        }

    override fun hentPerson(id: Long) =
        requireNotNull(hentPerson(queryOf("SELECT data FROM person WHERE id = ?;", id)))

    private fun hentPerson(query: Query) =
        using(sessionOf(dataSource)) { session ->
            session.run(query.map {
                SerialisertPerson(it.string("data"))
            }.asSingle)
        }?.deserialize()?.also {
            PostgresProbe.personLestFraDb()
        }
}
