package no.nav.helse.spleis.mediator.e2e

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidssituasjonDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.spleis.Behov
import no.nav.helse.spleis.Behov.Behovstype.Arbeidsavklaringspenger
import no.nav.helse.spleis.Behov.Behovstype.Arbeidsforhold
import no.nav.helse.spleis.Behov.Behovstype.Dagpenger
import no.nav.helse.spleis.Behov.Behovstype.Foreldrepenger
import no.nav.helse.spleis.Behov.Behovstype.Godkjenning
import no.nav.helse.spleis.Behov.Behovstype.InntekterForBeregning
import no.nav.helse.spleis.Behov.Behovstype.InntekterForOpptjeningsvurdering
import no.nav.helse.spleis.Behov.Behovstype.InntekterForSykepengegrunnlag
import no.nav.helse.spleis.Behov.Behovstype.Institusjonsopphold
import no.nav.helse.spleis.Behov.Behovstype.Medlemskap
import no.nav.helse.spleis.Behov.Behovstype.Omsorgspenger
import no.nav.helse.spleis.Behov.Behovstype.Opplæringspenger
import no.nav.helse.spleis.Behov.Behovstype.Pleiepenger
import no.nav.helse.spleis.Behov.Behovstype.Simulering
import no.nav.helse.spleis.Behov.Behovstype.Sykepengehistorikk
import no.nav.helse.spleis.Behov.Behovstype.Utbetaling
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.inntektsmeldingkontrakt.Periode
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class BehovkontraktTest : AbstractEndToEndMediatorTest() {

    @Test
    fun vilkårsgrunnlag() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        )
        sendInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        val behov = testRapid.inspektør.etterspurteBehov(Medlemskap)
        assertVedtaksperiodeBehov(
            behov,
            Dagpenger,
            InntekterForSykepengegrunnlag,
            InntekterForOpptjeningsvurdering,
            Medlemskap,
            Arbeidsforhold
        )
        assertMedlemskapdetaljer(behov)
        assertInntekterForSykepengegrunnlagdetaljer(behov)
        assertInntekterForOpptjeningsvurderingdetaljer(behov)
        assertArbeidsforholdV2detaljer(behov)
    }

    @Test
    fun ytelser() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        )
        val utbetalinghistorikkbehov = testRapid.inspektør.meldinger("behov").last()
        assertVedtaksperiodeBehov(utbetalinghistorikkbehov, Sykepengehistorikk)
        assertSykepengehistorikkdetaljer(utbetalinghistorikkbehov)
        sendInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendVilkårsgrunnlag(0)
        val behov = testRapid.inspektør.etterspurteBehov(InntekterForBeregning)
        assertVedtaksperiodeBehov(
            behov,
            Arbeidsavklaringspenger,
            Dagpenger,
            Foreldrepenger,
            Institusjonsopphold,
            Omsorgspenger,
            Opplæringspenger,
            Pleiepenger,
            InntekterForBeregning
        )
        assertArbeidsavklaringspengerdetaljer(behov)
        assertDagpengerdetaljer(behov)
        assertForeldrepengerdetaljer(behov)
        assertInstitusjonsoppholddetaljer(behov)
        assertOmsorgspengerdetaljer(behov)
        assertOpplæringspengerdetaljer(behov)
        assertPleiepengerdetaljer(behov)
        assertInntekterForBeregning(behov)
    }

    @Test
    fun simulering() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        )
        sendInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        val behov = testRapid.inspektør.etterspurteBehov(Simulering)
        assertVedtaksperiodeBehov(behov, Simulering)
        assertSimuleringdetaljer(behov)
    }

    @Test
    fun `godkjenning - selvstendig`() {
        sendNySøknadSelvstendig(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100), arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        sendSelvstendigsøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
            ventetid = 3.januar til 18.januar,
            arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE
        )

        sendVilkårsgrunnlagSelvstendig(0)
        sendYtelserSelvstendig(0)
        sendSimuleringSelvstendig(0, SimuleringMessage.Simuleringstatus.OK)
        val behov = testRapid.inspektør.etterspurteBehov(Godkjenning)
        assertVedtaksperiodeBehov(behov, Godkjenning)
        assertGodkjenningdetaljer(behov, erSelvstendig = true, "SELVSTENDIG_NÆRINGSDRIVENDE")
    }

    @Test
    fun godkjenning() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        )
        sendInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        val behov = testRapid.inspektør.etterspurteBehov(Godkjenning)
        assertVedtaksperiodeBehov(behov, Godkjenning)
        assertGodkjenningdetaljer(behov, erSelvstendig = false, "ARBEIDSTAKER")
    }

    @Test
    fun utbetaling() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        )
        sendInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        val behov = testRapid.inspektør.etterspurteBehov(Utbetaling)
        assertUtbetalingBehov(behov, Utbetaling)
        assertUtbetalingdetaljer(behov)
    }

    @Test
    fun annullering() {
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
        sendAnnullering(0)
        val behov = testRapid.inspektør.etterspurteBehov(Utbetaling)
        assertUtbetalingBehov(behov, Utbetaling)
        assertUtbetalingdetaljer(behov, true)
    }

    private fun assertVedtaksperiodeBehov(behov: JsonNode, vararg typer: Behov.Behovstype) {
        assertBehov(behov, *typer)
        assertTrue(behov.path("vedtaksperiodeId").asText().isNotEmpty())
    }

    private fun assertUtbetalingBehov(behov: JsonNode, vararg typer: Behov.Behovstype) {
        assertBehov(behov, *typer)
        assertTrue(behov.path("utbetalingId").asText().isNotEmpty())
    }

    private fun assertBehov(behov: JsonNode, vararg typer: Behov.Behovstype) {
        val id = behov.path("@id").asText()
        assertEquals("behov", behov.path("@event_name").asText())
        assertTrue(behov.path("fødselsnummer").asText().isNotEmpty())
        assertTrue(behov.path("organisasjonsnummer").asText().isNotEmpty())
        assertTrue(behov.path("@behov").isArray)
        assertDatotid(behov.path("@opprettet").asText())
        assertTrue(id.isNotEmpty())
        assertDoesNotThrow { UUID.fromString(id) }
        assertTrue(typer.map(Behov.Behovstype::utgåendeNavn).containsAll(behov.path("@behov").map(JsonNode::asText)))
    }

    private fun assertArbeidsavklaringspengerdetaljer(behov: JsonNode) {
        assertDato(behov.path(Arbeidsavklaringspenger.utgåendeNavn).path("periodeFom").asText())
        assertDato(behov.path(Arbeidsavklaringspenger.utgåendeNavn).path("periodeTom").asText())
    }

    private fun assertDagpengerdetaljer(behov: JsonNode) {
        assertDato(behov.path(Dagpenger.utgåendeNavn).path("periodeFom").asText())
        assertDato(behov.path(Dagpenger.utgåendeNavn).path("periodeTom").asText())
    }

    private fun assertInntekterForSykepengegrunnlagdetaljer(behov: JsonNode) {
        assertDato(behov.path(InntekterForSykepengegrunnlag.utgåendeNavn).path("skjæringstidspunkt").asText())
        assertÅrMåned(behov.path(InntekterForSykepengegrunnlag.utgåendeNavn).path("beregningStart").asText())
        assertÅrMåned(behov.path(InntekterForSykepengegrunnlag.utgåendeNavn).path("beregningSlutt").asText())
    }

    private fun assertInntekterForOpptjeningsvurderingdetaljer(behov: JsonNode) {
        assertDato(behov.path(InntekterForOpptjeningsvurdering.utgåendeNavn).path("skjæringstidspunkt").asText())
        assertÅrMåned(behov.path(InntekterForOpptjeningsvurdering.utgåendeNavn).path("beregningStart").asText())
        assertÅrMåned(behov.path(InntekterForOpptjeningsvurdering.utgåendeNavn).path("beregningSlutt").asText())
    }

    private fun assertMedlemskapdetaljer(behov: JsonNode) {
        assertDato(behov.path(Medlemskap.utgåendeNavn).path("skjæringstidspunkt").asText())
        assertDato(behov.path(Medlemskap.utgåendeNavn).path("medlemskapPeriodeFom").asText())
        assertDato(behov.path(Medlemskap.utgåendeNavn).path("medlemskapPeriodeTom").asText())
    }

    private fun assertArbeidsforholdV2detaljer(behov: JsonNode) {
        assertDato(behov.path(Arbeidsforhold.utgåendeNavn).path("skjæringstidspunkt").asText())
    }

    private fun assertForeldrepengerdetaljer(behov: JsonNode) {
        assertDato(behov.path(Foreldrepenger.utgåendeNavn).path("foreldrepengerFom").asText())
        assertDato(behov.path(Foreldrepenger.utgåendeNavn).path("foreldrepengerTom").asText())
    }

    private fun assertInstitusjonsoppholddetaljer(behov: JsonNode) {
        assertDato(behov.path(Institusjonsopphold.utgåendeNavn).path("institusjonsoppholdFom").asText())
        assertDato(behov.path(Institusjonsopphold.utgåendeNavn).path("institusjonsoppholdTom").asText())
    }

    private fun assertOmsorgspengerdetaljer(behov: JsonNode) {
        assertDato(behov.path(Omsorgspenger.utgåendeNavn).path("omsorgspengerFom").asText())
        assertDato(behov.path(Omsorgspenger.utgåendeNavn).path("omsorgspengerTom").asText())
    }

    private fun assertOpplæringspengerdetaljer(behov: JsonNode) {
        assertDato(behov.path(Opplæringspenger.utgåendeNavn).path("opplæringspengerFom").asText())
        assertDato(behov.path(Opplæringspenger.utgåendeNavn).path("opplæringspengerTom").asText())
    }

    private fun assertPleiepengerdetaljer(behov: JsonNode) {
        assertDato(behov.path(Pleiepenger.utgåendeNavn).path("pleiepengerFom").asText())
        assertDato(behov.path(Pleiepenger.utgåendeNavn).path("pleiepengerTom").asText())
    }

    private fun assertInntekterForBeregning(behov: JsonNode) {
        assertDato(behov.path(InntekterForBeregning.utgåendeNavn).path("fom").asText())
        assertDato(behov.path(InntekterForBeregning.utgåendeNavn).path("tom").asText())
    }

    private fun assertSykepengehistorikkdetaljer(behov: JsonNode) {
        assertDato(behov.path(Sykepengehistorikk.utgåendeNavn).path("historikkFom").asText())
        assertDato(behov.path(Sykepengehistorikk.utgåendeNavn).path("historikkTom").asText())
    }

    private fun assertSimuleringdetaljer(behov: JsonNode) {
        val simulering = behov.path(Simulering.utgåendeNavn)
        assertTrue(behov.path("utbetalingId").asText().isNotEmpty())
        assertDato(simulering.path("maksdato").asText())
        assertTrue(simulering.path("saksbehandler").asText().isNotEmpty())
        assertOppdragdetaljer(simulering, false)
    }

    private fun assertGodkjenningdetaljer(behov: JsonNode, erSelvstendig: Boolean, arbeidssituasjon: String) {
        val godkjenning = behov.path(Godkjenning.utgåendeNavn)
        assertTrue(behov.path("utbetalingId").asText().isNotEmpty())
        assertDato(godkjenning.path("periodeFom").asText())
        assertDato(godkjenning.path("periodeTom").asText())
        assertDato(godkjenning.path("skjæringstidspunkt").asText())
        assertTrue(godkjenning.path("periodetype").asText().isNotEmpty())
        assertTrue(godkjenning.path("vilkårsgrunnlagId").asText().isNotEmpty())
        assertTrue(godkjenning.path("førstegangsbehandling").isBoolean)
        assertTrue(godkjenning.path("utbetalingtype").asText().isNotEmpty())
        assertTrue(godkjenning.path("inntektskilde").asText().isNotEmpty())
        assertTrue(godkjenning.path("orgnummereMedRelevanteArbeidsforhold").isArray)
        assertFalse(godkjenning.path("orgnummereMedRelevanteArbeidsforhold").isEmpty)
        assertTrue(godkjenning.path("relevanteSøknader").isArray)
        assertTrue(godkjenning.path("tags").isArray)
        assertTrue(godkjenning.path("kanAvvises").isBoolean)
        assertTrue(godkjenning.path("behandlingId").asText().isNotEmpty())
        assertTrue(godkjenning.path("perioderMedSammeSkjæringstidspunkt").isArray)
        assertFalse(godkjenning.path("perioderMedSammeSkjæringstidspunkt").isEmpty)
        godkjenning.path("perioderMedSammeSkjæringstidspunkt").path(0).also {
            assertTrue(it.path("vedtaksperiodeId").asText().isNotEmpty())
            assertTrue(it.path("behandlingId").asText().isNotEmpty())
            assertDato(it.path("fom").asText())
            assertDato(it.path("tom").asText())
        }
        assertEquals(arbeidssituasjon, godkjenning.path("arbeidssituasjon").asText())
        godkjenning.path("sykepengegrunnlagsfakta").also {
            it.path("fastsatt").asText() in listOf("EtterSkjønn", "EtterHovedregel", "IInfotrygd")
            assertTrue(it.path("6G").isDouble)
            if (!erSelvstendig) {
                assertTrue(it.path("arbeidsgivere").isArray)
                assertTrue(it.path("selvstendig").isNull)
                it.path("arbeidsgivere").path(0)?.also { arbeidsgiver ->
                    assertTrue(arbeidsgiver.path("arbeidsgiver").isTextual)
                    assertTrue(arbeidsgiver.path("omregnetÅrsinntekt").isDouble)
                    assertTrue(arbeidsgiver.path("skjønnsfastsatt").isMissingNode)
                }
            } else {
                assertTrue(it.path("arbeidsgivere").isArray)
                assertTrue(it.path("arbeidsgivere").isEmpty)
                it.path("selvstendig").also { selvstendig ->
                    selvstendig.path("pensjonsgivendeInntekter").isArray
                    selvstendig.path("pensjonsgivendeInntekter").forEach { pensjonsinntekt ->
                        assertTrue(pensjonsinntekt.path("beløp").isDouble)
                        assertTrue(pensjonsinntekt.path("årstall").isInt)
                    }
                    selvstendig.path("beregningsgrunnlag").isDouble
                }
            }
        }
    }
}

private fun assertUtbetalingdetaljer(behov: JsonNode, erAnnullering: Boolean = false) {
    val utbetaling = behov.path(Utbetaling.utgåendeNavn)
    if (!erAnnullering) assertDato(utbetaling.path("maksdato").asText())
    assertTrue(utbetaling.path("saksbehandler").asText().isNotEmpty())
    assertOppdragdetaljer(utbetaling, erAnnullering)
}

private fun assertOppdragdetaljer(oppdrag: JsonNode, erAnnullering: Boolean) {
    assertTrue(oppdrag.path("mottaker").asText().isNotEmpty())
    assertTrue(oppdrag.path("fagsystemId").asText().isNotEmpty())
    assertTrue(oppdrag.path("fagområde").asText().isNotEmpty())
    assertTrue(oppdrag.path("endringskode").asText().isNotEmpty())
    val linjer = oppdrag.path("linjer")
    assertTrue(linjer.isArray)
    linjer.forEach { linje ->
        assertDato(linje.path("fom").asText())
        assertDato(linje.path("tom").asText())
        assertTrue(linje.path("sats").isInt)
        assertTrue(linje.path("grad").isDouble)
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

private fun assertÅrMåned(tekst: String) {
    assertTrue(tekst.isNotEmpty())
    assertDoesNotThrow { YearMonth.parse(tekst) }
}

private fun assertDato(tekst: String) {
    assertTrue(tekst.isNotEmpty())
    assertDoesNotThrow { LocalDate.parse(tekst) }
}

private fun assertDatotid(tekst: String) {
    assertTrue(tekst.isNotEmpty())
    assertDoesNotThrow { LocalDateTime.parse(tekst) }
}



