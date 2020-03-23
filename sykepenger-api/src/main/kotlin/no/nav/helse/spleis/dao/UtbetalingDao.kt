package no.nav.helse.spleis.dao

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import java.util.*
import javax.sql.DataSource

internal class UtbetalingDao(private val dataSource: DataSource) {
    fun hentUtbetaling(utbetalingsreferanse: String): Utbetalingsreferanse? {
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

    class Utbetalingsreferanse(internal val id: String,
                               internal val aktørId: String,
                               internal val orgnummer: String,
                               internal val vedtaksperiodeId: UUID)
}
