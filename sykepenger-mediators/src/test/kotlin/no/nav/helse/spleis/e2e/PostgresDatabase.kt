package no.nav.helse.spleis.e2e

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import javax.sql.DataSource

internal object PostgresDatabase {

    private var state: DBState = NotStarted
    private var builder = EmbeddedPostgres.builder()
    private var embeddedPostgres: EmbeddedPostgres? = null
    private var hikariConfig: HikariConfig? = null

    fun start(): PostgresDatabase {
        state.start(this)
        return this
    }

    fun stop(): PostgresDatabase {
        state.stop(this)
        return this
    }

    fun reset() {
        state.reset(this)
    }
    fun connection() = state.connection(this)

    private fun createHikariConfig(jdbcUrl: String) =
        HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            maximumPoolSize = 3
            minimumIdle = 1
            idleTimeout = 10001
            connectionTimeout = 1000
            maxLifetime = 30001
        }

    private interface DBState {
        fun connection(db: PostgresDatabase): DataSource {
            throw IllegalStateException("Cannot create connection in state ${this::class.simpleName}")
        }
        fun start(db: PostgresDatabase) {}
        fun stop(db: PostgresDatabase) {}
        fun reset(db: PostgresDatabase) {}
    }

    private object NotStarted : DBState {
        override fun start(db: PostgresDatabase) {
            db.state = Started
            db.embeddedPostgres = db.builder.start()
            db.hikariConfig = createHikariConfig(embeddedPostgres!!.getJdbcUrl("postgres", "postgres"))
        }
    }

    private object Started : DBState {
        override fun stop(db: PostgresDatabase) {
            db.state = NotStarted
            db.hikariConfig = null
            db.embeddedPostgres!!.close()
            db.embeddedPostgres = null
        }

        override fun connection(db: PostgresDatabase): DataSource {
            return HikariDataSource(db.hikariConfig)
        }

        override fun reset(db: PostgresDatabase) {
            Flyway.configure().dataSource(connection(db)).load().also {
                it.clean()
                it.migrate()
            }
        }
    }
}
