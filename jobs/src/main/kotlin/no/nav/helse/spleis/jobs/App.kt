package no.nav.helse.spleis.jobs

import com.github.navikt.tbd_libs.kafka.AivenConfig
import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import com.github.navikt.tbd_libs.sql_dsl.connection
import com.github.navikt.tbd_libs.sql_dsl.firstOrNull
import com.github.navikt.tbd_libs.sql_dsl.long
import com.github.navikt.tbd_libs.sql_dsl.mapNotNull
import com.github.navikt.tbd_libs.sql_dsl.prepareStatementWithNamedParameters
import com.github.navikt.tbd_libs.sql_dsl.single
import com.github.navikt.tbd_libs.sql_dsl.string
import com.github.navikt.tbd_libs.sql_dsl.transaction
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.util.*
import kotlin.system.measureTimeMillis
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.helse.etterlevelse.Regelverkslogg
import no.nav.helse.person.Person
import no.nav.helse.serde.SerialisertPerson
import no.nav.helse.serde.tilPersonData
import no.nav.helse.serde.tilSerialisertPerson
import org.apache.kafka.clients.producer.ProducerRecord
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory

val log = LoggerFactory.getLogger("no.nav.helse.spleis.gc.App")
val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

@ExperimentalTime
fun main(cliArgs: Array<String>) {
    Thread.setDefaultUncaughtExceptionHandler { thread, err ->
        log.error(
            "Uncaught exception in thread ${thread.name}: {}",
            err.message,
            err
        )
    }

    val args = cliArgs.takeIf(Array<*>::isNotEmpty)?.toList() ?: System.getenv("RUNTIME_OPTS").split(" ")
    if (args.isEmpty()) return log.error("Provide a task name as CLI argument")

    val dryrun = System.getenv("DRYRUN")?.toBoolean() ?: false

    val factory by lazy { ConsumerProducerFactory(AivenConfig.default) }

    when (val task = args[0].trim().lowercase()) {
        "vacuum" -> vacuumTask()
        "avstemming" -> avstemmingTask(factory, args.getOrNull(1)?.toIntOrNull())
        "migrate" -> migrateTask(factory)
        "migrate_v2" -> migrateV2Task(args[1].trim(), args.getOrNull(2)?.toIntOrNull() ?: 10)
        "test_speiljson" -> testSpeilJsonTask(args[1].trim())
        "migrereg" -> migrereGrunnbeløp(factory, args[1].trim())
        "dobbelutbetalinger" -> finneDobbelutbetalinger(args[1].trim())
        "feriepenger" -> startFeriepenger(factory, arbeidId = args[1].trim(), opptjeningsår = Year.of(args[2].trim().toInt()), dryrun = dryrun)
        else -> log.error("Unknown task $task")
    }
}

@ExperimentalTime
private fun vacuumTask() {
    val ds = DataSourceConfiguration(DbUser.SPLEIS).dataSource()
    log.info("Commencing VACUUM FULL")
    val duration = measureTime {
        ds.connection {
            createStatement().execute("VACUUM FULL person")
        }
    }
    log.info(
        "VACUUM FULL completed after {} hour(s), {} minute(s) and {} second(s)",
        duration.toInt(DurationUnit.HOURS),
        duration.toInt(DurationUnit.MINUTES) % 60,
        duration.toInt(DurationUnit.SECONDS) % 60
    )
}

private fun migrateV2Task(arbeidId: String, size: Int) {
    @Language("PostgreSQL")
    val query = """
        SELECT data FROM person WHERE fnr = ? LIMIT 1 FOR UPDATE SKIP LOCKED;
    """
    var migreringCounter = 0
    opprettOgUtførArbeid(arbeidId, size = size) { connection, fnr ->
        connection.transaction {
            // låser ned person-raden slik at spleis ikke tar inn meldinger og overskriver mens denne podden holder på
            val data = prepareStatement(query).use { stmt ->
                stmt.setLong(1, fnr)
                stmt.executeQuery().firstOrNull { it.string("data") }
            }
            if (data != null) {
                migreringCounter += 1
                log.info("[$migreringCounter] Utfører migrering")
                val time = measureTimeMillis {
                    val dto = SerialisertPerson(data).tilPersonDto()
                    check(dto.fødselsnummer.toLong() == fnr) { "fnr samsvarer ikke" }
                    val gjenopprettetPerson = Person.gjenopprett(Regelverkslogg.EmptyLog, dto)
                    val resultat = gjenopprettetPerson.dto().tilPersonData().tilSerialisertPerson()
                    check(
                        1 == prepareStatement("UPDATE person SET skjema_versjon=?, data=? WHERE fnr=?;").use { stmt ->
                            stmt.setInt(1, resultat.skjemaVersjon)
                            stmt.setString(2, resultat.json)
                            stmt.setLong(3, fnr)
                            stmt.executeUpdate()
                        }
                    )
                }
                log.info("[$migreringCounter] Utført på $time ms")
            }
        }
    }
}

private fun fåLås(connection: Connection, arbeidId: String): Boolean {
    // oppretter en lås som varer ut levetiden til sesjonen.
    // returnerer umiddelbart med true/false avhengig om vi fikk låsen eller ikke
    @Language("PostgreSQL")
    val query = "SELECT pg_try_advisory_lock(1337)"
    return connection.prepareStatement(query).use { stmt ->
        stmt.executeQuery().single { rs -> rs.getBoolean(1) }
    }
}

private fun fyllArbeidstabell(connection: Connection, arbeidId: String) {
    @Language("PostgreSQL")
    val query = """
        INSERT INTO arbeidstabell (arbeid_id,fnr,arbeid_startet,arbeid_ferdig)
        SELECT ?, fnr, null, null from person
        ON CONFLICT (arbeid_id,fnr) DO NOTHING; 
    """
    connection.prepareStatement(query).use { stmt ->
        stmt.setString(1, arbeidId)
        stmt.execute()
    }
}

private fun hentArbeid(connection: Connection, arbeidId: String, size: Int = 500): List<Long> {
    @Language("PostgreSQL")
    val query = """
    select fnr from arbeidstabell where arbeid_startet IS NULL and arbeid_id = ? limit $size for update skip locked; 
    """

    @Language("PostgreSQL")
    val oppdater = "update arbeidstabell set arbeid_startet=now() where arbeid_id=? and fnr = ANY(?)"
    return connection.transaction {
        prepareStatement(query).use { stmt ->
            stmt.setString(1, arbeidId)
            stmt.executeQuery().mapNotNull { it.getLong(1) }
        }.also { personer ->
            if (personer.isNotEmpty()) {
                val affectedRows = prepareStatement(oppdater).use { stmt ->
                    stmt.setString(1, arbeidId)
                    stmt.setArray(2, createArrayOf("BIGINT", personer.toTypedArray()))
                    stmt.executeUpdate()
                }
                check(affectedRows == personer.size) {
                    "forventet å oppdatere nøyaktig ${personer.size} rader"
                }
            }
        }
    }
}

private fun arbeidFullført(connection: Connection, arbeidId: String, fnr: Long) {
    @Language("PostgreSQL")
    val query = "update arbeidstabell set arbeid_ferdig=now() where arbeid_id=? and fnr=?"
    val affectedRows = connection.prepareStatement(query).use { stmt ->
        stmt.setString(1, arbeidId)
        stmt.setLong(2, fnr)
        stmt.executeUpdate()
    }
    check(affectedRows == 1) {
        "forventet å oppdatere nøyaktig én rad"
    }
}

private fun arbeidFinnes(connection: Connection, arbeidId: String): Boolean {
    @Language("PostgreSQL")
    val query = "SELECT COUNT(1) as antall FROM arbeidstabell where arbeid_id=?"
    val antall = connection.prepareStatement(query).use { stmt ->
        stmt.setString(1, arbeidId)
        stmt.executeQuery().single { rs -> rs.getLong(1) }
    }
    return antall > 0
}

private fun klargjørEllerVentPåTilgjengeligArbeid(connection: Connection, arbeidId: String) {
    if (fåLås(connection, arbeidId)) {
        if (arbeidFinnes(connection, arbeidId)) return
        return fyllArbeidstabell(connection, arbeidId)
    }

    log.info("Venter på at arbeid skal bli tilgjengelig")
    while (!arbeidFinnes(connection, arbeidId)) {
        log.info("Arbeid finnes ikke ennå, venter litt")
        runBlocking { delay(250) }
    }
}

fun opprettOgUtførArbeid(arbeidId: String, size: Int = 1, arbeider: (connection: Connection, fnr: Long) -> Unit) {
    DataSourceConfiguration(DbUser.MIGRATE).dataSource(maximumPoolSize = 1).use { ds ->
        ds.connection {
            klargjørEllerVentPåTilgjengeligArbeid(this, arbeidId)
            do {
                log.info("Forsøker å hente arbeid")
                val arbeidsliste = hentArbeid(this, arbeidId, size)
                    .also {
                        if (it.isNotEmpty()) log.info("Fikk ${it.size} stk")
                    }
                    .onEach { fnr ->
                        try {
                            arbeider(this, fnr)
                            arbeidFullført(this, arbeidId, fnr)
                        } catch (e: Exception) {
                            log.error("feil ved arbeidId=$arbeidId: ${e.message}", e)
                            sikkerlogg.error("feil ved arbeidId=$arbeidId, fnr=$fnr: ${e.message}", e)
                        }
                    }
            } while (arbeidsliste.isNotEmpty())
            log.info("Fant ikke noe arbeid, avslutter")
        }
    }
}

private fun testSpeilJsonTask(arbeidId: String) {
    /*opprettOgUtførArbeid(arbeidId) { session, fnr ->
        hentPerson(session, fnr)?.let { data ->
            try {
                val dto = SerialisertPerson(data).tilPersonDto()
                val person = Person.gjenopprett(MaskinellJurist(), dto)
                val pølsepakke: SpekematDTO.PølsepakkeDTO = TODO("Må hente pølsepakken fra spekemat")
                serializePersonForSpeil(person, pølsepakke)
            } catch (err: Exception) {
                log.info("person lar seg ikke serialisere: ${err.message}")
            }
        }
    }*/
}

fun hentPerson(connection: Connection, fnr: Long) =
    connection.prepareStatementWithNamedParameters("SELECT data FROM person WHERE fnr = :fnr ORDER BY id DESC LIMIT 1") {
        withParameter("fnr", fnr)
    }.use { stmt ->
        stmt.executeQuery().single { rs -> rs.string("data") }
    }

private fun migrateTask(factory: ConsumerProducerFactory) {
    DataSourceConfiguration(DbUser.MIGRATE).dataSource().use { ds ->
        var count = 0L
        factory.createProducer().use { producer ->
            ds.connection {
                prepareStatement("SELECT fnr FROM person").use { stmt ->
                    stmt.executeQuery().mapNotNull { row ->
                        row.long("fnr").toString().padStart(11, '0')
                    }
                }
            }.forEach { fnr ->
                count += 1
                producer.send(ProducerRecord("tbd.rapid.v1", fnr, lagMigrate(fnr)))
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

    ds.connection {
        @Language("PostgreSQL")
        val statement = """
            SELECT fnr
            FROM person
            WHERE (1 + mod(fnr, 28)) = :dag AND (sist_avstemt is null or sist_avstemt < now() - interval '1 day')
            """
        prepareStatementWithNamedParameters(statement) {
            withParameter("dag", dayOfMonth)
        }.use { stmt ->
            stmt.executeQuery().mapNotNull { it.long(1) }
        }.forEach { fnr ->
            val fnrStr = fnr.toString().padStart(11, '0')
            producer.send(ProducerRecord("tbd.rapid.v1", fnrStr, lagAvstemming(fnrStr)))
        }
    }

    producer.flush()
    log.info("Avstemming completed")
}

private fun lagMigrate(fnr: String) = """
{
  "@id": "${UUID.randomUUID()}",
  "@event_name": "json_migrate",
  "@opprettet": "${LocalDateTime.now()}",
  "fødselsnummer": "$fnr"
}
"""

private fun lagAvstemming(fnr: String) = """
{
  "@id": "${UUID.randomUUID()}",
  "@event_name": "person_avstemming",
  "@opprettet": "${LocalDateTime.now()}",
  "fødselsnummer": "$fnr"
}
"""

private class DataSourceConfiguration(dbUsername: DbUser) {
    private val env = System.getenv()

    private val gcpProjectId = requireNotNull(env["GCP_TEAM_PROJECT_ID"]) { "gcp project id must be set" }
    private val databaseRegion = requireNotNull(env["DATABASE_REGION"]) { "database region must be set" }
    private val databaseInstance = requireNotNull(env["DATABASE_INSTANCE"]) { "database instance must be set" }
    private val databaseUsername = requireNotNull(env["${dbUsername}_USERNAME"]) { "database username must be set" }
    private val databasePassword = requireNotNull(env["${dbUsername}_PASSWORD"]) { "database password must be set" }
    private val databaseName = requireNotNull(env["${dbUsername}_DATABASE"]) { "database name must be set" }

    private val hikariConfig = HikariConfig().apply {
        jdbcUrl = String.format(
            "jdbc:postgresql:///%s?%s&%s",
            databaseName,
            "cloudSqlInstance=$gcpProjectId:$databaseRegion:$databaseInstance",
            "socketFactory=com.google.cloud.sql.postgres.SocketFactory"
        )

        username = databaseUsername
        password = databasePassword

        maximumPoolSize = 2
    }

    fun dataSource(maximumPoolSize: Int = 2) = HikariDataSource(HikariConfig().apply {
        hikariConfig.copyStateTo(this)
        this.maximumPoolSize = maximumPoolSize
    })
}

private enum class DbUser(private val dbUserPrefix: String) {
    SPLEIS("DATABASE"), AVSTEMMING("DATABASE_SPLEIS_AVSTEMMING"), MIGRATE("DATABASE_SPLEIS_MIGRATE");

    override fun toString() = dbUserPrefix
}
