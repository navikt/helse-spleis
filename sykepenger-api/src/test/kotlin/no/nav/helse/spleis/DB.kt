package no.nav.helse.spleis

import com.github.navikt.tbd_libs.test_support.CleanupStrategy
import com.github.navikt.tbd_libs.test_support.DatabaseContainers

val databaseContainer = DatabaseContainers.container("spleis-api", CleanupStrategy.tables("person, melding"))
