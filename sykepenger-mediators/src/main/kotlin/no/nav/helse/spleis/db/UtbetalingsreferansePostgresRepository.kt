package no.nav.helse.spleis.db

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.spleis.PostgresProbe
import java.util.*
import javax.sql.DataSource

class UtbetalingsreferansePostgresRepository(private val dataSource: DataSource) : UtbetalingsreferanseRepository {

    override fun hentUtbetaling(utbetalingsreferanse: String): Utbetalingsreferanse? {
        return using(sessionOf(dataSource)) { session ->
            session.run(queryOf("SELECT id, aktor_id, orgnr, vedtaksperiode_id FROM utbetalingsreferanse WHERE id = ? LIMIT 1", utbetalingsreferanse).map {
                Utbetalingsreferanse(
                    id = it.string("id"),
                    aktørId = it.string("aktor_id"),
                    orgnummer = it.string("orgnr"),
                    vedtaksperiodeId = UUID.fromString(it.string("vedtaksperiode_id"))
                )
            }.asSingle)
        }?.also {
            PostgresProbe.utbetalingLestFraDb()
        }
    }

}
