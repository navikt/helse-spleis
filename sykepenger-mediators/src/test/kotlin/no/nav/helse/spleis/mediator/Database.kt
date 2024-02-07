package no.nav.helse.spleis.mediator

import com.github.navikt.tbd_libs.test_support.CleanupStrategy
import com.github.navikt.tbd_libs.test_support.DatabaseContainers

val databaseContainer = DatabaseContainers.container("spleis-mediators", CleanupStrategy.tables("person, melding"))