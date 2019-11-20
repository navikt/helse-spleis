package no.nav.helse.spleis

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.sak.SakObserver
import no.nav.helse.sak.VedtaksperiodeObserver
import java.util.*
import javax.sql.DataSource

class LagreUtbetalingDao(private val dataSource: DataSource,
                         private val probe: PostgresProbe = PostgresProbe): SakObserver {

    override fun sakEndret(sakEndretEvent: SakObserver.SakEndretEvent) {
    }

    override fun vedtaksperiodeTilUtbetaling(event: VedtaksperiodeObserver.UtbetalingEvent) {
        lagreUtbetaling(event.utbetalingsreferanse, event.aktørId, event.organisasjonsnummer, event.vedtaksperiodeId)
    }

    private fun lagreUtbetaling(utbetalingsreferanse: String, aktørId: String, organisasjonsnummer: String, vedtaksperiodeId: UUID) {
        using(sessionOf(dataSource)) { session ->
            session.run(queryOf("INSERT INTO utbetalingsreferanse (id, aktor_id, orgnr, sakskompleks_id) VALUES (?, ?, ?, ?)",
                    utbetalingsreferanse, aktørId, organisasjonsnummer, vedtaksperiodeId.toString()).asExecute)
        }.also {
            probe.utbetalingSkrevetTilDb()
        }
    }

}
