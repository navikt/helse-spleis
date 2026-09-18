package no.nav.helse.opprydding

import no.nav.helse.testdatabase.DatabaseContainer
import no.nav.helse.testdatabase.TestDataSource
import org.junit.jupiter.api.BeforeEach

// Alle tester bruker unike fødselsnumre (se `nyttFødselsnummer()`), så vi trenger ikke lenger
// en pool av databaser med truncate mellom hver test — én delt database holder, og tester kan
// kjøre parallelt mot den så lenge Hikari-poolen er stor nok til å dekke JUnit-parallellismen.
val databaseContainer = DatabaseContainer(
    appnavn = "spleis-opprydding-dev",
    maxHikariPoolSize = 32,
    postgresVersjon = 17,
)

internal abstract class DBTest {
    protected lateinit var dataSource: TestDataSource

    @BeforeEach
    internal fun setup() {
        dataSource = databaseContainer.nyTilkobling()
    }
}
