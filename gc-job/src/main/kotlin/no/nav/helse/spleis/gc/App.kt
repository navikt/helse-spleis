package no.nav.helse.spleis.gc

import com.zaxxer.hikari.HikariConfig
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import org.slf4j.LoggerFactory
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

private val log = LoggerFactory.getLogger("no.nav.helse.spleis.gc.App")

@ExperimentalTime
fun main() {
    Thread.setDefaultUncaughtExceptionHandler { thread, err -> log.error("Uncaught exception in thread ${thread.name}: {}", err.message, err) }
    val ds = HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(hikariConfig, System.getenv("VAULT_MOUNTPATH"), System.getenv("DB_NAME") + "-admin")

    log.info("Commencing VACUUM FULL")
    val duration = measureTime { using(sessionOf(ds)) { it.run(queryOf(("VACUUM FULL person")).asExecute) } }
    log.info("VACUUM FULL completed after {} hour(s), {} minute(s) and {} second(s)", duration.inHours.toInt(), duration.inMinutes.toInt() % 60, duration.inSeconds.toInt() % 60)
}

private val hikariConfig get() = HikariConfig().apply {
    jdbcUrl = System.getenv("JDBC_URL").removeSuffix("/") + "/" + System.getenv("DB_NAME")
    maximumPoolSize = 3
    minimumIdle = 1
    idleTimeout = 10001
    connectionTimeout = 1000
    maxLifetime = 30001
}
