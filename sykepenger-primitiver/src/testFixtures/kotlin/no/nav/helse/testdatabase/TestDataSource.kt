package no.nav.helse.testdatabase

import javax.sql.DataSource

/**
 * Tynn wrapper rundt en delt [DataSource]. Finnes for å holde eksisterende testkode (som ble
 * skrevet mot en pool av databaser) uendret — se [DatabaseContainer].
 */
class TestDataSource(
    val ds: DataSource,
)
