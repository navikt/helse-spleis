package no.nav.helse.serde.migration

import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import org.junit.jupiter.api.Test

@EnableToggle(Toggle.NyTilstandsflyt::class)
internal class V150MigrerVedtaksperioderTilNyTilstandsflytTest : MigrationTest({ V150MigrerVedtaksperioderTilNyTilstandsflyt() }) {

    @Test
    fun `Endrer alle tilstander til vedtaksperioder til tilstander i ny flyt`() {
        assertMigration(
            "/migrations/150/expected.json",
            "/migrations/150/original.json"
        )
    }
}