package no.nav.helse.dbscript

import com.github.navikt.tbd_libs.sql_dsl.transaction
import com.github.navikt.tbd_libs.sql_dsl.connection
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection

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

    protected fun gaal(begrunnelse: String) {
        println("""
            Nå kan du gå inn på https://audit-approval.iap.nav.cloud.nais.io/?team=tbd&timeRange=24h&database=spleis&status=new
            
            Legg inn følgende begrunnelse: 
            
            $begrunnelse
        """)
    }
}
