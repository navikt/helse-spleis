package no.nav.helse.spleis

import com.github.navikt.tbd_libs.test_support.CleanupStrategy
import com.github.navikt.tbd_libs.test_support.DatabaseContainers

val databaseContainer = DatabaseContainers.container(
    appnavn = "spleis-api",
    cleanupStrategy = CleanupStrategy.tables("person, melding"),
    postgresVersjon = 17,
)
