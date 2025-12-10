package no.nav.helse.dbscript

import com.github.navikt.tbd_libs.sql_dsl.transaction
import com.github.navikt.tbd_libs.sql_dsl.connection
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import kotlin.use

internal abstract class DbScript {
    abstract val beskrivelse: String

    abstract fun start(connectionInfo: ConnectionInfo)

    data class ConnectionInfo(val jdbcUrl: String, val epost: Input.Epost)

    private fun dataSource(connectionInfo: ConnectionInfo) = try {
        HikariDataSource(HikariConfig().apply {
            this.maximumPoolSize = 1
            this.jdbcUrl = connectionInfo.jdbcUrl
        })
    } catch (feil: Exception) {
        throw IllegalArgumentException("❌ Klarte ikke koble opp mot databasen. Har du startet proxyen?", feil)
    }

    protected fun databaseTransaksjon(connectionInfo: ConnectionInfo, block: Connection.() -> Unit) {
        dataSource(connectionInfo).connection {
            transaction {
                block(this)
            }
        }
    }

    protected fun Connection.audit(fødselsnummer: Input.Fødselsnummer, epost: Input.Epost, diff: String, beskrivelse: Input.Beskrivelse) {
        check(!autoCommit) { "Hei, hva søren driver du med, dette må skje i en transaction!"}
        check(1 == prepareStatement("INSERT INTO auditlog (personidentifikator, epost, diff, beskrivelse) VALUES (?,?,?,?)").use { stmt ->
            stmt.setString(1, fødselsnummer.verdi)
            stmt.setString(2, epost.verdi)
            stmt.setString(3, diff)
            stmt.setString(4, beskrivelse.verdi)
            stmt.executeUpdate()
        }) { "forventet å oppdatere nøyaktig én rad ved auditlogging" }
    }
}
