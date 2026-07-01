package no.nav.helse.spleis.utboks

import com.github.navikt.tbd_libs.sql_dsl.connection
import com.github.navikt.tbd_libs.sql_dsl.mapNotNull
import com.github.navikt.tbd_libs.sql_dsl.prepareStatementWithNamedParameters
import com.github.navikt.tbd_libs.sql_dsl.string
import com.github.navikt.tbd_libs.sql_dsl.stringOrNull
import com.github.navikt.tbd_libs.sql_dsl.transaction
import java.sql.Connection
import java.sql.ResultSet
import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.Personidentifikator
import org.intellij.lang.annotations.Language

internal class PostgresUtboksDao(private val dataSource: DataSource): UtboksDao {

    override fun lagre(connection: Connection, meldinger: List<UtgåendeMelding>, forårsaketAv: UUID) {
        check(!connection.autoCommit) { "lagre må kalles innenfor transaksjonen som lagrer person" }

        @Language("PostgreSQL")
        val sql = """
            INSERT INTO utboks (id, forarsaket_av, key, json, mottaker, opprettet)
            SELECT * FROM unnest(
                :id::uuid[],
                :forarsaket_av::uuid[],
                :key::text[],
                :json::jsonb[],
                :mottaker::text[],
                :opprettet::timestamptz[]
            );
        """

        connection.prepareStatementWithNamedParameters(sql) {
            withParameter("id", meldinger.map { it.id })
            withParameter("forarsaket_av", List(meldinger.size) { forårsaketAv })
            withParameter("key", meldinger.map { it.key })
            withParameter("json", meldinger.map { it.json.toString() })
            withParameter("mottaker", meldinger.map { it.mottaker.name })
            withParameter("opprettet") { setArray(it, connection.createArrayOf("timestamptz", meldinger.map { melding -> melding.opprettet }.toTypedArray())) }
        }.use { it.execute() }
    }

    override fun usendte(personidentifikator: Personidentifikator, send: (meldinger: List<UtgåendeMelding>) -> Kvittering) {
        @Language("PostgreSQL")
        val sqlHentFraUtboks = """
            SELECT lopenummer, key, json, mottaker 
            FROM utboks 
            WHERE (key = :key OR key IS NULL) 
            ORDER BY lopenummer
            FOR UPDATE SKIP LOCKED;
        """

        @Language("PostgreSQL")
        val sqlFlyttTilSendt = """
            WITH deleted AS (
                DELETE FROM utboks 
                WHERE id = ANY(:ider)
                RETURNING *
            )
            INSERT INTO sendt (id, lopenummer, forarsaket_av, key, json, mottaker, opprettet, sendt)
            SELECT id, lopenummer, forarsaket_av, key, json, mottaker, opprettet, :sendt
            FROM deleted;
        """

        dataSource.connection {
            transaction {
                val usendteMeldinger = prepareStatementWithNamedParameters(sqlHentFraUtboks) {
                    withParameter("key", personidentifikator.toString())
                }.mapNotNull { row -> row.somUtgåendeMelding() }
                val kvittering = send(usendteMeldinger)
                if (kvittering.ok.isEmpty()) return@transaction
                prepareStatementWithNamedParameters(sqlFlyttTilSendt) {
                    withParameter("sendt", kvittering.sendt)
                    withParameter("ider", kvittering.ok.map { it.id })
                }.use { it.execute() }
            }
        }
    }

    override fun personerMedUsendteMeldinger(): Set<Personidentifikator> {
        @Language("PostgreSQL")
        val sql = """
            SELECT DISTINCT key 
            FROM utboks 
            WHERE key IS NOT NULL
            LIMIT 1000
        """
        return dataSource.connection {
            prepareStatement(sql).mapNotNull { row ->
                Personidentifikator(row.string("key"))
            }.toSet()
        }
    }

    internal companion object {
        internal fun ResultSet.somUtgåendeMelding() = UtgåendeMelding(
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
