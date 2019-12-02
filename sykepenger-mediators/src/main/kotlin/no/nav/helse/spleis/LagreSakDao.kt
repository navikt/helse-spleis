package no.nav.helse.spleis

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.sak.Sak
import no.nav.helse.sak.SakObserver
import javax.sql.DataSource

class LagreSakDao(private val dataSource: DataSource,
                  private val probe: PostgresProbe = PostgresProbe): SakObserver {

    override fun sakEndret(sakEndretEvent: SakObserver.SakEndretEvent) {
        lagreSak(sakEndretEvent.aktørId, sakEndretEvent.memento)
    }

    private fun lagreSak(aktørId: String, memento: Sak.Memento) {
        using(sessionOf(dataSource)) { session ->
            session.run(queryOf("INSERT INTO person (aktor_id, data) VALUES (?, (to_json(?::json)))", aktørId, memento.state()).asExecute)
        }.also {
            probe.sakSkrevetTilDb()
        }
    }

}
