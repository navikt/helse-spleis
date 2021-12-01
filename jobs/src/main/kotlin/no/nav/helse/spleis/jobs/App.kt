package no.nav.helse.spleis.jobs

import com.zaxxer.hikari.HikariConfig
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.rapids_and_rivers.cli.AivenConfig
import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
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
        "avstemming" -> avstemmingTask(ConsumerProducerFactory(AivenConfig.default))
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
private fun avstemmingTask(factory: ConsumerProducerFactory) {
    val ds = hikariConfig.datasource("readonly")
    val dayOfMonth = LocalDate.now().dayOfMonth
    log.info("Commencing avstemming for dayOfMonth=$dayOfMonth")
    val producer = factory.createProducer()
    val paginated = PaginatedQuery("fnr,aktor_id", "unike_person", "(1 + mod(fnr, 31)) = :dayOfMonth")
    val duration = measureTime {
        paginated.run(ds, mapOf("dayOfMonth" to dayOfMonth)) { row ->
            val fnr = row.string("fnr").padStart(11, '0')
            //producer.send(ProducerRecord("tbd.rapid.v1", fnr, lagAvstemming(fnr, row.string("aktor_id"))))
        }
    }
    producer.flush()
    log.info("Avstemming completed after {} hour(s), {} minute(s) and {} second(s)", duration.toInt(DurationUnit.HOURS), duration.toInt(DurationUnit.MINUTES) % 60, duration.toInt(DurationUnit.SECONDS) % 60)
}

private fun lagAvstemming(fnr: String, aktørId: String) = """
{
  "@id": "${UUID.randomUUID()}",
  "@event_name": "person_avstemming",
  "@opprettet": "${LocalDateTime.now()}",
  "aktørId": "$aktørId",
  "fødselsnummer": "$fnr"
}
"""

private class PaginatedQuery(private val select: String, private val table: String, private val where: String) {
    private var count by Delegates.notNull<Long>()
    private val resultsPerPage = 1000

    private fun count(session: Session, params: Map<String, Any>) {
        this.count = session.run(queryOf("SELECT COUNT(1) FROM $table WHERE $where", params).map { it.long(1) }.asSingle) ?: 0
    }

    internal fun run(dataSource: DataSource, params: Map<String, Any>, handler: (Row) -> Unit) {
        sessionOf(dataSource).use { session ->
            count(session, params)
            val pages = ceil(count / resultsPerPage.toDouble()).toInt()
            log.info("Total of $count records, yielding $pages pages ($resultsPerPage results per page)")
            var currentPage = 0
            while (currentPage < pages) {
                val rows = session.run(queryOf("SELECT $select FROM $table WHERE $where LIMIT $resultsPerPage OFFSET ${currentPage * resultsPerPage}", params).map { row -> handler(row) }.asList).count()
                currentPage += 1
                log.info("Page $currentPage of $pages complete ($rows rows)")
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
