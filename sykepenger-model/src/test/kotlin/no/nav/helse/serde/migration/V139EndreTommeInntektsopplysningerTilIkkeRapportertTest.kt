package no.nav.helse.serde.migration

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.readResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

internal class V139EndreTommeInntektsopplysningerTilIkkeRapportertTest : MigrationTest(V139EndreTommeInntektsopplysningerTilIkkeRapportert()) {
    @Test
    fun `legger på påkrevde felter for tomme inntektsopplysninger`() {
        val original = toNode("/migrations/139/original.json".readResource())
        val migrert = migrer("/migrations/139/original.json".readResource())

        val originalHistorikk = original["vilkårsgrunnlagHistorikk"].toList()
        val migrertHistorikk = migrert["vilkårsgrunnlagHistorikk"].toList()

        val nyesteOriginaleArbeidsgiverInntekter = originalHistorikk[0]["vilkårsgrunnlag"][0]["sykepengegrunnlag"]["arbeidsgiverInntektsopplysninger"]
        val nyesteMigrerteArbeidsgiverInntekter = migrertHistorikk[0]["vilkårsgrunnlag"][0]["sykepengegrunnlag"]["arbeidsgiverInntektsopplysninger"]

        JSONAssert.assertEquals(nyesteOriginaleArbeidsgiverInntekter[0].toString(), nyesteMigrerteArbeidsgiverInntekter[0].toString(), JSONCompareMode.STRICT)

        val ikkeRapportertInntektsopplysning = nyesteMigrerteArbeidsgiverInntekter[1]["inntektsopplysning"]
        assertEquals("IKKE_RAPPORTERT", ikkeRapportertInntektsopplysning["kilde"].asText())
        assertEquals(1.januar, LocalDate.parse(ikkeRapportertInntektsopplysning["dato"].asText()))
        assertDoesNotThrow { UUID.fromString(ikkeRapportertInntektsopplysning["id"].asText()) }
        assertDoesNotThrow { LocalDateTime.parse(ikkeRapportertInntektsopplysning["tidsstempel"].asText()) }

        JSONAssert.assertEquals(originalHistorikk[0]["vilkårsgrunnlag"][1].toString(), migrertHistorikk[0]["vilkårsgrunnlag"][1].toString(), JSONCompareMode.STRICT)
        JSONAssert.assertEquals(originalHistorikk[1].toString(), migrertHistorikk[1].toString(), JSONCompareMode.STRICT)
    }

}
