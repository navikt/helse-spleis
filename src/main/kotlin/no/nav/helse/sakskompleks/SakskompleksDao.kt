package no.nav.helse.sakskompleks

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.sakskompleks.domain.Sakskompleks
import java.util.*
import javax.sql.DataSource

class SakskompleksDao(private val dataSource: DataSource) : Sakskompleks.Observer {

    fun finnSaker(brukerAktørId: String) =
            using(sessionOf(dataSource)) { session ->
                session.run(queryOf("SELECT data FROM SAKSKOMPLEKS WHERE bruker_aktor_id = ?", brukerAktørId).map { row ->
                    Sakskompleks(row.bytes("data")).also { sakskompleks ->
                        sakskompleks.addObserver(this)
                    }
                }.asList)
            }

    fun opprettSak(brukerAktørId: String) =
            Sakskompleks(
                    id = UUID.randomUUID(),
                    aktørId = brukerAktørId
            ).also { sak ->
                opprettSak(sak.lagre())
            }

    private fun opprettSak(memento: Sakskompleks.Memento) =
            using(sessionOf(dataSource)) { session ->
                session.run(queryOf("INSERT INTO SAKSKOMPLEKS(id, bruker_aktor_id, data) VALUES (?, ?, (to_json(?::json)))",
                        memento.id.toString(), memento.aktørId, memento.json).asUpdate)
            }

    private fun oppdaterSak(memento: Sakskompleks.Memento) =
            using(sessionOf(dataSource)) { session ->
                session.run(queryOf("UPDATE SAKSKOMPLEKS SET data=(to_json(?::json)) WHERE id=?",
                        memento.json, memento.id.toString()).asUpdate)
            }

    override fun stateChange(newState: Sakskompleks.Observer.State, oldState: Sakskompleks.Observer.State) {
        oppdaterSak(newState.memento)
    }
}
