package no.nav.helse.spleis

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import java.util.*
import javax.sql.DataSource

class UtbetalingsreferansePostgresRepository(private val dataSource: DataSource,
                                             private val probe: PostgresProbe = PostgresProbe) : UtbetalingsreferanseRepository {

    override fun hentUtbetaling(utbetalingsreferanse: String): Utbetalingsreferanse? {
        return using(sessionOf(dataSource)) { session ->
            session.run(queryOf("SELECT id, aktor_id, orgnr, sakskompleks_id FROM utbetalingsreferanse WHERE id = ? LIMIT 1", utbetalingsreferanse).map {
                Utbetalingsreferanse(
                        id = it.string("id"),
                        aktørId = it.string("aktor_id"),
                        orgnummer = it.string("orgnr"),
                        sakskompleksId = UUID.fromString(it.string("sakskompleks_id"))
                )
            }.asSingle)
        }?.also {
            probe.utbetalingLestFraDb()
        }
    }

}
