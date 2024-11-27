package no.nav.helse.serde.migration

import no.nav.helse.serde.migration.JsonMigration.Companion.uuid
import no.nav.helse.serde.migration.V311AvsenderOgTidsstempelPåRefusjonsopplysning.Companion.avsender
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class V311AvsenderOgTidsstempelPåRefusjonsopplysningTest : MigrationTest(V311AvsenderOgTidsstempelPåRefusjonsopplysning()) {
    override fun meldingerSupplier() =
        MeldingerSupplier {
            mapOf(
                UUID.fromString("75d708d5-e2b1-4ff4-9426-976921505925") to
                    Hendelse(
                        "75d708d5-e2b1-4ff4-9426-976921505925".uuid,
                        "INNTEKTSMELDING",
                        LocalDateTime.parse("2024-11-15T12:47:09.829616"),
                    ),
            )
        }

    @Test
    fun `migrerer avsender og tidsstempel på refusjonsopplysning`() {
        assertMigration("/migrations/311/expected.json", "/migrations/311/original.json")
    }

    @Test
    fun `mapper meldingstype til avsender`() {
        assertEquals("ARBEIDSGIVER", "INNTEKTSMELDING".avsender)
        assertEquals("SAKSBEHANDLER", "OVERSTYRARBEIDSGIVEROPPLYSNINGER".avsender)
        assertEquals("SAKSBEHANDLER", "OVERSTYRINNTEKT".avsender)
        assertEquals("SAKSBEHANDLER", "GJENOPPLIV_VILKÅRSGRUNNLAG".avsender)
        assertEquals("ARBEIDSGIVER", "SENDT_SØKNAD_NAV".avsender)
    }
}
