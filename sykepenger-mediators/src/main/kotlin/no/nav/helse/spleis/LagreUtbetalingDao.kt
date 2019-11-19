package no.nav.helse.spleis

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.sak.SakObserver
import no.nav.helse.sak.SakskompleksObserver
import java.util.*
import javax.sql.DataSource

class LagreUtbetalingDao(private val dataSource: DataSource,
                         private val probe: PostgresProbe = PostgresProbe): SakObserver {

    override fun sakEndret(sakEndretEvent: SakObserver.SakEndretEvent) {
    }

    override fun sakskompleksTilUtbetaling(event: SakskompleksObserver.UtbetalingEvent) {
        lagreUtbetaling(event.utbetalingsreferanse, event.aktørId, event.organisasjonsnummer, event.sakskompleksId)
    }

    private fun lagreUtbetaling(utbetalingsreferanse: String, aktørId: String, organisasjonsnummer: String, sakskompleksId: UUID) {
        using(sessionOf(dataSource)) { session ->
            session.run(queryOf("INSERT INTO utbetalingsreferanse (id, aktor_id, orgnr, sakskompleks_id) VALUES (?, ?, ?, ?)",
                    utbetalingsreferanse, aktørId, organisasjonsnummer, sakskompleksId.toString()).asExecute)
        }.also {
            probe.utbetalingSkrevetTilDb()
        }
    }

}
