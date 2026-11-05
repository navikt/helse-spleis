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
            Opptjeningsvurdering,
            Medlemskap,
            Arbeidsforhold,
        )
        assertMedlemskapdetaljer(behov)
        assertInntekterForSykepengegrunnlagdetaljer(behov)
        assertInntekterForOpptjeningsvurderingdetaljer(behov)
        assertArbeidsforholdV2detaljer(behov)
    }

    @Test
    fun `vilkårsgrunnlag - opptjeningsvurdering med rettighetskategoriId`() {
        besvarRettighetskategoriAutomatisk = false
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
        )
        sendNavNoInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)))
        val rettighetskategoriId = UUID.randomUUID()
        sendRettighetskategori(0, rettighetskategoriId = rettighetskategoriId)

        val behov = testRapid.inspektør.etterspurteBehov(Opptjeningsvurdering)
        val detaljer = behov.path(Opptjeningsvurdering.utgåendeNavn)
        assertEquals("2018-01-03", detaljer.path("skjæringstidspunkt").asString())
        assertEquals("Arbeidstaker", detaljer.path("arbeidssituasjon").asString())
        assertEquals(rettighetskategoriId.toString(), detaljer.path("rettighetskategoriId").asString())
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
                Opptjeningsvurdering,
                Medlemskap,
                Medlemskapsvurdering,
                Arbeidsforhold,
            )
            assertMedlemskapdetaljer(behov)
            assertDato(behov.path(Medlemskapsvurdering.utgåendeNavn).path("skjæringstidspunkt").asString())
            assertDato(behov.path(Medlemskapsvurdering.utgåendeNavn).path("medlemskapPeriodeFom").asString())
            assertDato(behov.path(Medlemskapsvurdering.utgåendeNavn).path("medlemskapPeriodeTom").asString())
        }

    @Test
    fun rettighetskategori() {
        besvarRettighetskategoriAutomatisk = false
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        val søknadId =
            sendSøknad(
                perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
            )
        sendNavNoInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)))
        val behov = testRapid.inspektør.etterspurteBehov(Rettighetskategori)
        assertVedtaksperiodeBehov(behov, Rettighetskategori)
        assertEquals("ARBEIDSTAKER", behov.path("yrkesaktivitetstype").asString())
        assertEquals(ORGNUMMER, behov.path("organisasjonsnummer").asString())
        assertTrue(behov.path("behandlingId").asString().isNotEmpty())
        val detaljer = behov.path(Rettighetskategori.utgåendeNavn)
        assertEquals("2018-01-03", detaljer.path("skjæringstidspunkt").asString())
        assertEquals(listOf(søknadId.toString()), detaljer.path("søknader").values().map(JsonNode::asString))
        assertTilstand(0, "AVVENTER_RETTIGHETSKATEGORI")

        sendRettighetskategori(0)
        assertTilstand(0, "AVVENTER_VILKÅRSPRØVING")
        assertTrue(testRapid.inspektør.harEtterspurteBehov(0, Medlemskap))
    }

    @Test
    fun `rettighetskategori - selvstendig`() {
        besvarRettighetskategoriAutomatisk = false
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

        val id = behov.path("@id").asString()
        assertTrue(id.isNotEmpty())
        assertDoesNotThrow { UUID.fromString(id) }

        assertEquals("behov", behov.path("@event_name").asString())
        assertTrue(behov.path("fødselsnummer").asString().isNotEmpty())
        assertTrue(behov.path("@behov").isArray)
        assertDatotid(behov.path("@opprettet").asString())
        assertTrue(behov.path("organisasjonsnummer").isMissingNode)
        assertTrue(behov.path("vedtaksperiodeId").asString().isNotEmpty())

        assertEquals(listOf(Rettighetskategori.utgåendeNavn), behov.path("@behov").values().map(JsonNode::asString))
        assertEquals("SELVSTENDIG", behov.path("yrkesaktivitetstype").asString())

        sendRettighetskategori(0, hovedkategori = "SELVSTENDIG_NÆRINGSDRIVENDE")
        assertTilstand(0, "AVVENTER_VILKÅRSPRØVING")
    }

    @Test
    fun `vilkårsgrunnlag - selvstendig`() {
        besvarRettighetskategoriAutomatisk = false
        sendNySøknadSelvstendig(
            SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100),
            arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE,
        )
        sendSelvstendigsøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
            ventetid = 3.januar til 18.januar,
            arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE,
        )
        val rettighetskategoriId = UUID.randomUUID()
        sendRettighetskategori(0, rettighetskategoriId = rettighetskategoriId, hovedkategori = "SELVSTENDIG_NÆRINGSDRIVENDE")
        val behov = testRapid.inspektør.etterspurteBehov(Medlemskap)
        assertEquals("SELVSTENDIG", behov.path("yrkesaktivitetstype").asString())
        assertTrue(behov.path("vedtaksperiodeId").asString().isNotEmpty())
        assertTrue(behov.path("behandlingId").asString().isNotEmpty())
        assertVedtaksperiodeBehov(
            behov,
            InntekterForSykepengegrunnlag,
            InntekterForOpptjeningsvurdering,
            Medlemskap,
            Arbeidsforhold,
            Forsikringsvurdering,
            Opptjeningsvurdering,
        )
        assertMedlemskapdetaljer(behov)
        assertInntekterForSykepengegrunnlagdetaljer(behov)
        assertInntekterForOpptjeningsvurderingdetaljer(behov)
        assertArbeidsforholdV2detaljer(behov)
        assertForsikringsvurderingdetaljer(behov, emptyList(), rettighetskategoriId)
    }

    @Test
    fun ytelser() {
        besvarRettighetskategoriAutomatisk = false
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
        )
        val utbetalinghistorikkbehov = testRapid.inspektør.meldinger("behov").last()
        assertVedtaksperiodeBehov(utbetalinghistorikkbehov, Sykepengehistorikk)
        assertSykepengehistorikkdetaljer(utbetalinghistorikkbehov)
        sendNavNoInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)))
        val rettighetskategoriId = UUID.randomUUID()
        sendRettighetskategori(0, rettighetskategoriId = rettighetskategoriId)
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
            RettighetskategoriResultat,
            MedlemskapsvurderingResultat,
        )
        assertArbeidsavklaringspengerdetaljer(behov)
        assertDagpengerdetaljer(behov)
        assertForeldrepengerdetaljer(behov)
        assertInstitusjonsoppholddetaljer(behov)
        assertOmsorgspengerdetaljer(behov)
        assertOpplæringspengerdetaljer(behov)
        assertPleiepengerdetaljer(behov)
        assertInntekterForBeregning(behov)
        assertEquals(
            rettighetskategoriId.toString(),
            behov.path(RettighetskategoriResultat.utgåendeNavn).path("rettighetskategoriId").asText(),
        )
        sendYtelser(0)
        assertTrue(testRapid.inspektør.harEtterspurteBehov(0, Simulering))
    }

    @Test
    fun `ytelser med toggle for graderte andre ytelser`() =
        Toggle.GraderteAndreYtelser.enable {
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
                GraderteAndreYtelserForBeregning,
                OpptjeningsvurderingResultat,
                RettighetskategoriResultat,
                MedlemskapsvurderingResultat,
            )
            assertEquals(3.januar, LocalDate.parse(behov.path(GraderteAndreYtelserForBeregning.utgåendeNavn).path("fom").asString()))
            assertEquals(26.januar, LocalDate.parse(behov.path(GraderteAndreYtelserForBeregning.utgåendeNavn).path("tom").asString()))
        }

    @Test
    fun `ytelser med toggle for medlemskapsvurderingresultat`() {
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
            RettighetskategoriResultat,
        )
        assertDoesNotThrow {
            UUID.fromString(
                behov.path(MedlemskapsvurderingResultat.utgåendeNavn).path("medlemskapsvurderingId").asString(),
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
        assertGodkjenningdetaljer(behov, erSelvstendig = true)
    }

    @Test
    fun godkjenning() {
        val opptjeningsvurderingId = UUID.randomUUID()
        val medlemskapsvurderingId = UUID.randomUUID()
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
        )
        sendNavNoInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)))
        sendVilkårsgrunnlag(0, opptjeningsvurderingId = opptjeningsvurderingId, medlemskapsvurderingId = medlemskapsvurderingId)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        val behov = testRapid.inspektør.etterspurteBehov(Godkjenning)
        assertVedtaksperiodeBehov(behov, Godkjenning)
        assertGodkjenningdetaljer(behov, erSelvstendig = false)
        assertEquals(opptjeningsvurderingId.toString(), behov.path(Godkjenning.utgåendeNavn).path("opptjeningsvurderingId").asString())
        assertEquals(medlemskapsvurderingId.toString(), behov.path(Godkjenning.utgåendeNavn).path("medlemskapsvurderingId").asString())
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
        assertTrue(behov.path("vedtaksperiodeId").asString().isNotEmpty())
    }

    private fun assertUtbetalingBehov(
        behov: JsonNode,
        vararg typer: Behov.Behovstype,
    ) {
        assertBehov(behov, *typer)
        assertTrue(behov.path("utbetalingId").asString().isNotEmpty())
    }

    private fun assertBehov(
        behov: JsonNode,
        vararg typer: Behov.Behovstype,
    ) {
        val id = behov.path("@id").asString()
        assertEquals("behov", behov.path("@event_name").asString())
        assertTrue(behov.path("fødselsnummer").asString().isNotEmpty())
        assertTrue(behov.path("organisasjonsnummer").asString().isNotEmpty())
        assertTrue(behov.path("@behov").isArray)
        assertDatotid(behov.path("@opprettet").asString())
        assertTrue(id.isNotEmpty())
        assertDoesNotThrow { UUID.fromString(id) }
        assertTrue(typer.map(Behov.Behovstype::utgåendeNavn).containsAll(behov.path("@behov").values().map(JsonNode::asString)))
    }

    private fun assertArbeidsavklaringspengerdetaljer(behov: JsonNode) {
        assertDato(behov.path(Arbeidsavklaringspenger.utgåendeNavn).path("periodeFom").asString())
        assertDato(behov.path(Arbeidsavklaringspenger.utgåendeNavn).path("periodeTom").asString())
    }

    private fun assertDagpengerdetaljer(behov: JsonNode) {
        assertDato(behov.path(Dagpenger.utgåendeNavn).path("periodeFom").asString())
        assertDato(behov.path(Dagpenger.utgåendeNavn).path("periodeTom").asString())
    }

    private fun assertInntekterForSykepengegrunnlagdetaljer(behov: JsonNode) {
        assertDato(behov.path(InntekterForSykepengegrunnlag.utgåendeNavn).path("skjæringstidspunkt").asString())
        assertÅrMåned(behov.path(InntekterForSykepengegrunnlag.utgåendeNavn).path("beregningStart").asString())
        assertÅrMåned(behov.path(InntekterForSykepengegrunnlag.utgåendeNavn).path("beregningSlutt").asString())
    }

    private fun assertInntekterForOpptjeningsvurderingdetaljer(behov: JsonNode) {
        assertDato(behov.path(InntekterForOpptjeningsvurdering.utgåendeNavn).path("skjæringstidspunkt").asString())
        assertÅrMåned(behov.path(InntekterForOpptjeningsvurdering.utgåendeNavn).path("beregningStart").asString())
        assertÅrMåned(behov.path(InntekterForOpptjeningsvurdering.utgåendeNavn).path("beregningSlutt").asString())
    }

    private fun assertMedlemskapdetaljer(behov: JsonNode) {
        assertDato(behov.path(Medlemskap.utgåendeNavn).path("skjæringstidspunkt").asString())
        assertDato(behov.path(Medlemskap.utgåendeNavn).path("medlemskapPeriodeFom").asString())
        assertDato(behov.path(Medlemskap.utgåendeNavn).path("medlemskapPeriodeTom").asString())
    }

    private fun assertArbeidsforholdV2detaljer(behov: JsonNode) {
        assertDato(behov.path(Arbeidsforhold.utgåendeNavn).path("skjæringstidspunkt").asString())
    }

    private fun assertForsikringsvurderingdetaljer(
        behov: JsonNode,
        forventedeSpesielleYrkesgrupper: List<String>,
        forventetRettighetskategoriId: UUID,
    ) {
        assertDato(behov.path(Forsikringsvurdering.utgåendeNavn).path("skjæringstidspunkt").asString())
        assertEquals(
            forventetRettighetskategoriId.toString(),
            behov.path(Forsikringsvurdering.utgåendeNavn).path("rettighetskategoriId").asString(),
        )
        assertEquals(
            forventedeSpesielleYrkesgrupper,
            behov
                .path(Forsikringsvurdering.utgåendeNavn)
                .path("spesielleYrkesgrupper")
                .values()
                .map(JsonNode::asString),
        )
    }

    private fun assertForeldrepengerdetaljer(behov: JsonNode) {
        assertDato(behov.path(Foreldrepenger.utgåendeNavn).path("foreldrepengerFom").asString())
        assertDato(behov.path(Foreldrepenger.utgåendeNavn).path("foreldrepengerTom").asString())
    }

    private fun assertInstitusjonsoppholddetaljer(behov: JsonNode) {
        assertDato(behov.path(Institusjonsopphold.utgåendeNavn).path("institusjonsoppholdFom").asString())
        assertDato(behov.path(Institusjonsopphold.utgåendeNavn).path("institusjonsoppholdTom").asString())
    }

    private fun assertOmsorgspengerdetaljer(behov: JsonNode) {
        assertDato(behov.path(Omsorgspenger.utgåendeNavn).path("omsorgspengerFom").asString())
        assertDato(behov.path(Omsorgspenger.utgåendeNavn).path("omsorgspengerTom").asString())
    }

    private fun assertOpplæringspengerdetaljer(behov: JsonNode) {
        assertDato(behov.path(Opplæringspenger.utgåendeNavn).path("opplæringspengerFom").asString())
        assertDato(behov.path(Opplæringspenger.utgåendeNavn).path("opplæringspengerTom").asString())
    }

    private fun assertPleiepengerdetaljer(behov: JsonNode) {
        assertDato(behov.path(Pleiepenger.utgåendeNavn).path("pleiepengerFom").asString())
        assertDato(behov.path(Pleiepenger.utgåendeNavn).path("pleiepengerTom").asString())
    }

    private fun assertInntekterForBeregning(behov: JsonNode) {
        assertDato(behov.path(InntekterForBeregning.utgåendeNavn).path("fom").asString())
        assertDato(behov.path(InntekterForBeregning.utgåendeNavn).path("tom").asString())
    }

    private fun assertSykepengehistorikkdetaljer(behov: JsonNode) {
        assertDato(behov.path(Sykepengehistorikk.utgåendeNavn).path("historikkFom").asString())
        assertDato(behov.path(Sykepengehistorikk.utgåendeNavn).path("historikkTom").asString())
    }

    private fun assertSimuleringdetaljer(behov: JsonNode) {
        val simulering = behov.path(Simulering.utgåendeNavn)
        assertTrue(behov.path("utbetalingId").asString().isNotEmpty())
        assertDato(simulering.path("maksdato").asString())
        assertTrue(simulering.path("saksbehandler").asString().isNotEmpty())
        assertOppdragdetaljer(simulering, false)
    }

    private fun assertGodkjenningdetaljer(
        behov: JsonNode,
        erSelvstendig: Boolean,
    ) {
        val godkjenning = behov.path(Godkjenning.utgåendeNavn)
        assertTrue(behov.path("utbetalingId").asString().isNotEmpty())
        assertDato(godkjenning.path("periodeFom").asString())
        assertDato(godkjenning.path("periodeTom").asString())
        assertDato(godkjenning.path("skjæringstidspunkt").asString())
        assertTrue(godkjenning.path("periodetype").asString().isNotEmpty())
        assertTrue(godkjenning.path("vilkårsgrunnlagId").asString().isNotEmpty())
        assertTrue(godkjenning.path("opptjeningsvurderingId").asString().isNotEmpty())
        assertTrue(godkjenning.path("medlemskapsvurderingId").asString().isNotEmpty())
        assertTrue(godkjenning.path("førstegangsbehandling").isBoolean)
        assertTrue(godkjenning.path("utbetalingtype").asString().isNotEmpty())
        assertTrue(godkjenning.path("inntektskilde").asString().isNotEmpty())
        assertTrue(godkjenning.path("orgnummereMedRelevanteArbeidsforhold").isArray)
        assertFalse(godkjenning.path("orgnummereMedRelevanteArbeidsforhold").isEmpty)
        assertTrue(godkjenning.path("relevanteSøknader").isArray)
        assertTrue(godkjenning.path("tags").isArray)
        assertTrue(godkjenning.path("kanAvvises").isBoolean)
        assertTrue(godkjenning.path("behandlingId").asString().isNotEmpty())
        assertTrue(godkjenning.path("perioderMedSammeSkjæringstidspunkt").isArray)
        assertFalse(godkjenning.path("perioderMedSammeSkjæringstidspunkt").isEmpty)
        godkjenning.path("perioderMedSammeSkjæringstidspunkt").path(0).also {
            assertTrue(it.path("vedtaksperiodeId").asString().isNotEmpty())
            assertTrue(it.path("behandlingId").asString().isNotEmpty())
            assertDato(it.path("fom").asString())
            assertDato(it.path("tom").asString())
            it.path("yrkesaktivitet").also { ya ->
                val yaType = ya.path("yrkesaktivitetstype").asString()
                assertTrue(yaType.isNotEmpty())
                if (yaType == "ARBEIDSTAKER") {
                    assertTrue(ya.path("organisasjonsnummer").asString().isNotEmpty())
                } else {
                    assertTrue(ya.path("organisasjonsnummer").isMissingNode)
                }
            }
        }
        godkjenning.path("sykepengegrunnlagsfakta").also {
            it.path("fastsatt").asString() in listOf("EtterSkjønn", "EtterHovedregel", "IInfotrygd")
            assertTrue(it.path("6G").isDouble)
            if (!erSelvstendig) {
                assertTrue(it.path("arbeidsgivere").isArray)
                assertTrue(it.path("selvstendig").isNull)
                it.path("arbeidsgivere").path(0)?.also { arbeidsgiver ->
                    assertTrue(arbeidsgiver.path("arbeidsgiver").isString)
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
    if (!erAnnullering) assertDato(utbetaling.path("maksdato").asString())
    assertTrue(utbetaling.path("saksbehandler").asString().isNotEmpty())
    assertOppdragdetaljer(utbetaling, erAnnullering)
}

private fun assertOppdragdetaljer(
    oppdrag: JsonNode,
    erAnnullering: Boolean,
) {
    assertTrue(oppdrag.path("mottaker").asString().isNotEmpty())
    assertTrue(oppdrag.path("fagsystemId").asString().isNotEmpty())
    assertTrue(oppdrag.path("fagområde").asString().isNotEmpty())
    assertTrue(oppdrag.path("endringskode").asString().isNotEmpty())
    val linjer = oppdrag.path("linjer")
    assertTrue(linjer.isArray)
    linjer.forEach { linje ->
        assertDato(linje.path("fom").asString())
        assertDato(linje.path("tom").asString())
        assertTrue(linje.path("sats").isInt)
        assertTrue(linje.path("grad").isDouble)
        assertTrue(linje.path("delytelseId").isInt)
        assertTrue(linje.has("refFagsystemId"))
        assertTrue(linje.has("refDelytelseId"))
        if (!erAnnullering) {
            assertTrue(linje.has("datoStatusFom"))
            assertTrue(linje.has("statuskode"))
        } else {
            assertDato(linje.path("datoStatusFom").asString())
            assertTrue(linje.path("statuskode").asString().isNotEmpty())
        }
        assertTrue(linje.path("endringskode").asString().isNotEmpty())
        assertTrue(linje.path("klassekode").asString().isNotEmpty())
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
