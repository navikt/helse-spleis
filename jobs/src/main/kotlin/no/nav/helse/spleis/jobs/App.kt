package no.nav.helse.spleis.jobs

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
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
        "migrate_v2" -> migrateV2Task(args[1].trim())
        "test_speiljson" -> testSpeilJsonTask(args[1].trim())
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

private fun migrateV2Task(arbeidId: String) {
    @Language("PostgreSQL")
    val query = """
        SELECT data FROM person WHERE fnr = ? LIMIT 1 FOR UPDATE SKIP LOCKED;
    """
    var migreringCounter = 0
    opprettOgUtførArbeid(arbeidId) { session, fnr ->
        session.transaction { txSession ->
            // låser ned person-raden slik at spleis ikke tar inn meldinger og overskriver mens denne podden holder på
            val data = txSession.run(queryOf(query, fnr).map { it.string("data") }.asSingle)
            if (data != null) {
                migreringCounter += 1
                log.info("[$migreringCounter] Utfører migrering")
                val time = measureTimeMillis {
                    val resultat = SerialisertPerson(data).deserialize(MaskinellJurist()).serialize()
                    check(1 == txSession.run(queryOf("UPDATE person SET skjema_versjon=:skjemaversjon, data=:data WHERE fnr=:ident", mapOf(
                        "skjemaversjon" to resultat.skjemaVersjon,
                        "data" to resultat.json,
                        "ident" to fnr
                    )).asUpdate))
                }
                log.info("[$migreringCounter] Utført på $time ms")
            }
        }
    }
}


private fun fyllArbeidstabell(session: Session, arbeidId: String) {
    @Language("PostgreSQL")
    val query = """
        INSERT INTO arbeidstabell (arbeid_id,fnr,arbeid_startet,arbeid_ferdig)
        SELECT ?, fnr, null, null from person
        ON CONFLICT (arbeid_id,fnr) DO NOTHING; 
    """
    session.run(queryOf(query, arbeidId).asExecute)
}
private fun hentArbeid(session: Session, arbeidId: String): Long? {
    @Language("PostgreSQL")
    val query = """
    select fnr from arbeidstabell where arbeid_startet IS NULL and arbeid_id = ? limit 1 for update skip locked; 
    """
    @Language("PostgreSQL")
    val oppdater = "update arbeidstabell set arbeid_startet=now() where arbeid_id=? and fnr=?"
    return session.transaction { txSession ->
        txSession.run(queryOf(query, arbeidId).map { it.long("fnr") }.asSingle)?.also { fnr ->
            txSession.run(queryOf(oppdater, arbeidId, fnr).asUpdate)
        }
    }
}
private fun arbeidFullført(session: Session, arbeidId: String, fnr: Long) {
    @Language("PostgreSQL")
    val query = "update arbeidstabell set arbeid_ferdig=now() where arbeid_id=? and fnr=?"
    session.run(queryOf(query, arbeidId, fnr).asUpdate)
}
private fun arbeidFinnes(session: Session, arbeidId: String): Boolean {
    @Language("PostgreSQL")
    val query = "SELECT COUNT(1) as antall FROM arbeidstabell where arbeid_id=?"
    val antall = session.run(queryOf(query, arbeidId).map { it.long("antall") }.asSingle) ?: 0
    return antall > 0
}

private fun opprettOgUtførArbeid(arbeidId: String, arbeider: (session: Session, fnr: Long) -> Unit) {
    DataSourceConfiguration(DbUser.MIGRATE).dataSource().use { ds ->
        sessionOf(ds).use { session ->
            fyllArbeidstabell(session, arbeidId)

            log.info("Venter på at arbeid skal bli tilgjengelig")
            while (!arbeidFinnes(session, arbeidId)) {
                log.info("Arbeid finnes ikke ennå, venter litt")
                runBlocking { delay(250) }
            }
            var fnr: Long?
            do {
                log.info("Forsøker å hente arbeid")
                fnr = hentArbeid(session, arbeidId)?.also { fnr ->
                    try {
                        arbeider(session, fnr)
                    } finally {
                        arbeidFullført(session, arbeidId, fnr)
                    }
                }
            } while (fnr != null)
            log.info("Fant ikke noe arbeid, avslutter")
        }
    }
}

private fun testSpeilJsonTask(arbeidId: String) {
    opprettOgUtførArbeid(arbeidId) { session, fnr ->
        hentPerson(session, fnr)?.let { (aktørId, data) ->
            try {
                serializePersonForSpeil(SerialisertPerson(data).deserialize(MaskinellJurist()), emptyList())
            } catch (err: Exception) {
                log.info("$aktørId lar seg ikke serialisere: ${err.message}")
            }
        }
    }
}
private fun hentPerson(session: Session, fnr: Long) =
    session.run(queryOf("SELECT aktor_id, data FROM person WHERE fnr = ? ORDER BY id DESC LIMIT 1", fnr).map {
        it.string("aktor_id") to it.string("data")
    }.asSingle)

private fun migrateTask(factory: ConsumerProducerFactory) {
    DataSourceConfiguration(DbUser.MIGRATE).dataSource().use { ds ->
        var count = 0L
        factory.createProducer().use { producer ->
            sessionOf(ds).use { session ->
                session.run(queryOf("SELECT fnr,aktor_id FROM person").map { row ->
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
            FROM person
            WHERE (1 + mod(fnr, 28)) = :dayOfMonth AND (sist_avstemt is null or sist_avstemt < now() - interval '1 day')
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
