package no.nav.helse.spleis.mediator.e2e

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.spleis.mediator.TestMessageFactory.UtbetalingshistorikkTestdata
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class InfotrygdendringTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `sender infotrygdendring`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendInfotrygdendring()
        val behov = testRapid.inspektør.melding(testRapid.inspektør.antall() - 1)
        assertBehov(behov)
        assertSykepengehistorikkdetaljer(behov)
    }

    @Test
    fun `sender infotrygdendring uten at vi vet om person fra før`() {
        sendInfotrygdendring()
        assertTrue(testRapid.inspektør.antall() == 0)
    }

    @Test
    fun `utgående melding om overlappende infotrygdutbetaling`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(
                SoknadsperiodeDTO(
                    fom = 3.januar,
                    tom = 26.januar,
                    sykmeldingsgrad = 100
                )
            )
        )
        sendInfotrygdendring()
        sendUtbetalingshistorikkEtterInfotrygdendring(
            listOf(
                UtbetalingshistorikkTestdata(
                    fom = 3.januar,
                    tom = 26.januar,
                    arbeidskategorikode = "01",
                    utbetalteSykeperioder = listOf(
                        UtbetalingshistorikkTestdata.UtbetaltSykeperiode(
                            fom = 3.januar,
                            tom = 26.januar,
                            dagsats = 1400.0,
                            typekode = "0",
                            utbetalingsgrad = "100",
                            organisasjonsnummer = ORGNUMMER
                        )
                    ),
                    inntektsopplysninger = emptyList()
                )
            )
        )
        val overlappendeInfotrygdperiodeEtterInfotrygdendringEvent =
            testRapid.inspektør.siste("overlappende_infotrygdperioder")
        assertNotNull(overlappendeInfotrygdperiodeEtterInfotrygdendringEvent)
    }

    private fun assertSykepengehistorikkdetaljer(behov: JsonNode) {
        assertDato(
            behov.path(Aktivitet.Behov.Behovtype.Sykepengehistorikk.name).path("historikkFom")
                .asText()
        )
        assertDato(
            behov.path(Aktivitet.Behov.Behovtype.Sykepengehistorikk.name).path("historikkTom")
                .asText()
        )
    }

    private fun assertDato(tekst: String) {
        assertTrue(tekst.isNotEmpty())
        Assertions.assertDoesNotThrow { LocalDate.parse(tekst) }
    }

    private fun assertDatotid(tekst: String) {
        assertTrue(tekst.isNotEmpty())
        Assertions.assertDoesNotThrow { LocalDateTime.parse(tekst) }
    }

    private fun assertBehov(behov: JsonNode) {
        val id = behov.path("@id").asText()
        assertEquals("behov", behov.path("@event_name").asText())
        assertTrue(behov.path("fødselsnummer").asText().isNotEmpty())
        assertTrue(behov.path("@behov").isArray)
        assertDatotid(behov.path("@opprettet").asText())
        assertTrue(id.isNotEmpty())
        Assertions.assertDoesNotThrow { UUID.fromString(id) }
        assertEquals(
            Aktivitet.Behov.Behovtype.Sykepengehistorikk.name,
            behov.path("@behov").firstOrNull()?.asText()
        )
    }

}
