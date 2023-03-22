package no.nav.helse.spleis.jobs.avstemAlle

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.rapids_and_rivers.cli.AivenConfig
import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory
import org.apache.kafka.clients.producer.ProducerRecord
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

private val log = LoggerFactory.getLogger("no.nav.helse.spleis.gc.avstemAlle.App")

private const val brukernavn: String = "<BRUKERNAVN>"
private const val passord: String = "<PASSORD>"
private val limit: Int? = 1000

@ExperimentalTime
fun main() {
    Thread.setDefaultUncaughtExceptionHandler { thread, err ->
        log.error(
            "Uncaught exception in thread ${thread.name}: {}",
            err.message,
            err
        )
    }
    avstemmingTask(ConsumerProducerFactory(AivenConfig.default))
}

@ExperimentalTime
private fun avstemmingTask(factory: ConsumerProducerFactory) {
    // Håndter on-prem og gcp database tilkobling forskjellig
    val ds = DataSourceConfiguration().dataSource()
    log.info("Commencing avstemming for all persons")

    val producer = factory.createProducer()
    val duration = measureTime {
        @Language("PostgreSQL")
        val query = "SELECT fnr, aktor_id FROM unike_person LIMIT ?"
        sessionOf(ds).use {
            it.run(queryOf(query, limit).map { row ->
                val fnr = row.string("fnr").padStart(11, '0')
                producer.send(ProducerRecord("tbd.rapid.v1", fnr, lagAvstemming(fnr, row.string("aktor_id"))))
            }.asList)
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

private class DataSourceConfiguration {
    private val hikariConfig = HikariConfig().apply {
        jdbcUrl = String.format("jdbc:postgresql://localhost:5432/spleis")

        username = brukernavn
        password = passord

        maximumPoolSize = 3
        minimumIdle = 1
        initializationFailTimeout = Duration.ofMinutes(1).toMillis()
        connectionTimeout = Duration.ofSeconds(5).toMillis()
        maxLifetime = Duration.ofMinutes(30).toMillis()
        idleTimeout = Duration.ofMinutes(10).toMillis()
    }

    fun dataSource() = HikariDataSource(hikariConfig)
}