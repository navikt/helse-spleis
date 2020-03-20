package no.nav.helse.spleis.db

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.spleis.PostgresProbe
import java.util.*
import javax.sql.DataSource

class LagreUtbetalingDao(private val dataSource: DataSource) {

    fun lagreUtbetaling(utbetalingsreferanse: String, aktørId: String, organisasjonsnummer: String, vedtaksperiodeId: UUID) {
        using(sessionOf(dataSource)) { session ->
            session.run(queryOf("INSERT INTO utbetalingsreferanse (id, aktor_id, orgnr, vedtaksperiode_id) VALUES (?, ?, ?, ?)",
                    utbetalingsreferanse, aktørId, organisasjonsnummer, vedtaksperiodeId.toString()).asExecute)
        }.also {
            PostgresProbe.utbetalingSkrevetTilDb()
        }
    }

}
