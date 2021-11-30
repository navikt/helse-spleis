package no.nav.helse.spleis.jobs

import com.zaxxer.hikari.HikariConfig
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import org.slf4j.LoggerFactory
import java.time.LocalDate
import javax.sql.DataSource
import kotlin.math.ceil
import kotlin.properties.Delegates
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

private val log = LoggerFactory.getLogger("no.nav.helse.spleis.gc.App")

@ExperimentalTime
fun main(args: Array<String>) {
    Thread.setDefaultUncaughtExceptionHandler { thread, err -> log.error("Uncaught exception in thread ${thread.name}: {}", err.message, err) }

    if (args.isEmpty()) return log.error("Provide a task name as CLI argument")

    when (val task = args[0].trim().lowercase()) {
        "vacuum" -> vacuumTask()
        "avstemming" -> avstemmingTask()
        else -> log.error("Unknown task $task")
    }
}

@ExperimentalTime
private fun vacuumTask() {
    val ds = hikariConfig.datasource("admin")
    log.info("Commencing VACUUM FULL")
    val duration = measureTime {
        sessionOf(ds).use { session -> session.run(queryOf(("VACUUM FULL person")).asExecute) }
    }
    log.info("VACUUM FULL completed after {} hour(s), {} minute(s) and {} second(s)", duration.toInt(DurationUnit.HOURS), duration.toInt(DurationUnit.MINUTES) % 60, duration.toInt(DurationUnit.SECONDS) % 60)
}

@ExperimentalTime
private fun avstemmingTask() {
    val ds = hikariConfig.datasource("readonly")
    log.info("Commencing avstemming")
    val dayOfMonth = LocalDate.now().dayOfMonth
    val paginated = PaginatedQuery("fnr,aktor_id", "unike_person", "(1 + mod(fnr, 31)) = :dayOfMonth")
    val duration = measureTime {
        paginated.run(ds, mapOf("dayOfMonth" to dayOfMonth)) { row ->
            // TODO: push message to kafka
        }
    }
    log.info("Avstemming completed after {} hour(s), {} minute(s) and {} second(s)", duration.toInt(DurationUnit.HOURS), duration.toInt(DurationUnit.MINUTES) % 60, duration.toInt(DurationUnit.SECONDS) % 60)
}

private class PaginatedQuery(private val select: String, private val table: String, private val where: String) {
    private var count by Delegates.notNull<Long>()
    private val resultsPerPage = 1000

    private fun count(session: Session, params: Map<String, Any>) {
        this.count = session.run(queryOf("SELECT COUNT(1) FROM $table WHERE $where", params).map { it.long(1) }.asSingle) ?: 0
    }

    internal fun run(dataSource: DataSource, params: Map<String, Any>, handler: (Row) -> Unit) {
        sessionOf(dataSource).use { session ->
            count(session, params)
            val pages = ceil(count / resultsPerPage.toDouble())
            log.info("Total of $count records, yielding $pages pages ($resultsPerPage results pre page)")
            var currentPage = 0
            while (currentPage < pages) {
                session.run(queryOf("SELECT $select FROM $table WHERE $where LIMIT $resultsPerPage OFFSET ${currentPage * resultsPerPage}", params).map { row -> handler(row) }.asList)
                currentPage += 1
                log.info("Page $currentPage of $pages complete")
            }
        }
    }
}

private val hikariConfig get() = HikariConfig().apply {
    jdbcUrl = System.getenv("JDBC_URL").removeSuffix("/") + "/" + System.getenv("DB_NAME")
    maximumPoolSize = 3
    minimumIdle = 1
    idleTimeout = 10001
    connectionTimeout = 1000
    maxLifetime = 30001
}

private fun HikariConfig.datasource(role: String) =
    HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(this, System.getenv("VAULT_MOUNTPATH"), System.getenv("DB_NAME") + "-$role")
