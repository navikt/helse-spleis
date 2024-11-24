package no.nav.helse.opprydding

import com.github.navikt.tbd_libs.naisful.postgres.ConnectionConfigFactory
import com.github.navikt.tbd_libs.naisful.postgres.jdbcUrlWithGoogleSocketFactory
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.rapids_rivers.RapidApplication

internal class ApplicationBuilder(env: Map<String, String>) {
    private val rapidsConnection = RapidApplication.create(env)
    private val hikariConfig = HikariConfig().apply {
        jdbcUrl = jdbcUrlWithGoogleSocketFactory(
            databaseInstance = env.getValue("DATABASE_INSTANCE"),
            metode = ConnectionConfigFactory.MountPath("/var/run/secrets/spleis_sql")
        )
        poolName = "app"
        maximumPoolSize = 1
    }

    init {
        SlettPersonRiver(rapidsConnection, PersonRepository(HikariDataSource(hikariConfig)))
    }

    internal fun start() = rapidsConnection.start()
}