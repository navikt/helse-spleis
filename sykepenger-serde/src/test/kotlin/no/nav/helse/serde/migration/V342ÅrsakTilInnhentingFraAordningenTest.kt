package no.nav.helse.serde.migration

import java.time.LocalDateTime
import java.util.UUID
import org.junit.jupiter.api.Test

internal class V342ÅrsakTilInnhentingFraAordningenTest: MigrationTest(V342ÅrsakTilInnhentingFraAordningen()) {

    @Test
    fun `Sette årsakTilInnhenting på inntekter fra Aordningen`() {
        assertMigration("/migrations/342/expected.json", "/migrations/342/original.json")
    }

    override fun meldingerSupplier() = MeldingerSupplier { mapOf(
        hendelse(UUID.fromString("00000000-0000-0000-0000-000000000000"), "KAN_VÆRE"),
        hendelse(UUID.fromString("00000000-0000-0000-0000-000000000001"), "SYKEPENGEGRUNNLAG_FOR_ARBEIDSGIVER"),
        hendelse(UUID.fromString("00000000-0000-0000-0000-000000000002"), "HVA_SOM_HELST"),
        //"00000000-0000-0000-0000-000000000003" er utelatt med overlegg
        hendelse(UUID.fromString("00000000-0000-0000-0000-000000000004"), "SYKEPENGEGRUNNLAG_FOR_ARBEIDSGIVER")
    )}

    private fun hendelse(meldingsreferanseId: UUID, meldingstype: String) = meldingsreferanseId to Hendelse(
        meldingsreferanseId = meldingsreferanseId,
        meldingstype = meldingstype,
        lestDato = LocalDateTime.now(),
    )
}
