package no.nav.helse.spleis.mediator.e2e

import no.nav.helse.Toggle
import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidssituasjonDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.hendelser.ForsikringsvurderingResultat
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.spleis.Behov
import no.nav.helse.spleis.Behov.Behovstype.*
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.inntektsmeldingkontrakt.Periode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class BehovkontraktTest : AbstractEndToEndMediatorTest() {
    @Test
    fun vilkårsgrunnlag() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
        )
        sendNavNoInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)))
        val behov = testRapid.inspektør.etterspurteBehov(Medlemskap)
        assertVedtaksperiodeBehov(
            behov,
            InntekterForSykepengegrunnlag,
            InntekterForOpptjeningsvurdering,
            Behov.Behovstype.Opptjeningsvurdering,
            Medlemskap,
            Arbeidsforhold,
        )
        assertMedlemskapdetaljer(behov)
        assertInntekterForSykepengegrunnlagdetaljer(behov)
        assertInntekterForOpptjeningsvurderingdetaljer(behov)
        assertArbeidsforholdV2detaljer(behov)
    }

    @Test
    fun `vilkårsgrunnlag med toggle for medlemskapsvurdering`() =
        Toggle.MedlemskapsvurderingBehov.enable {
            sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
            sendSøknad(
                perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
            )
            sendNavNoInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)))
            val behov = testRapid.inspektør.etterspurteBehov(Medlemskap)
            assertVedtaksperiodeBehov(
                behov,
                InntekterForSykepengegrunnlag,
                InntekterForOpptjeningsvurdering,
                Behov.Behovstype.Opptjeningsvurdering,
                Medlemskap,
                Behov.Behovstype.Medlemskapsvurdering,
                Arbeidsforhold,
            )
            assertMedlemskapdetaljer(behov)
            assertDato(behov.path(Behov.Behovstype.Medlemskapsvurdering.utgåendeNavn).path("skjæringstidspunkt").asText())
            assertDato(behov.path(Behov.Behovstype.Medlemskapsvurdering.utgåendeNavn).path("medlemskapPeriodeFom").asText())
            assertDato(behov.path(Behov.Behovstype.Medlemskapsvurdering.utgåendeNavn).path("medlemskapPeriodeTom").asText())
        }

    @Test
    fun `rettighetskategori`() =
        Toggle.AvklareRettighetskategori.enable {
            sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
            val søknadId =
                sendSøknad(
                    perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
                )
            sendNavNoInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)))
            val behov = testRapid.inspektør.etterspurteBehov(Rettighetskategori)
            assertVedtaksperiodeBehov(behov, Rettighetskategori)
            assertEquals("ARBEIDSTAKER", behov.path("yrkesaktivitetstype").asText())
            assertEquals(ORGNUMMER, behov.path("organisasjonsnummer").asText())
            assertTrue(behov.path("behandlingId").asText().isNotEmpty())
            val detaljer = behov.path(Rettighetskategori.utgåendeNavn)
            assertEquals("2018-01-03", detaljer.path("skjæringstidspunkt").asText())
            assertEquals(listOf(søknadId.toString()), detaljer.path("søknader").values().map(JsonNode::asText))
            assertTilstand(0, "AVVENTER_RETTIGHETSKATEGORI")

            sendRettighetskategori(0)
            assertTilstand(0, "AVVENTER_VILKÅRSPRØVING")
            assertTrue(testRapid.inspektør.harEtterspurteBehov(0, Medlemskap))
        }

    @Test
    fun `rettighetskategori - selvstendig`() =
        Toggle.AvklareRettighetskategori.enable {
            sendNySøknadSelvstendig(
                SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100),
                arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE,
            )
            sendSelvstendigsøknad(
                perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
                ventetid = 3.januar til 18.januar,
                arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE,
            )
            val behov = testRapid.inspektør.etterspurteBehov(Rettighetskategori)

            val id = behov.path("@id").asText()
            assertTrue(id.isNotEmpty())
            assertDoesNotThrow { UUID.fromString(id) }

            assertEquals("behov", behov.path("@event_name").asText())
            assertTrue(behov.path("fødselsnummer").asText().isNotEmpty())
            assertTrue(behov.path("@behov").isArray)
            assertDatotid(behov.path("@opprettet").asText())
            assertTrue(behov.path("organisasjonsnummer").isMissingNode)
            assertTrue(behov.path("vedtaksperiodeId").asText().isNotEmpty())

            assertEquals(listOf(Rettighetskategori.utgåendeNavn), behov.path("@behov").values().map(JsonNode::asText))
            assertEquals("SELVSTENDIG", behov.path("yrkesaktivitetstype").asText())

            sendRettighetskategori(0, hovedkategori = "SELVSTENDIG_NÆRINGSDRIVENDE")
            assertTilstand(0, "AVVENTER_VILKÅRSPRØVING")
        }

    @Test
    fun `vilkårsgrunnlag - selvstendig`() {
        sendNySøknadSelvstendig(
            SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100),
            arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE,
        )
        sendSelvstendigsøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
            ventetid = 3.januar til 18.januar,
            arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE,
        )
        val behov = testRapid.inspektør.etterspurteBehov(Medlemskap)
        assertVedtaksperiodeBehov(
            behov,
            InntekterForSykepengegrunnlag,
            InntekterForOpptjeningsvurdering,
            Medlemskap,
            Arbeidsforhold,
            Behov.Behovstype.Forsikringsvurdering,
            Behov.Behovstype.Opptjeningsvurdering,
        )
        assertMedlemskapdetaljer(behov)
        assertInntekterForSykepengegrunnlagdetaljer(behov)
        assertInntekterForOpptjeningsvurderingdetaljer(behov)
        assertArbeidsforholdV2detaljer(behov)
        assertForsikringsvurderingdetaljer(behov, emptyList())
    }

    @Test
    fun `vilkårsgrunnlag - jordbruker`() =
        Toggle.Jordbruker.enable {
            sendNySøknadSelvstendig(
                SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100),
                arbeidssituasjon = ArbeidssituasjonDTO.JORDBRUKER,
            )
            sendSelvstendigsøknad(
                perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
                ventetid = 3.januar til 18.januar,
                arbeidssituasjon = ArbeidssituasjonDTO.JORDBRUKER,
            )
            val behov = testRapid.inspektør.etterspurteBehov(Medlemskap)
            assertVedtaksperiodeBehov(
                behov,
                InntekterForSykepengegrunnlag,
                InntekterForOpptjeningsvurdering,
                Medlemskap,
                Arbeidsforhold,
                Behov.Behovstype.Opptjeningsvurdering,
                Behov.Behovstype.Forsikringsvurdering,
            )
            assertForsikringsvurderingdetaljer(behov, listOf("JORDBRUKER"))
        }

    @Test
    fun ytelser() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
        )
        val utbetalinghistorikkbehov = testRapid.inspektør.meldinger("behov").last()
        assertVedtaksperiodeBehov(utbetalinghistorikkbehov, Sykepengehistorikk)
        assertSykepengehistorikkdetaljer(utbetalinghistorikkbehov)
        sendNavNoInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)))
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
            InntekterForBeregning,
            OpptjeningsvurderingResultat,
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
    fun `ytelser med toggle for medlemskapsvurderingresultat`() =
        Toggle.MedlemskapsvurderingResultatBehov.enable {
            sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
            sendSøknad(
                perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
            )
            sendNavNoInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)))
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
                InntekterForBeregning,
                OpptjeningsvurderingResultat,
                MedlemskapsvurderingResultat,
            )
            assertDoesNotThrow {
                UUID.fromString(
                    behov.path(MedlemskapsvurderingResultat.utgåendeNavn).path("medlemskapsvurderingId").asText(),
                )
            }
        }

    @Test
    fun simulering() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
        )
        sendNavNoInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)))
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        val behov = testRapid.inspektør.etterspurteBehov(Simulering)
        assertVedtaksperiodeBehov(behov, Simulering)
        assertSimuleringdetaljer(behov)
    }

    @Test
    fun `godkjenning - selvstendig`() {
        sendNySøknadSelvstendig(
            SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100),
            arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE,
        )
        sendSelvstendigsøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
            ventetid = 3.januar til 18.januar,
            arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE,
        )

        val forsikringsvurderingId = UUID.randomUUID()
        sendVilkårsgrunnlagSelvstendig(vedtaksperiodeIndeks = 0, forsikringsvurderingId = forsikringsvurderingId)
        sendYtelser(
            vedtaksperiodeIndeks = 0,
            forsikringsvurderingResultat =
                ForsikringsvurderingResultat(
                    forsikringsvurderingId = forsikringsvurderingId,
                    dekning = null,
                    opphørsdato = null,
                    harIndividuellForsikring = false,
                    villeHattForsikringOmDenVarBetalt = false,
                    harForsikringSomIkkePasserMedSøknadstype = false,
                ),
            orgnummer = "SELVSTENDIG",
        )
        sendSimuleringSelvstendig(0, SimuleringMessage.Simuleringstatus.OK)
        val behov = testRapid.inspektør.etterspurteBehov(Godkjenning)
        assertVedtaksperiodeBehov(behov, Godkjenning)
        assertGodkjenningdetaljer(behov, erSelvstendig = true, "SELVSTENDIG_NÆRINGSDRIVENDE")
    }

    @Test
    fun godkjenning() {
        val opptjeningsvurderingId = UUID.randomUUID()
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
        )
        sendNavNoInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)))
        sendVilkårsgrunnlag(0, opptjeningsvurderingId = opptjeningsvurderingId)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        val behov = testRapid.inspektør.etterspurteBehov(Godkjenning)
        assertVedtaksperiodeBehov(behov, Godkjenning)
        assertGodkjenningdetaljer(behov, erSelvstendig = false, "ARBEIDSTAKER")
        assertEquals(opptjeningsvurderingId.toString(), behov.path(Godkjenning.utgåendeNavn).path("opptjeningsvurderingId").asText())
    }

    @Test
    fun utbetaling() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
        )
        sendNavNoInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)))
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
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
        )
        sendNavNoInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)))
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

    private fun assertVedtaksperiodeBehov(
        behov: JsonNode,
        vararg typer: Behov.Behovstype,
    ) {
        assertBehov(behov, *typer)
        assertTrue(behov.path("vedtaksperiodeId").asText().isNotEmpty())
    }

    private fun assertUtbetalingBehov(
        behov: JsonNode,
        vararg typer: Behov.Behovstype,
    ) {
        assertBehov(behov, *typer)
        assertTrue(behov.path("utbetalingId").asText().isNotEmpty())
    }

    private fun assertBehov(
        behov: JsonNode,
        vararg typer: Behov.Behovstype,
    ) {
        val id = behov.path("@id").asText()
        assertEquals("behov", behov.path("@event_name").asText())
        assertTrue(behov.path("fødselsnummer").asText().isNotEmpty())
        assertTrue(behov.path("organisasjonsnummer").asText().isNotEmpty())
        assertTrue(behov.path("@behov").isArray)
        assertDatotid(behov.path("@opprettet").asText())
        assertTrue(id.isNotEmpty())
        assertDoesNotThrow { UUID.fromString(id) }
        assertTrue(typer.map(Behov.Behovstype::utgåendeNavn).containsAll(behov.path("@behov").values().map(JsonNode::asText)))
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

    private fun assertForsikringsvurderingdetaljer(
        behov: JsonNode,
        forventedeSpesielleYrkesgrupper: List<String>,
    ) {
        assertDato(behov.path(Behov.Behovstype.Forsikringsvurdering.utgåendeNavn).path("skjæringstidspunkt").asText())
        assertEquals(
            forventedeSpesielleYrkesgrupper,
            behov
                .path(Behov.Behovstype.Forsikringsvurdering.utgåendeNavn)
                .path("spesielleYrkesgrupper")
                .values()
                .map(JsonNode::asText),
        )
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

    private fun assertGodkjenningdetaljer(
        behov: JsonNode,
        erSelvstendig: Boolean,
        arbeidssituasjon: String,
    ) {
        val godkjenning = behov.path(Godkjenning.utgåendeNavn)
        assertTrue(behov.path("utbetalingId").asText().isNotEmpty())
        assertDato(godkjenning.path("periodeFom").asText())
        assertDato(godkjenning.path("periodeTom").asText())
        assertDato(godkjenning.path("skjæringstidspunkt").asText())
        assertTrue(godkjenning.path("periodetype").asText().isNotEmpty())
        assertTrue(godkjenning.path("vilkårsgrunnlagId").asText().isNotEmpty())
        assertTrue(godkjenning.path("opptjeningsvurderingId").asText().isNotEmpty())
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
            it.path("yrkesaktivitet").also { ya ->
                val yaType = ya.path("yrkesaktivitetstype").asText()
                assertTrue(yaType.isNotEmpty())
                if (yaType == "ARBEIDSTAKER") {
                    assertTrue(ya.path("organisasjonsnummer").asText().isNotEmpty())
                } else {
                    assertTrue(ya.path("organisasjonsnummer").isMissingNode)
                }
            }
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

private fun assertUtbetalingdetaljer(
    behov: JsonNode,
    erAnnullering: Boolean = false,
) {
    val utbetaling = behov.path(Utbetaling.utgåendeNavn)
    if (!erAnnullering) assertDato(utbetaling.path("maksdato").asText())
    assertTrue(utbetaling.path("saksbehandler").asText().isNotEmpty())
    assertOppdragdetaljer(utbetaling, erAnnullering)
}

private fun assertOppdragdetaljer(
    oppdrag: JsonNode,
    erAnnullering: Boolean,
) {
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
