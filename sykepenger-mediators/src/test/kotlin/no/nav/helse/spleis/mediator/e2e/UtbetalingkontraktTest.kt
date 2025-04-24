package no.nav.helse.spleis.mediator.e2e

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.februar
import no.nav.helse.flex.sykepengesoknad.kafka.FravarDTO
import no.nav.helse.flex.sykepengesoknad.kafka.FravarstypeDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.spleis.mediator.e2e.KontraktAssertions.assertOgFjern
import no.nav.helse.spleis.mediator.e2e.KontraktAssertions.assertUtgåendeMelding
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.inntektsmeldingkontrakt.Periode
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class UtbetalingkontraktTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `ny utbetaling`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        )
        sendInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        val utbetalingEndret = testRapid.inspektør.siste("utbetaling_endret")
        assertUtbetalingEndret(utbetalingEndret, "NY", "IKKE_UTBETALT")
        @Language("JSON")
        val forventet = """
           {
              "@event_name": "vedtaksperiode_ny_utbetaling", 
              "fødselsnummer": "12029240045",
              "organisasjonsnummer": "987654321",
              "vedtaksperiodeId": "<uuid>",
              "utbetalingId": "<uuid>"
           }
        """
        testRapid.assertUtgåendeMelding(forventet)
    }

    @Test
    fun `utbetaling utbetalt`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        )
        sendInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()
        assertUtbetalingUtbetalt(utbetalingUtbetaltForventetJson)
        val utbetalingEndret = testRapid.inspektør.siste("utbetaling_endret")
        assertUtbetalingEndret(utbetalingEndret, "OVERFØRT", "UTBETALT")
    }

    @Test
    fun `manuell behandling`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        )
        sendInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(
            vedtaksperiodeIndeks = 0,
            automatiskBehandling = false
        )
        sendUtbetaling()
        val utbetaltEvent = testRapid.inspektør.siste("utbetaling_utbetalt")
        assertFalse(utbetaltEvent["automatiskBehandling"].booleanValue())
    }

    @Test
    fun `automatisk behandling`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        )
        sendInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(
            vedtaksperiodeIndeks = 0,
            automatiskBehandling = true
        )
        sendUtbetaling()
        val utbetaltEvent = testRapid.inspektør.siste("utbetaling_utbetalt")
        assertTrue(utbetaltEvent["automatiskBehandling"].booleanValue())
    }

    @Test
    fun `arbeid ikke gjenopptatt`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100))
        sendSøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(listOf(Periode(fom = 1.januar, tom = 16.januar)), førsteFraværsdag = 1.januar)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(vedtaksperiodeIndeks = 0, automatiskBehandling = true)
        sendUtbetaling()

        sendNySøknad(SoknadsperiodeDTO(fom = 1.mars, tom = 31.mars, sykmeldingsgrad = 100))
        sendSøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.mars, tom = 31.mars, sykmeldingsgrad = 100)))
        sendInntektsmelding(listOf(Periode(fom = 1.mars, tom = 16.mars)), førsteFraværsdag = 1.mars)
        sendVilkårsgrunnlag(1)
        sendYtelser(1)
        sendSimulering(1, SimuleringMessage.Simuleringstatus.OK)

        sendOverstyringTidslinje((1.februar til 28.februar).map { ManuellOverskrivingDag(it, Dagtype.ArbeidIkkeGjenopptattDag) })
        sendYtelser(1)
        sendSimulering(1, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(vedtaksperiodeIndeks = 1, automatiskBehandling = true)
        sendUtbetaling()

        val utbetaltEvent = testRapid.inspektør.siste("utbetaling_utbetalt")
        val utbetalingsdager = utbetaltEvent["utbetalingsdager"].associate { it.path("dato").asLocalDate() to it.path("type").asText() }
        (1.februar til 28.februar).forEach { assertEquals("ArbeidIkkeGjenopptattDag", utbetalingsdager[it]) }
    }

    @Test
    fun `spleis sender korrekt grad (avrundet) ut`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 30))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 30, faktiskGrad = 80))
        )
        sendInntektsmelding(listOf(Periode(fom = 1.januar, tom = 16.januar)), førsteFraværsdag = 1.januar)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0, true)
        sendUtbetaling()
        val utbetaling = testRapid.inspektør.siste("utbetaling_utbetalt")
        assertEquals(20.0, utbetaling.path("arbeidsgiverOppdrag").path("linjer").first().path("grad").asDouble())
    }

    @Test
    fun `Feriedager og permisjonsdager blir mappet riktig fra utbetalingstidslinjen for utbetaling_utbetalt`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
            fravær = listOf(
                FravarDTO(fom = 20.januar, tom = 21.januar, type = FravarstypeDTO.FERIE),
                FravarDTO(fom = 22.januar, tom = 22.januar, type = FravarstypeDTO.PERMISJON)
            )
        )
        sendInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()
        val utbetaling = testRapid.inspektør.siste("utbetaling_utbetalt")

        assertEquals(2, utbetaling.path("utbetalingsdager").toList().filter { it["type"].asText() == "Feriedag" }.size)
        assertEquals(1, utbetaling.path("utbetalingsdager").toList().filter { it["type"].asText() == "Permisjonsdag" }.size)
    }
    
    @Test
    fun `utbetaling med avviste dager`() {
        sendNySøknad(
            SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100),
            SoknadsperiodeDTO(fom = 27.januar, tom = 30.januar, sykmeldingsgrad = 15)
        )
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100), SoknadsperiodeDTO(fom = 27.januar, tom = 30.januar, sykmeldingsgrad = 15))
        )
        sendInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()
        val utbetalt = testRapid.inspektør.siste("utbetaling_utbetalt")
        assertUtbetaltInkluderAvviste(utbetalt)
    }

    @Test
    fun `utbetaling uten utbetaling`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        )
        sendInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()

        sendNySøknad(SoknadsperiodeDTO(fom = 27.januar, tom = 31.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 27.januar, tom = 31.januar, sykmeldingsgrad = 100)),
            fravær = listOf(FravarDTO(fom = 27.januar, tom = 31.januar, FravarstypeDTO.FERIE))
        )
        sendYtelser(1)
        sendUtbetalingsgodkjenning(1)
        val utbetalt = testRapid.inspektør.siste("utbetaling_uten_utbetaling")
        assertUtbetalt(utbetalt)
        val utbetalingEndret = testRapid.inspektør.siste("utbetaling_endret")
        assertUtbetalingEndret(utbetalingEndret, "IKKE_UTBETALT", "GODKJENT_UTEN_UTBETALING")
    }

    @Test
    fun `annullering full refusjon`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        )
        sendInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()
        sendAnnullering(utbetalingId.toString())
        sendUtbetaling()
        @Language("JSON")
        val forventet = """
            {
                "@event_name": "utbetaling_annullert",
                "fødselsnummer": "12029240045",
                "organisasjonsnummer": "987654321",
                "epost" : "siri.saksbehandler@nav.no",
                "ident" : "S1234567",
                "fom" : "2018-01-03",
                "tom" : "2018-01-26",
                "arbeidsgiverFagsystemId": "$arbeidsgiverFagsystemId",
                "personFagsystemId": "$personFagsystemId",
                "korrelasjonsId": "$korrelasjonsId",
                "tidspunkt": "<timestamp>",
                "utbetalingId": "<uuid>"
            }
        """
        testRapid.assertUtgåendeMelding(forventet)
        val utbetalingEndret = testRapid.inspektør.siste("utbetaling_endret")
        assertUtbetalingEndret(utbetalingEndret, "OVERFØRT", "ANNULLERT", true)
    }

    @Test
    fun `annullering delvis refusjon`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        )
        sendInntektsmelding(
            listOf(Periode(fom = 3.januar, tom = 18.januar)),
            førsteFraværsdag = 3.januar,
            opphørsdatoForRefusjon = 20.januar
        )
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK, forventedeFagområder = setOf("SPREF", "SP"))
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()
        sendAnnullering(utbetalingId.toString())
        sendUtbetaling()
        @Language("JSON")
        val forventet = """
            {
                "@event_name": "utbetaling_annullert",
                "fødselsnummer": "12029240045",
                "organisasjonsnummer": "987654321",
                "epost" : "siri.saksbehandler@nav.no",
                "ident" : "S1234567",
                "fom" : "2018-01-03",
                "tom" : "2018-01-26",
                "arbeidsgiverFagsystemId": "$arbeidsgiverFagsystemId",
                "personFagsystemId": "$personFagsystemId",
                "korrelasjonsId": "$korrelasjonsId",
                "tidspunkt": "<timestamp>",
                "utbetalingId": "<uuid>"
            }
        """
        testRapid.assertUtgåendeMelding(forventet)
    }

    @Test
    fun `annullering ingen refusjon`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        )
        sendInntektsmelding(
            listOf(Periode(fom = 3.januar, tom = 18.januar)),
            førsteFraværsdag = 3.januar,
            opphørsdatoForRefusjon = 3.januar
        )
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK, forventedeFagområder = setOf("SP"))
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()
        sendAnnullering(utbetalingId.toString())
        sendUtbetaling()
        @Language("JSON")
        val forventet = """
            {
                "@event_name": "utbetaling_annullert",
                "fødselsnummer": "12029240045",
                "organisasjonsnummer": "987654321",
                "epost" : "siri.saksbehandler@nav.no",
                "ident" : "S1234567",
                "fom" : "2018-01-03",
                "tom" : "2018-01-26",
                "arbeidsgiverFagsystemId": "$arbeidsgiverFagsystemId",
                "personFagsystemId": "$personFagsystemId",
                "korrelasjonsId": "$korrelasjonsId",
                "tidspunkt": "<timestamp>",
                "utbetalingId": "<uuid>"
            }
        """
        testRapid.assertUtgåendeMelding(forventet)
    }

    private fun assertUtbetaltInkluderAvviste(melding: JsonNode) {
        assertUtbetalt(melding)
        melding.path("utbetalingsdager").toList().filter { it["type"].asText() == "AvvistDag" }.also { avvisteDager ->
            assertTrue(avvisteDager.isNotEmpty())
            assertTrue(avvisteDager.all { it.hasNonNull("begrunnelser") })
        }
    }

    private fun assertUtbetalingEndret(melding: JsonNode, fra: String, til: String, annullering: Boolean = false) {
        assertTrue(melding.path("fødselsnummer").asText().isNotEmpty())
        assertTrue(melding.path("organisasjonsnummer").asText().isNotEmpty())
        assertTrue(melding.path("utbetalingId").asText().isNotEmpty())
        assertTrue(melding.path("korrelasjonsId").asText().isNotEmpty())
        assertTrue(melding.path("type").asText().isNotEmpty())
        assertTrue(melding.path("forrigeStatus").asText().isNotEmpty())
        assertTrue(melding.path("gjeldendeStatus").asText().isNotEmpty())
        assertEquals(fra, melding.path("forrigeStatus").asText())
        assertEquals(til, melding.path("gjeldendeStatus").asText())
        assertOppdragdetaljerEnkel(melding.path("arbeidsgiverOppdrag"))
        assertOppdragdetaljerEnkel(melding.path("personOppdrag"))
    }

    private fun assertUtbetalt(melding: JsonNode) {
        assertTrue(melding.path("fødselsnummer").asText().isNotEmpty())
        assertTrue(melding.path("organisasjonsnummer").asText().isNotEmpty())
        assertTrue(melding.path("utbetalingId").asText().isNotEmpty())
        assertTrue(melding.path("korrelasjonsId").asText().isNotEmpty())
        assertTrue(melding.path("type").asText().isNotEmpty())
        assertDato(melding.path("fom").asText())
        assertDato(melding.path("tom").asText())
        assertDato(melding.path("maksdato").asText())
        assertTrue(melding.path("forbrukteSykedager").isInt)
        assertTrue(melding.path("gjenståendeSykedager").isInt)
        assertTrue(melding.path("stønadsdager").isInt)
        assertTrue(melding.path("ident").asText().isNotEmpty())
        assertTrue(melding.path("epost").asText().isNotEmpty())
        assertTrue(melding.path("utbetalingsdager").toList().isNotEmpty())
        assertDatotid(melding.path("tidspunkt").asText())
        assertTrue(melding.path("automatiskBehandling").isBoolean)
        assertOppdragdetaljer(melding.path("arbeidsgiverOppdrag"), false)
        assertOppdragdetaljer(melding.path("personOppdrag"), false)
    }

    private fun assertOppdragdetaljerEnkel(oppdrag: JsonNode) {
        assertTrue(oppdrag.path("fagsystemId").asText().isNotEmpty())
        assertTrue(oppdrag.path("mottaker").asText().isNotEmpty())
        assertTrue(oppdrag.path("nettoBeløp").isInt)
        val linjer = oppdrag.path("linjer")
        assertTrue(linjer.isArray)
        linjer.forEach { linje ->
            assertDato(linje.path("fom").asText())
            assertDato(linje.path("tom").asText())
            assertTrue(linje.path("totalbeløp").isInt)
        }
    }

    private fun assertOppdragdetaljer(oppdrag: JsonNode, erAnnullering: Boolean) {
        assertTrue(oppdrag.path("mottaker").asText().isNotEmpty())
        assertTrue(oppdrag.path("fagsystemId").asText().isNotEmpty())
        assertTrue(oppdrag.path("fagområde").asText().isNotEmpty())
        assertTrue(oppdrag.path("nettoBeløp").isInt)
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
            assertTrue(linje.path("sats").isInt)
            assertTrue(linje.path("grad").isDouble)
            assertTrue(linje.path("stønadsdager").isInt)
            assertTrue(linje.path("totalbeløp").isInt)
            if (erAnnullering) {
                assertEquals(0, linje.path("stønadsdager").asInt())
                assertEquals(0, linje.path("totalbeløp").asInt())
            }
            if (!erAnnullering) {
                assertTrue(linje.has("statuskode"))
            } else {
                assertTrue(linje.path("statuskode").asText().isNotEmpty())
            }
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

    private fun assertUtbetalingUtbetalt(forventetMelding: String) {
        testRapid.assertUtgåendeMelding(forventetMelding) {
            it.assertOgFjernFagsystemId("arbeidsgiverOppdrag.fagsystemId")
            it.assertOgFjernFagsystemId("personOppdrag.fagsystemId")
        }
    }

    private val arbeidsgiverFagsystemId
        get() = testRapid.inspektør.siste("utbetaling_utbetalt").path("arbeidsgiverOppdrag").path("fagsystemId").asText().also {
            check(
                it.matches(
                    FagsystemIdRegex
                )
            )
        }
    private val personFagsystemId
        get() = testRapid.inspektør.siste("utbetaling_utbetalt").path("personOppdrag").path("fagsystemId").asText().also {
            check(
                it.matches(
                    FagsystemIdRegex
                )
            )
        }
    private val utbetalingId get() = testRapid.inspektør.siste("utbetaling_utbetalt").path("utbetalingId").let { UUID.fromString(it.asText()) }
    private val korrelasjonsId get() = testRapid.inspektør.siste("utbetaling_utbetalt").path("korrelasjonsId").let { UUID.fromString(it.asText()) }


    private companion object {
        private val FagsystemIdRegex = "[A-Z,2-7]{26}".toRegex()
        private fun ObjectNode.assertOgFjernFagsystemId(key: String) {
            assertOgFjern(key) { check(it.asText().matches(FagsystemIdRegex)) }
        }

        @Language("JSON")
        private val utbetalingUtbetaltForventetJson = """
        {
            "@event_name": "utbetaling_utbetalt",
            "korrelasjonsId": "<uuid>",
            "utbetalingId": "<uuid>",
            "tidspunkt": "<timestamp>",
            "organisasjonsnummer": "987654321",
            "type": "UTBETALING",
            "fom": "2018-01-03",
            "tom": "2018-01-26",
            "maksdato": "2019-01-01",
            "forbrukteSykedager": 6,
            "gjenståendeSykedager": 242,
            "stønadsdager": 6,
            "ident": "O123456",
            "epost": "jan@banan.no",
            "automatiskBehandling": false,
            "arbeidsgiverOppdrag": {
                "mottaker": "987654321",
                "fagområde": "SPREF",
                "linjer": [{
                    "fom": "2018-01-19",
                    "tom": "2018-01-26",
                    "sats": 1431,
                    "grad": 100.0,
                    "stønadsdager": 6,
                    "totalbeløp": 8586,
                    "statuskode": null
                }],
                "nettoBeløp": 8586,
                "stønadsdager": 6,
                "fom": "2018-01-19",
                "tom": "2018-01-26"
            },
            "personOppdrag": {
                "mottaker": "12029240045",
                "fagområde": "SP",
                "linjer": [],
                "nettoBeløp": 0,
                "stønadsdager": 0,
                "fom": "-999999999-01-01",
                "tom": "-999999999-01-01"
            },
            "utbetalingsdager": [{
                "dato": "2018-01-03",
                "type": "ArbeidsgiverperiodeDag",
                "begrunnelser": null
            }, {
                "dato": "2018-01-04",
                "type": "ArbeidsgiverperiodeDag",
                "begrunnelser": null
            }, {
                "dato": "2018-01-05",
                "type": "ArbeidsgiverperiodeDag",
                "begrunnelser": null
            }, {
                "dato": "2018-01-06",
                "type": "ArbeidsgiverperiodeDag",
                "begrunnelser": null
            }, {
                "dato": "2018-01-07",
                "type": "ArbeidsgiverperiodeDag",
                "begrunnelser": null
            }, {
                "dato": "2018-01-08",
                "type": "ArbeidsgiverperiodeDag",
                "begrunnelser": null
            }, {
                "dato": "2018-01-09",
                "type": "ArbeidsgiverperiodeDag",
                "begrunnelser": null
            }, {
                "dato": "2018-01-10",
                "type": "ArbeidsgiverperiodeDag",
                "begrunnelser": null
            }, {
                "dato": "2018-01-11",
                "type": "ArbeidsgiverperiodeDag",
                "begrunnelser": null
            }, {
                "dato": "2018-01-12",
                "type": "ArbeidsgiverperiodeDag",
                "begrunnelser": null
            }, {
                "dato": "2018-01-13",
                "type": "ArbeidsgiverperiodeDag",
                "begrunnelser": null
            }, {
                "dato": "2018-01-14",
                "type": "ArbeidsgiverperiodeDag",
                "begrunnelser": null
            }, {
                "dato": "2018-01-15",
                "type": "ArbeidsgiverperiodeDag",
                "begrunnelser": null
            }, {
                "dato": "2018-01-16",
                "type": "ArbeidsgiverperiodeDag",
                "begrunnelser": null
            }, {
                "dato": "2018-01-17",
                "type": "ArbeidsgiverperiodeDag",
                "begrunnelser": null
            }, {
                "dato": "2018-01-18",
                "type": "ArbeidsgiverperiodeDag",
                "begrunnelser": null
            }, {
                "dato": "2018-01-19",
                "type": "NavDag",
                "begrunnelser": null
            }, {
                "dato": "2018-01-20",
                "type": "NavHelgDag",
                "begrunnelser": null
            }, {
                "dato": "2018-01-21",
                "type": "NavHelgDag",
                "begrunnelser": null
            }, {
                "dato": "2018-01-22",
                "type": "NavDag",
                "begrunnelser": null
            }, {
                "dato": "2018-01-23",
                "type": "NavDag",
                "begrunnelser": null
            }, {
                "dato": "2018-01-24",
                "type": "NavDag",
                "begrunnelser": null
            }, {
                "dato": "2018-01-25",
                "type": "NavDag",
                "begrunnelser": null
            }, {
                "dato": "2018-01-26",
                "type": "NavDag",
                "begrunnelser": null
            }],
            "fødselsnummer": "12029240045"
        }
    """
    }
}


