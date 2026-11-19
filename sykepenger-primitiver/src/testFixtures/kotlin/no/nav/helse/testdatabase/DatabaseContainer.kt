package no.nav.helse.testdatabase

import no.nav.sykepenger.libs.postgres.TestDatabase

/**
 * Én delt database per modul, migrert én gang med Flyway. Bruker [TestDatabase] fra sykepenger-libs, som starter en
 * Postgres-testcontainer, eller oppretter en egen database på serveren i `TEST_POSTGRES_JDBC_URL` når den er satt.
 *
 * Testene bruker unike fødselsnumre (se `nyttFødselsnummer()`) i stedet for å truncate mellom
 * hver test, så én database og én pool holder, så lenge poolen er stor nok til å dekke JUnit-parallellismen.
 */
class DatabaseContainer(
    postgresVersjon: Int = 17,
    maxHikariPoolSize: Int = 8,
) {
    private val testDatabase =
        TestDatabase
            .start(postgresImage = "postgres:$postgresVersjon", maximumPoolSize = maxHikariPoolSize)
            .also { it.migrer() }

    private val delt = TestDataSource(testDatabase.dataSource)

    // API-kompatibilitetsshim for eksisterende testkode: siden alle tester nå bruker unike
    // fødselsnumre holder det med én delt datakilde — ingen truncate eller retur av tilkobling
    // trengs mellom testene.
    fun nyTilkobling(): TestDataSource = delt
}
