package no.nav.helse.spleis.db

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.spleis.PostgresProbe
import no.nav.helse.spleis.hendelser.JsonMessage
import javax.sql.DataSource

internal class HendelseRecorder(
    private val dataSource: DataSource,
    private val probe: PostgresProbe = PostgresProbe
) {

    fun lagreMelding(melding: JsonMessage) {
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "INSERT INTO melding (data) VALUES ((to_json(?::json)))",
                    melding.toJson()
                ).asExecute
            )
        }.also {
            PostgresProbe.hendelseSkrevetTilDb()
        }
    }
}
