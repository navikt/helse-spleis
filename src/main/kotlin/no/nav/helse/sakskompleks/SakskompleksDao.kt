package no.nav.helse.sakskompleks

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.sakskompleks.domain.Sakskompleks
import javax.sql.DataSource

class SakskompleksDao(private val dataSource: DataSource) {

    companion object {
        private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    fun finnSaker(brukerAktørId: String) =
            using(sessionOf(dataSource)) { session ->
                session.run(queryOf("SELECT data FROM SAKSKOMPLEKS WHERE bruker_aktor_id = ?", brukerAktørId).map { row ->
                    objectMapper.readerFor(Sakskompleks::class.java)
                            .readValue<Sakskompleks>(row.bytes("data"))
                }.asList)
            }

    fun opprettSak(sakskompleks: Sakskompleks) =
            using(sessionOf(dataSource)) { session ->
                session.run(queryOf("INSERT INTO SAKSKOMPLEKS(id, bruker_aktor_id, data) VALUES (?, ?, (to_json(?::json)))",
                        sakskompleks.id().toString(), sakskompleks.aktørId(), objectMapper.writeValueAsString(sakskompleks)).asUpdate)
            }

    fun oppdaterSak(sakskompleks: Sakskompleks) =
            using(sessionOf(dataSource)) { session ->
                session.run(queryOf("UPDATE SAKSKOMPLEKS SET data=(to_json(?::json)) WHERE id=?",
                        objectMapper.writeValueAsString(sakskompleks), sakskompleks.id().toString()).asUpdate)
            }
}
