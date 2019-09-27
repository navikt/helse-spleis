package no.nav.helse.sakskompleks

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.sakskompleks.domain.Sakskompleks
import no.nav.helse.sakskompleks.domain.SakskompleksObserver
import no.nav.helse.sakskompleks.domain.SakskompleksObserver.StateChangeEvent
import java.util.*
import javax.sql.DataSource

class SakskompleksDao(private val dataSource: DataSource) : SakskompleksObserver {

    fun finnSaker(brukerAktørId: String) =
            using(sessionOf(dataSource)) { session ->
                session.run(queryOf("SELECT data FROM SAKSKOMPLEKS WHERE bruker_aktor_id = ?", brukerAktørId).map { row ->
                    Sakskompleks.restore(Sakskompleks.Memento(row.bytes("data"))).also { sakskompleks ->
                        sakskompleks.addObserver(this)
                    }
                }.asList)
            }

    fun opprettSak(brukerAktørId: String) =
            Sakskompleks(
                    id = UUID.randomUUID(),
                    aktørId = brukerAktørId
            ).also { sak ->
                sak.addObserver(this)
            }

    private fun opprettSak(id: UUID, brukerAktørId: String, memento: Sakskompleks.Memento) =
            using(sessionOf(dataSource)) { session ->
                session.run(queryOf("INSERT INTO SAKSKOMPLEKS(id, bruker_aktor_id, data) VALUES (?, ?, (to_json(?::json)))",
                        id.toString(), brukerAktørId, memento.toString()).asUpdate)
            }

    private fun oppdaterSak(id: UUID, memento: Sakskompleks.Memento) =
            using(sessionOf(dataSource)) { session ->
                session.run(queryOf("UPDATE SAKSKOMPLEKS SET data=(to_json(?::json)) WHERE id=?",
                        memento.toString(), id.toString()).asUpdate)
            }

    override fun sakskompleksChanged(event: StateChangeEvent) {
        when (event.previousState) {
            "StartTilstand" -> {
                opprettSak(event.id, event.aktørId, event.currentMemento)
            }
            else -> oppdaterSak(event.id, event.currentMemento)
        }
    }
}
