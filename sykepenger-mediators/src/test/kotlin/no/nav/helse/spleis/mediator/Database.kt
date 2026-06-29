package no.nav.helse.spleis.mediator

import com.github.navikt.tbd_libs.test_support.CleanupStrategy
import com.github.navikt.tbd_libs.test_support.DatabaseContainers

val databaseContainer = DatabaseContainers.container(
    appnavn = "spleis-mediators",
    cleanupStrategy = CleanupStrategy.tables("person, melding, utboks"),
    postgresVersjon = 17,
)
