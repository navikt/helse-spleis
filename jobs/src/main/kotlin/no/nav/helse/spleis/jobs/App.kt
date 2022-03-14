package no.nav.helse.spleis.jobs

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.time.Duration
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.rapids_and_rivers.cli.AivenConfig
import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import org.apache.kafka.clients.producer.ProducerRecord
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
    Thread.setDefaultUncaughtExceptionHandler { thread, err ->
        log.error(
            "Uncaught exception in thread ${thread.name}: {}",
            err.message,
            err
        )
    }

    if (args.isEmpty()) return log.error("Provide a task name as CLI argument")

    when (val task = args[0].trim().lowercase()) {
        "vacuum" -> vacuumTask()
        "avstemming" -> avstemmingTask(ConsumerProducerFactory(AivenConfig.default), args.getOrNull(1)?.toIntOrNull())
        else -> log.error("Unknown task $task")
    }
}

@ExperimentalTime
private fun vacuumTask() {
    val ds = when (System.getenv("NAIS_CLUSTER_NAME")) {
        "dev-gcp",
        "prod-gcp" -> GCP().dataSource()
        "dev-fss",
        "prod-fss" -> OnPrem("admin").dataSource()
        else -> throw IllegalArgumentException("env variable NAIS_CLUSTER_NAME has an unsupported value")
    }
    log.info("Commencing VACUUM FULL")
    val duration = measureTime {
        sessionOf(ds).use { session -> session.run(queryOf(("VACUUM FULL person")).asExecute) }
    }
    log.info(
        "VACUUM FULL completed after {} hour(s), {} minute(s) and {} second(s)",
        duration.toInt(DurationUnit.HOURS),
        duration.toInt(DurationUnit.MINUTES) % 60,
        duration.toInt(DurationUnit.SECONDS) % 60
    )
}

@ExperimentalTime
private fun avstemmingTask(factory: ConsumerProducerFactory, customDayOfMonth: Int? = null) {
    // Håndter on-prem og gcp database tilkobling forskjellig
    val ds = when (System.getenv("NAIS_CLUSTER_NAME")) {
        "dev-gcp",
        "prod-gcp" -> GCP().dataSource()
        "dev-fss",
        "prod-fss" -> OnPrem().dataSource()
        else -> throw IllegalArgumentException("env variable NAIS_CLUSTER_NAME has an unsupported value")
    }
    val dayOfMonth = customDayOfMonth ?: LocalDate.now().dayOfMonth
    log.info("Commencing avstemming for dayOfMonth=$dayOfMonth")
    val producer = factory.createProducer()
    val paginated = PaginatedQuery(
        "fnr,aktor_id",
        "unike_person",
        "(1 + mod(fnr, 27)) = :dayOfMonth AND (sist_avstemt is null or sist_avstemt < now() - interval '1 day')"
    )
    val duration = measureTime {
        paginated.run(ds, mapOf("dayOfMonth" to dayOfMonth)) { row ->
            val fnr = row.string("fnr").padStart(11, '0')
            producer.send(ProducerRecord("tbd.rapid.v1", fnr, lagAvstemming(fnr, row.string("aktor_id"))))
        }
    }
    producer.flush()
    log.info(
        "Avstemming completed after {} hour(s), {} minute(s) and {} second(s)",
        duration.toInt(DurationUnit.HOURS),
        duration.toInt(DurationUnit.MINUTES) % 60,
        duration.toInt(DurationUnit.SECONDS) % 60
    )
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
        this.count =
            session.run(queryOf("SELECT COUNT(1) FROM $table WHERE $where", params).map { it.long(1) }.asSingle) ?: 0
    }

    internal fun run(dataSource: DataSource, params: Map<String, Any>, handler: (Row) -> Unit) {
        sessionOf(dataSource).use { session ->
            count(session, params)
            val pages = ceil(count / resultsPerPage.toDouble()).toInt()
            log.info("Total of $count records, yielding $pages pages ($resultsPerPage results per page)")
            var currentPage = 0
            while (currentPage < pages) {
                val rows = session.run(
                    queryOf(
                        "SELECT $select FROM $table WHERE $where LIMIT $resultsPerPage OFFSET ${currentPage * resultsPerPage}",
                        params
                    ).map { row -> handler(row) }.asList
                ).count()
                currentPage += 1
                log.info("Page $currentPage of $pages complete ($rows rows)")
            }
        }
    }
}

private interface DataSourceConfiguration {
    fun dataSource(): DataSource
}

private class GCP : DataSourceConfiguration {
    private val env = System.getenv()

    private val gcpProjectId = env["GCP_TEAM_PROJECT_ID"]
    private val databaseRegion = env["DATABASE_REGION"]
    private val databaseInstance = env["DATABASE_INSTANCE"]
    private val databaseUsername = env["DATABASE_SPLEIS_API_USERNAME"]?.toString()
    private val databasePassword = env["DATABASE_SPLEIS_API_PASSWORD"]?.toString()
    private val databaseName = env["DATABASE_DATABASE"]

    private val hikariConfig = HikariConfig().apply {
        requireNotNull(gcpProjectId) { "gcp project id must be set" }
        requireNotNull(databaseRegion) { "database region must be set" }
        requireNotNull(databaseInstance) { "database instance must be set" }
        requireNotNull(databaseName) { "database name must be set" }
        requireNotNull(databaseUsername) { "database username must be set"}
        requireNotNull(databasePassword) { "database password must be set"}

        jdbcUrl = String.format(
            "jdbc:postgresql:///%s?%s&%s",
            databaseName,
            "cloudSqlInstance=$gcpProjectId:$databaseRegion:$databaseInstance",
            "socketFactory=com.google.cloud.sql.postgres.SocketFactory"
        )

        username = databaseUsername
        password = databasePassword

        maximumPoolSize = 3
        minimumIdle = 1
        connectionTimeout = Duration.ofSeconds(5).toMillis()
        maxLifetime = Duration.ofMinutes(30).toMillis()
        idleTimeout = Duration.ofMinutes(10).toMillis()
    }

    override fun dataSource() = HikariDataSource(hikariConfig)
}

// Understands how to create a data source from environment variables
private class OnPrem(private val role: String = "readonly"): DataSourceConfiguration {
    private val env = System.getenv()
    private val url = env["JDBC_URL"]
    private val dbName = env["DB_NAME"]

    // username and password is only needed when vault is not enabled,
    // since we rotate credentials automatically when vault is enabled
    private val hikariConfig = HikariConfig().apply {
        requireNotNull(url) { "postgres url must be set" }
        requireNotNull(dbName) { "db name must be set" }
        jdbcUrl = url.removeSuffix("/") + "/" + dbName

        maximumPoolSize = 3
        minimumIdle = 1
        connectionTimeout = Duration.ofSeconds(5).toMillis()
        maxLifetime = Duration.ofMinutes(30).toMillis()
        idleTimeout = Duration.ofMinutes(10).toMillis()
    }

    override fun dataSource(): DataSource = HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(
        hikariConfig,
        System.getenv("VAULT_MOUNTPATH"),
        System.getenv("DB_NAME") + "-$role"
    )
}
