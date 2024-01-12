package no.nav.helse.spleis

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer

internal class DB {
    companion object {
        internal val instance = PostgreSQLContainer<Nothing>("postgres:15").apply {
            withCreateContainerCmdModifier { command -> command.withName("spleis-api") }
            withReuse(true)
            withLabel("app-navn", "spleis-api")
            start()
        }
        fun clean() = Flyway.configure().dataSource(createDataSource()).cleanDisabled(false).load().clean()
        internal fun migrate(): DataSource {
            val dataSource = createDataSource()
            Flyway.configure()
                .cleanDisabled(false)
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .also { it.clean() }
                .migrate()
            return dataSource
        }
        private fun createDataSource() =
            HikariDataSource(HikariConfig().apply {
                this.jdbcUrl = instance.jdbcUrl
                this.username = instance.username
                this.password = instance.password
                maximumPoolSize = 3
                minimumIdle = 1
                idleTimeout = 10001
                connectionTimeout = 1000
                initializationFailTimeout = 5000
                maxLifetime = 30001
            })
    }
}
