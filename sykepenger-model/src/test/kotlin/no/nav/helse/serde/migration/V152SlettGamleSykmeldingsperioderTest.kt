package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V152SlettGamleSykmeldingsperioderTest: MigrationTest(V152SlettGamleSykmeldingsperioder()) {

    @Test
    fun `Sletter gamle sykmeldingsperioder for en arbeidsgiver`() {
        assertMigration(
            "/migrations/152/enArbeidsgiverExpected.json",
            "/migrations/152/enArbeidsgiverOriginal.json"
        )
    }

    @Test
    fun `Sletter gamle sykmeldingsperioder når vi kun har en periode`() {
        assertMigration(
            "/migrations/152/enArbeidsgiverExpected2.json",
            "/migrations/152/enArbeidsgiverOriginal2.json"
        )
    }

  @Test
    fun `dummy case med tomme lister`() {
        assertMigration(
            "/migrations/152/simpleDummyExpected.json",
            "/migrations/152/simpleDummyOriginal.json"
        )
    }

    @Test
    fun `Sletter gamle sykmeldingsperioder på tvers av arbeidsgivere`() {
        assertMigration(
            "/migrations/152/flereArbeidsgivereExpected.json",
            "/migrations/152/flereArbeidsgivereOriginal.json"
        )
    }

    @Test
    fun `Arbeidsgiver med ingen vedtaksperioder avbryter ikke migreringen av andre arbeidsgivere`() {
        assertMigration(
            "/migrations/152/flereArbeidsgivereExpected2.json",
            "/migrations/152/flereArbeidsgivereOriginal2.json"
        )
    }
}