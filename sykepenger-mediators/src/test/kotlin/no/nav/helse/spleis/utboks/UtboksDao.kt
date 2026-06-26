package no.nav.helse.spleis.utboks

import com.github.navikt.tbd_libs.sql_dsl.connection
import com.github.navikt.tbd_libs.sql_dsl.mapNotNull
import com.github.navikt.tbd_libs.sql_dsl.prepareStatementWithNamedParameters
import com.github.navikt.tbd_libs.sql_dsl.string
import com.github.navikt.tbd_libs.sql_dsl.stringOrNull
import java.sql.Connection
import java.sql.ResultSet
import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.Personidentifikator
import org.intellij.lang.annotations.Language

internal class UtboksDao(private val dataSource: DataSource) {

    /**
     * Lagrer ned alle meldingene i samme database-transaksjon som personen lagres ned
     */
    fun lagre(connection: Connection, meldinger: List<UtgåendeMelding>, forårsaketAv: UUID) {
        check(!connection.autoCommit) { "lagre må kalles innenfor transaksjonen som lagrer person" }

        @Language("PostgreSQL")
        val sql = """
            INSERT INTO utboks (id, forarsaket_av, key, json, mottaker)
            SELECT * FROM unnest(
                :id::uuid[],
                :forarsaket_av::uuid[],
                :key::text[],
                :json::jsonb[],
                :mottaker::text[]
            );
        """

        connection.prepareStatementWithNamedParameters(sql) {
            withParameter("id", meldinger.map { it.id })
            withParameter("forarsaket_av", List(meldinger.size) { forårsaketAv })
            withParameter("key", meldinger.map { it.key })
            withParameter("json", meldinger.map { it.json.toString() })
            withParameter("mottaker", meldinger.map { it.mottaker.name })
        }.execute()
    }

    /**
     * Henter alle usendte meldinger som er for den spesifikke personidentifikatoren,
     * eller som ikke har noen personidentifikator (key is null)
     */
    fun usendte(personidentifikator: Personidentifikator): List<UtgåendeMelding> {
        @Language("PostgreSQL")
        val sql = """
            SELECT * FROM utboks where sendt is null AND (key = :key OR key is null) FOR UPDATE SKIP LOCKED;
        """

        return dataSource.connection {
            prepareStatementWithNamedParameters(sql) {
                withParameter("key", personidentifikator.toString())
            }.mapNotNull { row ->
                row.tilUtgåendeMelding()
            }
        }
    }

    /**
     * Markerer alle meldinger som er sendt OK som sendt i databasen.
     */
    fun sendt(kvittering: Kvittering) {
        @Language("PostgreSQL")
        val sql = """
            UPDATE utboks SET sendt = :sendt WHERE id = ANY(:ider)
        """

        return dataSource.connection {
            prepareStatementWithNamedParameters(sql) {
                withParameter("sendt", kvittering.sendt)
                withParameter("ider", kvittering.ok.map { it.id })
            }.executeUpdate()
        }
    }

    /**
     * Henter alle personidentifikatorer som har usendte meldinger i utboksen
     */
    fun personerMedUsendteMeldinger(): Set<Personidentifikator> {
        @Language("PostgreSQL")
        val sql = """
            SELECT distinct key FROM utboks where sendt is null
        """
        return dataSource.connection {
            prepareStatement(sql).mapNotNull { row ->
                row.stringOrNull("key")?.let { Personidentifikator(it) }
            }.toSet()
        }
    }

    private companion object {
        fun ResultSet.tilUtgåendeMelding() = UtgåendeMelding(
            key = stringOrNull("key"),
            json = string("json"),
            mottaker = when (val mottaker = string("mottaker")) {
                "RAPID" -> UtgåendeMelding.Mottaker.RAPID
                "SUBSUMSJON" -> UtgåendeMelding.Mottaker.SUBSUMSJON
                else -> error("Mottaker $mottaker har jeg aldri hørt om, den må du eventuelt legge inn.")
            }
        )
    }
}
