package no.nav.helse.spleis.e2e

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.helse.testhelpers.januar
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.felles.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class UtbetalingkontraktTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `utbetaling utbetalt`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()
        val utbetalt = testRapid.inspektør.siste("utbetaling_utbetalt")
        assertUtbetalt(utbetalt)
    }

    @Test
    fun annullering() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()
        sendAnnullering(testRapid.inspektør.etterspurteBehov(Utbetaling).path(Utbetaling.name).path("fagsystemId").asText())
        sendUtbetaling()
        val utbetalt = testRapid.inspektør.siste("utbetaling_annullert")
        assertAnnullert(utbetalt)
    }

    private fun assertUtbetalt(melding: JsonNode) {
        assertTrue(melding.path("utbetalingId").asText().isNotEmpty())
        assertTrue(melding.path("type").asText().isNotEmpty())
        assertDato(melding.path("fom").asText())
        assertDato(melding.path("tom").asText())
        assertDato(melding.path("maksdato").asText())
        assertTrue(melding.path("forbrukteSykedager").isInt)
        assertTrue(melding.path("gjenståendeSykedager").isInt)
        assertTrue(melding.path("ident").asText().isNotEmpty())
        assertTrue(melding.path("epost").asText().isNotEmpty())
        assertDatotid(melding.path("tidspunkt").asText())
        assertTrue(melding.path("automatiskBehandling").isBoolean)
        assertOppdragdetaljer(melding.path("arbeidsgiverOppdrag"), false)
        assertOppdragdetaljer(melding.path("personOppdrag"), false)
    }

    private fun assertAnnullert(melding: JsonNode) {
        assertTrue(melding.path("utbetalingId").asText().isNotEmpty())
        assertTrue(melding.path("fagsystemId").asText().isNotEmpty())
        assertDato(melding.path("fom").asText())
        assertDato(melding.path("tom").asText())
        assertDatotid(melding.path("annullertAvSaksbehandler").asText())
        assertTrue(melding.path("saksbehandlerEpost").asText().isNotEmpty())
        assertTrue(melding.path("utbetalingslinjer").isArray)
        assertFalse(melding.path("utbetalingslinjer").isEmpty)
        melding.path("utbetalingslinjer").onEach {
            assertDato(it.path("fom").asText())
            assertDato(it.path("tom").asText())
        }
    }

    private fun assertOppdragdetaljer(oppdrag: JsonNode, erAnnullering: Boolean) {
        assertTrue(oppdrag.path("mottaker").asText().isNotEmpty())
        assertTrue(oppdrag.path("fagsystemId").asText().isNotEmpty())
        assertTrue(oppdrag.path("fagområde").asText().isNotEmpty())
        assertTrue(oppdrag.path("endringskode").asText().isNotEmpty())
        assertTrue(oppdrag.path("stønadsdager").isInt)
        assertDato(oppdrag.path("fom").asText())
        assertDato(oppdrag.path("tom").asText())
        if (erAnnullering) {
            assertEquals(0, oppdrag.path("stønadsdager").asInt())
        }
        val linjer = oppdrag.path("linjer")
        assertTrue(linjer.isArray)
        linjer.forEach { linje ->
            assertDato(linje.path("fom").asText())
            assertDato(linje.path("tom").asText())
            assertTrue(linje.path("dagsats").isInt)
            assertTrue(linje.path("lønn").isInt)
            assertTrue(linje.path("grad").isDouble)
            assertTrue(linje.path("stønadsdager").isInt)
            assertTrue(linje.path("totalbeløp").isInt)
            if (erAnnullering) {
                assertEquals(0, linje.path("stønadsdager").asInt())
                assertEquals(0, linje.path("totalbeløp").asInt())
            }
            assertTrue(linje.path("delytelseId").isInt)
            assertTrue(linje.has("refFagsystemId"))
            assertTrue(linje.has("refDelytelseId"))
            if (!erAnnullering) {
                assertTrue(linje.has("datoStatusFom"))
                assertTrue(linje.has("statuskode"))
            } else {
                assertDato(linje.path("datoStatusFom").asText())
                assertTrue(linje.path("statuskode").asText().isNotEmpty())
            }
            assertTrue(linje.path("endringskode").asText().isNotEmpty())
            assertTrue(linje.path("klassekode").asText().isNotEmpty())
        }
    }

    private fun assertDato(tekst: String) {
        assertTrue(tekst.isNotEmpty())
        assertDoesNotThrow { LocalDate.parse(tekst) }
    }

    private fun assertDatotid(tekst: String) {
        assertTrue(tekst.isNotEmpty())
        assertDoesNotThrow { LocalDateTime.parse(tekst) }
    }
}


