package no.nav.helse.spleis.e2e

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.flywaydb.core.Flyway
import org.intellij.lang.annotations.Language
import javax.sql.DataSource

internal object PostgresDatabase {

    private var state: DBState = NotStarted
    private var embeddedPostgres: EmbeddedPostgres? = null
    private var dataSource: DataSource? = null

    fun start(): PostgresDatabase {
        state.start(this)
        return this
    }

    fun reset() {
        state.reset(this)
    }

    fun connection() = state.connection(this)

    private fun stop(): PostgresDatabase {
        state.stop(this)
        return this
    }

    private fun startDatbase() {
        embeddedPostgres = EmbeddedPostgres.builder().start()
        val hikariConfig = createHikariConfig(embeddedPostgres!!.getJdbcUrl("postgres", "postgres"))
        dataSource = HikariDataSource(hikariConfig)
        createSchema(connection())
        Runtime.getRuntime().addShutdownHook(Thread(this::stop))
    }

    private fun createSchema(dataSource: DataSource) {
        Flyway.configure().dataSource(dataSource).load().migrate()
        using(sessionOf(dataSource)) { it.run(queryOf(truncateTablesSql).asExecute) }
    }

    private fun resetSchema() {
        using(sessionOf(connection())) { it.run(queryOf("SELECT truncate_tables();").asExecute) }
    }

    private fun stopDatabase() {
        embeddedPostgres!!.close()
        embeddedPostgres = null
    }

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
            state = Started
            db.startDatbase()
        }
    }

    private object Started : DBState {
        override fun stop(db: PostgresDatabase) {
            db.state = NotStarted
            db.stopDatabase()
        }

        override fun connection(db: PostgresDatabase): DataSource {
            return db.dataSource!!
        }

        override fun reset(db: PostgresDatabase) {
            db.resetSchema()
        }

    }

    @Language("PostgreSQL")
    private val truncateTablesSql = """
CREATE OR REPLACE FUNCTION truncate_tables() RETURNS void AS ${'$'}${'$'}
DECLARE
    statements CURSOR FOR
        SELECT tablename FROM pg_tables
        WHERE schemaname = 'public' AND tablename NOT LIKE 'flyway%';
BEGIN
    FOR stmt IN statements LOOP
        EXECUTE 'TRUNCATE TABLE ' || quote_ident(stmt.tablename) || ' CASCADE;';
    END LOOP;
END;
${'$'}${'$'} LANGUAGE plpgsql;
"""
}
