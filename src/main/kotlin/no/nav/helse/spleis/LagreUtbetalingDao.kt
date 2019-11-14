package no.nav.helse.spleis

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.SakskompleksObserver
import java.util.*
import javax.sql.DataSource

class LagreUtbetalingDao(private val dataSource: DataSource,
                         private val probe: PostgresProbe = PostgresProbe): PersonObserver {

    override fun personEndret(personEndretEvent: PersonObserver.PersonEndretEvent) {
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
