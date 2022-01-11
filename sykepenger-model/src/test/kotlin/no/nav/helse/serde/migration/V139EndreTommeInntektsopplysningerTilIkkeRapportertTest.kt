package no.nav.helse.serde.migration

import no.nav.helse.readResource
import no.nav.helse.serde.assertJsonEquals
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class V139EndreTommeInntektsopplysningerTilIkkeRapportertTest : MigrationTest(V139EndreTommeInntektsopplysningerTilIkkeRapportert()) {
    @Test
    fun `legger på påkrevde felter for tomme inntektsopplysninger`() {
        val original = toNode("/migrations/139/original.json".readResource())
        val migrert = migrer("/migrations/139/original.json".readResource())

        val originalHistorikk = original["vilkårsgrunnlagHistorikk"].toList()
        val migrertHistorikk = migrert["vilkårsgrunnlagHistorikk"].toList()

        val nyesteOriginaleArbeidsgiverInntekter = originalHistorikk[0]["vilkårsgrunnlag"][0]["sykepengegrunnlag"]["arbeidsgiverInntektsopplysninger"]
        val nyesteMigrerteArbeidsgiverInntekter = migrertHistorikk[0]["vilkårsgrunnlag"][0]["sykepengegrunnlag"]["arbeidsgiverInntektsopplysninger"]

        assertJsonEquals(
            nyesteOriginaleArbeidsgiverInntekter[0].toString(),
            nyesteMigrerteArbeidsgiverInntekter[0].toString()
        )

        val ikkeRapportertInntektsopplysning = nyesteMigrerteArbeidsgiverInntekter[1]["inntektsopplysning"]
        assertEquals("IKKE_RAPPORTERT", ikkeRapportertInntektsopplysning["kilde"].asText())
        assertEquals(1.januar, LocalDate.parse(ikkeRapportertInntektsopplysning["dato"].asText()))
        assertDoesNotThrow { UUID.fromString(ikkeRapportertInntektsopplysning["id"].asText()) }
        assertDoesNotThrow { LocalDateTime.parse(ikkeRapportertInntektsopplysning["tidsstempel"].asText()) }

        assertJsonEquals(
            originalHistorikk[0]["vilkårsgrunnlag"][1],
            migrertHistorikk[0]["vilkårsgrunnlag"][1]
        )

        assertJsonEquals(originalHistorikk[1].toString(), migrertHistorikk[1].toString())
    }

}
