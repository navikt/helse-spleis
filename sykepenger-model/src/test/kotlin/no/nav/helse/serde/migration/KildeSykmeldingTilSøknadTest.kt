package no.nav.helse.serde.migration

import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import org.junit.jupiter.api.Test

internal class KildeSykmeldingTilSøknadTest: MigrationTest(TestMigrering){

    @Test
    fun `endrer fra sykmelding til søknad for en enkeltdag`() {
        assertMigration(
            expectedJson = "/migrations/kildesykmeldingtilsøknad/enkeltdag_expected.json",
            originalJson = "/migrations/kildesykmeldingtilsøknad/enkeltdag_original.json"
        )
    }

    @Test
    fun `endrer fra sykmelding til søknad for en periode`() {
        assertMigration(
            expectedJson = "/migrations/kildesykmeldingtilsøknad/periode_expected.json",
            originalJson = "/migrations/kildesykmeldingtilsøknad/periode_original.json"
        )
    }

    @Test
    fun `ikke aktuell person`() {
        assertMigration(
            expectedJson = "/migrations/kildesykmeldingtilsøknad/ikke_aktuell_expected.json",
            originalJson = "/migrations/kildesykmeldingtilsøknad/ikke_aktuell_original.json"
        )
    }


    private object TestMigrering: KildeSykmeldingTilSøknad(versjon = 1337) {
        override fun perioderSomSkalEndres() = mapOf(
            21.januar til 31.januar to "5bf3d166-c13f-4e8f-8cc0-b30a6fc5e9a7".uuid,
            31.januar.somPeriode() to "f6b2280e-96d5-4adb-ada5-d22a4f20fb4c".uuid
        )
    }
}