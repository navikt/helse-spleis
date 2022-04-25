package no.nav.helse.spleis.db

import java.util.UUID
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf

class EndretVedtaksperiodeDao(private val dataSource: DataSource) {
    fun lagreEndretVedtaksperiode(fødselsnummer: String, vedtaksperiodeId: UUID, gammelTilstand: String, nyTilstand: String) =
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    "INSERT INTO endret_vedtaksperiode (fodselsnummer, vedtaksperiode_id, gammel_tilstand, ny_tilstand) VALUES(?, ?, ?, ?)",
                    fødselsnummer,
                    vedtaksperiodeId,
                    gammelTilstand,
                    nyTilstand
                ).asUpdate
            )
        }
}