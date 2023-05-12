package no.nav.helse.spleis.jobs

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.etterlevelse.MaskinellJurist
import no.nav.helse.serde.SerialisertPerson
import no.nav.helse.serde.api.serializePersonForSpeil
import no.nav.helse.serde.serialize
import no.nav.rapids_and_rivers.cli.AivenConfig
import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory
import org.apache.kafka.clients.producer.ProducerRecord
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import kotlin.system.measureTimeMillis
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
        "migrate" -> migrateTask(ConsumerProducerFactory(AivenConfig.default))
        "migrate_v2" -> migrateV2Task(args[1].trim().toInt())
        "test_speiljson" -> testSpeilJsonTask()
        else -> log.error("Unknown task $task")
    }
}

@ExperimentalTime
private fun vacuumTask() {
    val ds = DataSourceConfiguration(DbUser.SPLEIS).dataSource()
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

private fun migrateV2Task(targetVersjon: Int) {
    DataSourceConfiguration(DbUser.MIGRATE).dataSource().use { ds ->
        sessionOf(ds).use { session ->
            val finnArbeid = { txSession: TransactionalSession, utførArbeid: (Long, String) -> Unit ->
                txSession.run(queryOf("SELECT fnr,data FROM person WHERE skjema_versjon < $targetVersjon LIMIT 1 FOR UPDATE SKIP LOCKED;").map {
                    it.long("fnr") to it.string("data")
                }.asSingle)?.also { (ident, arbeid) -> utførArbeid(ident, arbeid) } != null

            }
            var arbeidUtført: Boolean
            var migreringCounter = 0
            do {
                arbeidUtført = session.transaction { txSession ->
                    finnArbeid(txSession) { ident, data ->
                        migreringCounter += 1
                        log.info("[$migreringCounter] Utfører migrering")
                        val time = measureTimeMillis {
                            val resultat = SerialisertPerson(data).deserialize(MaskinellJurist()).serialize()
                            check(1 == txSession.run(queryOf("UPDATE person SET skjema_versjon=:skjemaversjon, data=:data WHERE fnr=:ident", mapOf(
                                "skjemaversjon" to resultat.skjemaVersjon,
                                "data" to resultat.json,
                                "ident" to ident
                            )).asUpdate))
                        }
                        log.info("[$migreringCounter] Utført på $time ms")
                    }
                }
            } while (arbeidUtført)
        }
    }
}

// tester hvorvidt personer lar seg serialisere til speil uten exceptions
// bør bare kjøres med parallellism=1 fordi arbeidet kan ikke fordeles på flere podder
private fun testSpeilJsonTask() {
    DataSourceConfiguration(DbUser.MIGRATE).dataSource().use { ds ->
        sessionOf(ds).use { session ->
            session.run(queryOf("SELECT fnr,aktor_id FROM unike_person").map {
                it.long("fnr") to it.string("aktor_id")
            }.asList)
                .forEach {  (fnr, aktørId) ->
                    hentPerson(session, fnr)?.let { data ->
                        try {
                            serializePersonForSpeil(SerialisertPerson(data).deserialize(MaskinellJurist()), emptyList())
                        } catch (err: Exception) {
                            log.info("$aktørId lar seg ikke serialisere: ${err.message}")
                        }
                    }
                }
        }
    }
}
private fun hentPerson(session: Session, fnr: Long) =
    session.run(queryOf("SELECT data FROM person WHERE fnr = ? ORDER BY id DESC LIMIT 1", fnr).map {
        it.string("data")
    }.asSingle)

private fun migrateTask(factory: ConsumerProducerFactory) {
    DataSourceConfiguration(DbUser.MIGRATE).dataSource().use { ds ->
        var count = 0L
        factory.createProducer().use { producer ->
            sessionOf(ds).use { session ->
                session.run(queryOf("SELECT fnr,aktor_id FROM unike_person").map { row ->
                    val fnr = row.string("fnr").padStart(11, '0')
                    fnr to row.string("aktor_id")
                }.asList)
            }.forEach { (fnr, aktørId) ->
                count += 1
                producer.send(ProducerRecord("tbd.rapid.v1", fnr, lagMigrate(fnr, aktørId)))
            }
            producer.flush()
        }
        println()
        println("==============================")
        println("Sendte ut $count migreringer")
        println("==============================")
        println()
    }

}

@ExperimentalTime
private fun avstemmingTask(factory: ConsumerProducerFactory, customDayOfMonth: Int? = null) {
    // Håndter on-prem og gcp database tilkobling forskjellig
    val ds = DataSourceConfiguration(DbUser.AVSTEMMING).dataSource()
    val dayOfMonth = customDayOfMonth ?: LocalDate.now().dayOfMonth
    log.info("Commencing avstemming for dayOfMonth=$dayOfMonth")
    val producer = factory.createProducer()
    // spørringen funker slik at den putter alle personer opp i 28 ulike "bøtter" (fnr % 28 gir tall mellom 0 og 27, også plusser vi på én slik at vi starter fom. 1)
    // hvor én bøtte tilsvarer én dag i måneden. Tallet 28 (pga februar) ble valgt slik at vi sikrer oss at vi avstemmer
    // alle personer hver måned. Dag 29, 30, 31 avstemmes 0 personer siden det er umulig å ha disse rest-verdiene

    sessionOf(ds).use { session ->
        @Language("PostgreSQL")
        val statement = """
            SELECT fnr, aktor_id
            FROM unike_person
            WHERE ((1 + mod(fnr, 28)) = :dayOfMonth AND (sist_avstemt is null or sist_avstemt < now() - interval '1 day')) OR sist_avstemt < now() - interval '32 DAYS'
            """
        session.run(queryOf(statement, mapOf("dayOfMonth" to dayOfMonth)).map { row ->
            row.string("aktor_id") to row.string("fnr")
        }.asList).forEach { (aktørId, fnr) ->
            val fnrStr = fnr.padStart(11, '0')
            producer.send(ProducerRecord("tbd.rapid.v1", fnr, lagAvstemming(fnrStr, aktørId)))
        }
    }

    producer.flush()
    log.info("Avstemming completed")
}

private fun lagMigrate(fnr: String, aktørId: String) = """
{
  "@id": "${UUID.randomUUID()}",
  "@event_name": "json_migrate",
  "@opprettet": "${LocalDateTime.now()}",
  "aktørId": "$aktørId",
  "fødselsnummer": "$fnr"
}
"""

private fun lagAvstemming(fnr: String, aktørId: String) = """
{
  "@id": "${UUID.randomUUID()}",
  "@event_name": "person_avstemming",
  "@opprettet": "${LocalDateTime.now()}",
  "aktørId": "$aktørId",
  "fødselsnummer": "$fnr"
}
"""

private class DataSourceConfiguration(dbUsername: DbUser) {
    private val env = System.getenv()

    private val gcpProjectId = requireNotNull(env["GCP_TEAM_PROJECT_ID"]) { "gcp project id must be set" }
    private val databaseRegion = requireNotNull(env["DATABASE_REGION"]) { "database region must be set" }
    private val databaseInstance = requireNotNull(env["DATABASE_INSTANCE"]) { "database instance must be set" }
    private val databaseUsername = requireNotNull(env["${dbUsername}_USERNAME"]) { "database username must be set" }
    private val databasePassword = requireNotNull(env["${dbUsername}_PASSWORD"]) { "database password must be set"}
    private val databaseName = requireNotNull(env["${dbUsername}_DATABASE"]) { "database name must be set"}

    private val hikariConfig = HikariConfig().apply {
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
        initializationFailTimeout = Duration.ofMinutes(1).toMillis()
        connectionTimeout = Duration.ofSeconds(5).toMillis()
        maxLifetime = Duration.ofMinutes(30).toMillis()
        idleTimeout = Duration.ofMinutes(10).toMillis()
    }

    fun dataSource(maximumPoolSize: Int = 3) = HikariDataSource(hikariConfig.apply {
        this.maximumPoolSize = maximumPoolSize
    })
}

private enum class DbUser(private val dbUserPrefix: String) {
    SPLEIS("DATABASE"), AVSTEMMING("DATABASE_SPLEIS_AVSTEMMING"), MIGRATE("DATABASE_SPLEIS_MIGRATE");

    override fun toString() = dbUserPrefix
}
