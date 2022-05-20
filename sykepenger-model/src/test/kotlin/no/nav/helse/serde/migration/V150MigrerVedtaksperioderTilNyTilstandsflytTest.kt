package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V150MigrerVedtaksperioderTilNyTilstandsflytTest :
    MigrationTest({ V150MigrerVedtaksperioderTilNyTilstandsflyt() }) {

    @Test
    fun `Endrer alle tilstander til vedtaksperioder til tilstander i ny flyt`() {
        assertMigration(
            "/migrations/150/expected.json",
            "/migrations/150/original.json"
        )
    }
}