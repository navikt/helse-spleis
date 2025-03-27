package no.nav.helse.opprydding

import com.github.navikt.tbd_libs.test_support.CleanupStrategy
import com.github.navikt.tbd_libs.test_support.DatabaseContainers
import com.github.navikt.tbd_libs.test_support.TestDataSource
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

val databaseContainer = DatabaseContainers.container("spleis-opprydding-dev", CleanupStrategy.tables("person, melding"))

internal abstract class DBTest {
    protected lateinit var dataSource: TestDataSource

    @BeforeEach
    internal fun setup() {
        dataSource = databaseContainer.nyTilkobling()
    }

    @AfterEach
    internal fun tearDown() {
        databaseContainer.droppTilkobling(dataSource)
    }
}
