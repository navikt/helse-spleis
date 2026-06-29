package no.nav.helse.spleis.utboks

import com.github.navikt.tbd_libs.sql_dsl.connection
import com.github.navikt.tbd_libs.sql_dsl.mapNotNull
import com.github.navikt.tbd_libs.sql_dsl.prepareStatementWithNamedParameters
import com.github.navikt.tbd_libs.sql_dsl.string
import com.github.navikt.tbd_libs.sql_dsl.stringOrNull
import com.github.navikt.tbd_libs.sql_dsl.transaction
import java.sql.Connection
import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.Personidentifikator
import org.intellij.lang.annotations.Language

internal class PostgresUtboksDao(private val dataSource: DataSource): UtboksDao {

    override fun lagre(connection: Connection, meldinger: List<UtgåendeMelding>, forårsaketAv: UUID) {
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

    override fun usendte(personidentifikator: Personidentifikator, send: (meldinger: List<UtgåendeMelding>) -> Kvittering) {
        @Language("PostgreSQL")
        val sqlHent = """
            SELECT lopenummer, key, json, mottaker FROM utboks 
            WHERE sendt IS NULL AND (key = :key OR key IS NULL) 
            ORDER BY lopenummer
            FOR UPDATE SKIP LOCKED;
        """

        @Language("PostgreSQL")
        val sqlMarkerSendt = """
            UPDATE utboks 
            SET sendt = :sendt 
            WHERE id = ANY(:ider);
        """

        dataSource.connection {
            transaction {
                val usendteMeldinger = prepareStatementWithNamedParameters(sqlHent) {
                    withParameter("key", personidentifikator.toString())
                }.mapNotNull { row ->
                    UtgåendeMelding(
                        key = row.stringOrNull("key"),
                        json = row.string("json"),
                        mottaker = when (val mottaker = row.string("mottaker")) {
                            "RAPID" -> UtgåendeMelding.Mottaker.RAPID
                            "SUBSUMSJON" -> UtgåendeMelding.Mottaker.SUBSUMSJON
                            else -> error("Mottaker $mottaker har jeg aldri hørt om, den må du eventuelt legge inn.")
                        }
                    )
                }
                val kvittering= send(usendteMeldinger)
                prepareStatementWithNamedParameters(sqlMarkerSendt) {
                    withParameter("sendt", kvittering.sendt)
                    withParameter("ider", kvittering.ok.map { it.id })
                }.executeUpdate()
            }
        }
    }

    override fun personerMedUsendteMeldinger(): Set<Personidentifikator> {
        @Language("PostgreSQL")
        val sql = """
            SELECT DISTINCT key 
            FROM utboks 
            WHERE sendt IS NULL;
        """
        return dataSource.connection {
            prepareStatement(sql).mapNotNull { row ->
                row.stringOrNull("key")?.let { Personidentifikator(it) }
            }.toSet()
        }
    }
}
