package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V299EgenmeldingerFraSykdomstidslinjeTilVedtaksperiodeTest : MigrationTest(V299EgenmeldingerFraSykdomstidslinjeTilVedtaksperiode()) {

    @Test
    fun `migrerer egenmeldingsperioder fra sykdomstidslinjen til vedtaksperiode`() {
        assertMigration("/migrations/299/expected.json", "/migrations/299/original.json")
    }


    @Test
    fun `ingen egenmeldingsperioder fra sykdomstidslinjen`() {
        assertMigration("/migrations/299/expected_ingen_egenmeldinger.json", "/migrations/299/original_ingen_egenmeldinger.json")
    }
}