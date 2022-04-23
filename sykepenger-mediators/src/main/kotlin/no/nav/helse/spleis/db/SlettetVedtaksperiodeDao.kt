package no.nav.helse.spleis.db

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf

class SlettetVedtaksperiodeDao(private val dataSource: DataSource) {
    fun lagreSlettetVedtaksperiode(fødselsnummer: String, vedtaksperiodeId: UUID, vedtaksperiodeNode: JsonNode) =
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    "INSERT INTO slettet_vedtaksperiode (fodselsnummer, vedtaksperiode_id, data) VALUES(?, ?, ?::jsonb)",
                    fødselsnummer,
                    vedtaksperiodeId,
                    vedtaksperiodeNode.toString()
                ).asUpdate
            )
        }
}
