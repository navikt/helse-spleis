package no.nav.helse.testdatabase

import com.zaxxer.hikari.HikariDataSource

/**
 * Tynn wrapper rundt en delt [HikariDataSource]. Finnes for å holde eksisterende testkode (som ble
 * skrevet mot en pool av databaser) uendret — se [DatabaseContainer].
 */
class TestDataSource(val ds: HikariDataSource)
