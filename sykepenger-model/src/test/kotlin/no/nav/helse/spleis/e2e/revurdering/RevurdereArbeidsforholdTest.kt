package no.nav.helse.spleis.e2e.revurdering

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Toggle
import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.OverstyrtArbeidsgiveropplysning
import no.nav.helse.dsl.TestPerson
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.OverstyrArbeidsforhold.ArbeidsforholdOverstyrt
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SKJØNNSMESSIG_FASTSETTELSE_REVURDERING
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_2
import no.nav.helse.person.inntekt.Inntektsmelding
import no.nav.helse.person.inntekt.SkattSykepengegrunnlag
import no.nav.helse.person.inntekt.SkjønnsmessigFastsatt
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.person
import no.nav.helse.spleis.e2e.grunnlag
import no.nav.helse.spleis.e2e.repeat
import no.nav.helse.spleis.e2e.sammenligningsgrunnlag
import no.nav.helse.utbetalingslinjer.UtbetalingInntektskilde.EN_ARBEIDSGIVER
import no.nav.helse.utbetalingslinjer.UtbetalingInntektskilde.FLERE_ARBEIDSGIVERE
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

internal class RevurderArbeidsforholdTest: AbstractDslTest() {

    @Test
    fun `revurder arbeidsforhold i Avsluttet`() = Toggle.TjuefemprosentAvvik.enable {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT)
            håndterVilkårsgrunnlagMedGhostArbeidsforhold(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            (inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør ?: fail { "finner ikke vilkårsgrunnlag" }).also { vilkårsgrunnlag ->
                val sykepengegrunnlagInspektør = vilkårsgrunnlag.sykepengegrunnlag.inspektør
                val sammenligningsgrunnlagInspektør = vilkårsgrunnlag.sammenligningsgrunnlag.inspektør

                assertEquals(744000.årlig, sykepengegrunnlagInspektør.beregningsgrunnlag)
                assertEquals(561804.årlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
                assertEquals(744000.årlig, sammenligningsgrunnlagInspektør.sammenligningsgrunnlag)
                assertEquals(FLERE_ARBEIDSGIVERE, sykepengegrunnlagInspektør.inntektskilde)
                assertEquals(FLERE_ARBEIDSGIVERE, inspektør.inntektskilde(1.vedtaksperiode))
                assertEquals(0, vilkårsgrunnlag.sykepengegrunnlag.inspektør.avviksprosent)
                assertEquals(2, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                    assertEquals(31000.månedlig, it.inntektsopplysning.inspektør.beløp)
                    assertEquals(Inntektsmelding::class, it.inntektsopplysning::class)
                }
                sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
                    assertEquals(31000.månedlig, it.inntektsopplysning.inspektør.beløp)
                    assertEquals(SkattSykepengegrunnlag::class, it.inntektsopplysning::class)
                }
                assertEquals(2, sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                    assertEquals(31000.månedlig, it.rapportertInntekt)
                }
                sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
                    assertEquals(31000.månedlig, it.rapportertInntekt)
                }
            }
            nullstillTilstandsendringer()
            håndterOverstyrArbeidsforhold(1.januar, ArbeidsforholdOverstyrt(a2, true, "test"))
            assertVarsel(RV_IV_2)
            håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, 31000.månedlig)))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_SKJØNNSMESSIG_FASTSETTELSE_REVURDERING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
            (inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør ?: fail { "finner ikke vilkårsgrunnlag" }).also { vilkårsgrunnlag ->
                val sykepengegrunnlagInspektør = vilkårsgrunnlag.sykepengegrunnlag.inspektør
                val sammenligningsgrunnlagInspektør = vilkårsgrunnlag.sammenligningsgrunnlag.inspektør

                assertEquals(372000.årlig, sykepengegrunnlagInspektør.beregningsgrunnlag)
                assertEquals(372000.årlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
                assertEquals(744000.årlig, sammenligningsgrunnlagInspektør.sammenligningsgrunnlag)
                assertEquals(EN_ARBEIDSGIVER, sykepengegrunnlagInspektør.inntektskilde)
                assertEquals(EN_ARBEIDSGIVER, inspektør.inntektskilde(1.vedtaksperiode))
                assertEquals(50, vilkårsgrunnlag.sykepengegrunnlag.inspektør.avviksprosent)
                assertEquals(1, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                    assertEquals(31000.månedlig, it.inntektsopplysning.inspektør.beløp)
                    assertEquals(SkjønnsmessigFastsatt::class, it.inntektsopplysning::class)
                }
                assertEquals(2, sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                    assertEquals(31000.månedlig, it.rapportertInntekt)
                }
                sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
                    assertEquals(31000.månedlig, it.rapportertInntekt)
                }
            }
        }
    }
    @Test
    fun `overstyrer forlengelse, førstegangsbehandling revurderes`() = Toggle.TjuefemprosentAvvik.enable {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT)
            håndterVilkårsgrunnlagMedGhostArbeidsforhold(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            // ny periode
            håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING)
            (inspektør.vilkårsgrunnlag(2.vedtaksperiode)?.inspektør ?: fail { "finner ikke vilkårsgrunnlag" }).also { vilkårsgrunnlag ->
                val sykepengegrunnlagInspektør = vilkårsgrunnlag.sykepengegrunnlag.inspektør
                val sammenligningsgrunnlagInspektør = vilkårsgrunnlag.sammenligningsgrunnlag.inspektør

                assertEquals(744000.årlig, sykepengegrunnlagInspektør.beregningsgrunnlag)
                assertEquals(561804.årlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
                assertEquals(744000.årlig, sammenligningsgrunnlagInspektør.sammenligningsgrunnlag)
                assertEquals(FLERE_ARBEIDSGIVERE, sykepengegrunnlagInspektør.inntektskilde)
                assertEquals(FLERE_ARBEIDSGIVERE, inspektør.inntektskilde(1.vedtaksperiode))
                assertEquals(0, vilkårsgrunnlag.sykepengegrunnlag.inspektør.avviksprosent)
                assertEquals(2, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                    assertEquals(31000.månedlig, it.inntektsopplysning.inspektør.beløp)
                    assertEquals(Inntektsmelding::class, it.inntektsopplysning::class)
                }
                sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
                    assertEquals(31000.månedlig, it.inntektsopplysning.inspektør.beløp)
                    assertEquals(SkattSykepengegrunnlag::class, it.inntektsopplysning::class)
                }
                assertEquals(2, sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                    assertEquals(31000.månedlig, it.rapportertInntekt)
                }
                sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
                    assertEquals(31000.månedlig, it.rapportertInntekt)
                }
            }
            nullstillTilstandsendringer()
            håndterOverstyrArbeidsforhold(1.januar, ArbeidsforholdOverstyrt(a2, true, "test"))
            assertVarsel(RV_IV_2)
            håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, 31000.månedlig)))
            assertEquals(50, inspektør.vilkårsgrunnlag(2.vedtaksperiode)?.inspektør?.sykepengegrunnlag?.inspektør?.avviksprosent)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertTilstander(
                1.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_SKJØNNSMESSIG_FASTSETTELSE_REVURDERING,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                TIL_UTBETALING,
                AVSLUTTET
            )
            assertTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
            (inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør ?: fail { "finner ikke vilkårsgrunnlag" }).also { vilkårsgrunnlag ->
                val sykepengegrunnlagInspektør = vilkårsgrunnlag.sykepengegrunnlag.inspektør
                val sammenligningsgrunnlagInspektør = vilkårsgrunnlag.sammenligningsgrunnlag.inspektør

                assertEquals(372000.årlig, sykepengegrunnlagInspektør.beregningsgrunnlag)
                assertEquals(372000.årlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
                assertEquals(744000.årlig, sammenligningsgrunnlagInspektør.sammenligningsgrunnlag)
                assertEquals(EN_ARBEIDSGIVER, sykepengegrunnlagInspektør.inntektskilde)
                assertEquals(EN_ARBEIDSGIVER, inspektør.inntektskilde(1.vedtaksperiode))
                assertEquals(50, vilkårsgrunnlag.sykepengegrunnlag.inspektør.avviksprosent)
                assertEquals(1, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                    assertEquals(31000.månedlig, it.inntektsopplysning.inspektør.beløp)
                    assertEquals(SkjønnsmessigFastsatt::class, it.inntektsopplysning::class)
                }
                assertEquals(2, sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                    assertEquals(31000.månedlig, it.rapportertInntekt)
                }
                sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
                    assertEquals(31000.månedlig, it.rapportertInntekt)
                }
            }
        }
    }

    @Test
    fun `deaktiverer arbeidsforhold frem & tilbake, førstegangsbehandling revurderes`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT)
            håndterVilkårsgrunnlagMedGhostArbeidsforhold(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            // ny periode
            håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING)
            (inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør ?: fail { "finner ikke vilkårsgrunnlag" }).also { vilkårsgrunnlag ->
                val sykepengegrunnlagInspektør = vilkårsgrunnlag.sykepengegrunnlag.inspektør
                val sammenligningsgrunnlagInspektør = vilkårsgrunnlag.sammenligningsgrunnlag.inspektør

                assertEquals(744000.årlig, sykepengegrunnlagInspektør.beregningsgrunnlag)
                assertEquals(561804.årlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
                assertEquals(744000.årlig, sammenligningsgrunnlagInspektør.sammenligningsgrunnlag)
                assertEquals(FLERE_ARBEIDSGIVERE, sykepengegrunnlagInspektør.inntektskilde)
                assertEquals(FLERE_ARBEIDSGIVERE, inspektør.inntektskilde(1.vedtaksperiode))
                assertEquals(0, vilkårsgrunnlag.sykepengegrunnlag.inspektør.avviksprosent)
                assertEquals(2, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                    assertEquals(31000.månedlig, it.inntektsopplysning.inspektør.beløp)
                    assertEquals(Inntektsmelding::class, it.inntektsopplysning::class)
                }
                sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
                    assertEquals(31000.månedlig, it.inntektsopplysning.inspektør.beløp)
                    assertEquals(SkattSykepengegrunnlag::class, it.inntektsopplysning::class)
                }
                assertEquals(2, sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                    assertEquals(31000.månedlig, it.rapportertInntekt)
                }
                sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
                    assertEquals(31000.månedlig, it.rapportertInntekt)
                }
            }
            nullstillTilstandsendringer()
            håndterOverstyrArbeidsforhold(1.januar, ArbeidsforholdOverstyrt(a2, true, "test"))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            (inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør ?: fail { "finner ikke vilkårsgrunnlag" }).also { vilkårsgrunnlag ->
                val sykepengegrunnlagInspektør = vilkårsgrunnlag.sykepengegrunnlag.inspektør
                val sammenligningsgrunnlagInspektør = vilkårsgrunnlag.sammenligningsgrunnlag.inspektør

                assertEquals(372000.årlig, sykepengegrunnlagInspektør.beregningsgrunnlag)
                assertEquals(372000.årlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
                assertEquals(744000.årlig, sammenligningsgrunnlagInspektør.sammenligningsgrunnlag)
                assertEquals(EN_ARBEIDSGIVER, sykepengegrunnlagInspektør.inntektskilde)
                assertEquals(EN_ARBEIDSGIVER, inspektør.inntektskilde(1.vedtaksperiode))
                assertEquals(50, vilkårsgrunnlag.sykepengegrunnlag.inspektør.avviksprosent)
                assertEquals(1, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                    assertEquals(31000.månedlig, it.inntektsopplysning.inspektør.beløp)
                    assertEquals(Inntektsmelding::class, it.inntektsopplysning::class)
                }
                assertEquals(2, sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                    assertEquals(31000.månedlig, it.rapportertInntekt)
                }
                sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
                    assertEquals(31000.månedlig, it.rapportertInntekt)
                }
            }
            håndterOverstyrArbeidsforhold(1.januar, ArbeidsforholdOverstyrt(a2, false, "test"))
            inspektør.utbetalinger(1.vedtaksperiode).also { utbetalinger ->
                assertEquals(2, utbetalinger.size)
                assertEquals(Utbetalingstatus.FORKASTET, utbetalinger.last().inspektør.tilstand)
            }
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            assertTilstander(
                1.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_SKJØNNSMESSIG_FASTSETTELSE_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                AVSLUTTET
            )
            assertTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
            (inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør ?: fail { "finner ikke vilkårsgrunnlag" }).also { vilkårsgrunnlag ->
                val sykepengegrunnlagInspektør = vilkårsgrunnlag.sykepengegrunnlag.inspektør
                val sammenligningsgrunnlagInspektør = vilkårsgrunnlag.sammenligningsgrunnlag.inspektør

                assertEquals(744000.årlig, sykepengegrunnlagInspektør.beregningsgrunnlag)
                assertEquals(561804.årlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
                assertEquals(744000.årlig, sammenligningsgrunnlagInspektør.sammenligningsgrunnlag)
                assertEquals(FLERE_ARBEIDSGIVERE, sykepengegrunnlagInspektør.inntektskilde)
                assertEquals(FLERE_ARBEIDSGIVERE, inspektør.inntektskilde(1.vedtaksperiode))
                assertEquals(0, vilkårsgrunnlag.sykepengegrunnlag.inspektør.avviksprosent)
                assertEquals(2, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                    assertEquals(31000.månedlig, it.inntektsopplysning.inspektør.beløp)
                    assertEquals(Inntektsmelding::class, it.inntektsopplysning::class)
                }
                sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
                    assertEquals(31000.månedlig, it.inntektsopplysning.inspektør.beløp)
                    assertEquals(SkattSykepengegrunnlag::class, it.inntektsopplysning::class)
                }
                assertEquals(2, sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                    assertEquals(31000.månedlig, it.rapportertInntekt)
                }
                sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
                    assertEquals(31000.månedlig, it.rapportertInntekt)
                }
            }
        }
    }

    @Test
    fun `revurderer arbeidsforhold i AvventerHistorikkRevurdering`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT)
            håndterVilkårsgrunnlagMedGhostArbeidsforhold(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            (inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør ?: fail { "finner ikke vilkårsgrunnlag" }).also { vilkårsgrunnlag ->
                val sykepengegrunnlagInspektør = vilkårsgrunnlag.sykepengegrunnlag.inspektør
                val sammenligningsgrunnlagInspektør = vilkårsgrunnlag.sammenligningsgrunnlag.inspektør

                assertEquals(744000.årlig, sykepengegrunnlagInspektør.beregningsgrunnlag)
                assertEquals(561804.årlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
                assertEquals(744000.årlig, sammenligningsgrunnlagInspektør.sammenligningsgrunnlag)
                assertEquals(FLERE_ARBEIDSGIVERE, sykepengegrunnlagInspektør.inntektskilde)
                assertEquals(FLERE_ARBEIDSGIVERE, inspektør.inntektskilde(1.vedtaksperiode))
                assertEquals(0, vilkårsgrunnlag.sykepengegrunnlag.inspektør.avviksprosent)
                assertEquals(2, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                    assertEquals(31000.månedlig, it.inntektsopplysning.inspektør.beløp)
                    assertEquals(Inntektsmelding::class, it.inntektsopplysning::class)
                }
                sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
                    assertEquals(31000.månedlig, it.inntektsopplysning.inspektør.beløp)
                    assertEquals(SkattSykepengegrunnlag::class, it.inntektsopplysning::class)
                }
                assertEquals(2, sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                    assertEquals(31000.månedlig, it.rapportertInntekt)
                }
                sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
                    assertEquals(31000.månedlig, it.rapportertInntekt)
                }
            }
            nullstillTilstandsendringer()
            håndterOverstyrArbeidsforhold(1.januar, ArbeidsforholdOverstyrt(a2, true, "test"))
            (inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør ?: fail { "finner ikke vilkårsgrunnlag" }).also { vilkårsgrunnlag ->
                val sykepengegrunnlagInspektør = vilkårsgrunnlag.sykepengegrunnlag.inspektør
                val sammenligningsgrunnlagInspektør = vilkårsgrunnlag.sammenligningsgrunnlag.inspektør

                assertEquals(372000.årlig, sykepengegrunnlagInspektør.beregningsgrunnlag)
                assertEquals(372000.årlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
                assertEquals(744000.årlig, sammenligningsgrunnlagInspektør.sammenligningsgrunnlag)
                assertEquals(EN_ARBEIDSGIVER, sykepengegrunnlagInspektør.inntektskilde)
                assertEquals(EN_ARBEIDSGIVER, inspektør.inntektskilde(1.vedtaksperiode))
                assertEquals(50, vilkårsgrunnlag.sykepengegrunnlag.inspektør.avviksprosent)
                assertEquals(1, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                    assertEquals(31000.månedlig, it.inntektsopplysning.inspektør.beløp)
                    assertEquals(Inntektsmelding::class, it.inntektsopplysning::class)
                }
                assertEquals(2, sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                    assertEquals(31000.månedlig, it.rapportertInntekt)
                }
                sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
                    assertEquals(31000.månedlig, it.rapportertInntekt)
                }
            }
            håndterOverstyrArbeidsforhold(1.januar, ArbeidsforholdOverstyrt(a2, false, "test"))
            (inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør ?: fail { "finner ikke vilkårsgrunnlag" }).also { vilkårsgrunnlag ->
                val sykepengegrunnlagInspektør = vilkårsgrunnlag.sykepengegrunnlag.inspektør
                val sammenligningsgrunnlagInspektør = vilkårsgrunnlag.sammenligningsgrunnlag.inspektør

                assertEquals(744000.årlig, sykepengegrunnlagInspektør.beregningsgrunnlag)
                assertEquals(561804.årlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
                assertEquals(744000.årlig, sammenligningsgrunnlagInspektør.sammenligningsgrunnlag)
                assertEquals(FLERE_ARBEIDSGIVERE, sykepengegrunnlagInspektør.inntektskilde)
                assertEquals(FLERE_ARBEIDSGIVERE, inspektør.inntektskilde(1.vedtaksperiode))
                assertEquals(0, vilkårsgrunnlag.sykepengegrunnlag.inspektør.avviksprosent)
                assertEquals(2, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                    assertEquals(31000.månedlig, it.inntektsopplysning.inspektør.beløp)
                    assertEquals(Inntektsmelding::class, it.inntektsopplysning::class)
                }
                sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
                    assertEquals(31000.månedlig, it.inntektsopplysning.inspektør.beløp)
                    assertEquals(SkattSykepengegrunnlag::class, it.inntektsopplysning::class)
                }
                assertEquals(2, sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                    assertEquals(31000.månedlig, it.rapportertInntekt)
                }
                sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
                    assertEquals(31000.månedlig, it.rapportertInntekt)
                }
            }
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            assertTilstander(
                1.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_SKJØNNSMESSIG_FASTSETTELSE_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                AVSLUTTET
            )
        }
    }

    @Test
    fun `revurderer arbeidsforhold i AvventerSimuleringRevurdering`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT)
            håndterVilkårsgrunnlagMedGhostArbeidsforhold(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            (inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør ?: fail { "finner ikke vilkårsgrunnlag" }).also { vilkårsgrunnlag ->
                val sykepengegrunnlagInspektør = vilkårsgrunnlag.sykepengegrunnlag.inspektør
                val sammenligningsgrunnlagInspektør = vilkårsgrunnlag.sammenligningsgrunnlag.inspektør

                assertEquals(744000.årlig, sykepengegrunnlagInspektør.beregningsgrunnlag)
                assertEquals(561804.årlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
                assertEquals(744000.årlig, sammenligningsgrunnlagInspektør.sammenligningsgrunnlag)
                assertEquals(FLERE_ARBEIDSGIVERE, sykepengegrunnlagInspektør.inntektskilde)
                assertEquals(FLERE_ARBEIDSGIVERE, inspektør.inntektskilde(1.vedtaksperiode))
                assertEquals(0, vilkårsgrunnlag.sykepengegrunnlag.inspektør.avviksprosent)
                assertEquals(2, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                    assertEquals(31000.månedlig, it.inntektsopplysning.inspektør.beløp)
                    assertEquals(Inntektsmelding::class, it.inntektsopplysning::class)
                }
                sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
                    assertEquals(31000.månedlig, it.inntektsopplysning.inspektør.beløp)
                    assertEquals(SkattSykepengegrunnlag::class, it.inntektsopplysning::class)
                }
                assertEquals(2, sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                    assertEquals(31000.månedlig, it.rapportertInntekt)
                }
                sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
                    assertEquals(31000.månedlig, it.rapportertInntekt)
                }
            }
            nullstillTilstandsendringer()
            håndterOverstyrArbeidsforhold(1.januar, ArbeidsforholdOverstyrt(a2, true, "test"))
            (inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør ?: fail { "finner ikke vilkårsgrunnlag" }).also { vilkårsgrunnlag ->
                val sykepengegrunnlagInspektør = vilkårsgrunnlag.sykepengegrunnlag.inspektør
                val sammenligningsgrunnlagInspektør = vilkårsgrunnlag.sammenligningsgrunnlag.inspektør

                assertEquals(372000.årlig, sykepengegrunnlagInspektør.beregningsgrunnlag)
                assertEquals(372000.årlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
                assertEquals(744000.årlig, sammenligningsgrunnlagInspektør.sammenligningsgrunnlag)
                assertEquals(EN_ARBEIDSGIVER, sykepengegrunnlagInspektør.inntektskilde)
                assertEquals(EN_ARBEIDSGIVER, inspektør.inntektskilde(1.vedtaksperiode))
                assertEquals(50, vilkårsgrunnlag.sykepengegrunnlag.inspektør.avviksprosent)
                assertEquals(1, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                    assertEquals(31000.månedlig, it.inntektsopplysning.inspektør.beløp)
                    assertEquals(Inntektsmelding::class, it.inntektsopplysning::class)
                }
                assertEquals(2, sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                    assertEquals(31000.månedlig, it.rapportertInntekt)
                }
                sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
                    assertEquals(31000.månedlig, it.rapportertInntekt)
                }
            }
            håndterYtelser(1.vedtaksperiode)
            håndterOverstyrArbeidsforhold(1.januar, ArbeidsforholdOverstyrt(a2, false, "test"))
            (inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør ?: fail { "finner ikke vilkårsgrunnlag" }).also { vilkårsgrunnlag ->
                val sykepengegrunnlagInspektør = vilkårsgrunnlag.sykepengegrunnlag.inspektør
                val sammenligningsgrunnlagInspektør = vilkårsgrunnlag.sammenligningsgrunnlag.inspektør

                assertEquals(744000.årlig, sykepengegrunnlagInspektør.beregningsgrunnlag)
                assertEquals(561804.årlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
                assertEquals(744000.årlig, sammenligningsgrunnlagInspektør.sammenligningsgrunnlag)
                assertEquals(FLERE_ARBEIDSGIVERE, sykepengegrunnlagInspektør.inntektskilde)
                assertEquals(FLERE_ARBEIDSGIVERE, inspektør.inntektskilde(1.vedtaksperiode))
                assertEquals(0, vilkårsgrunnlag.sykepengegrunnlag.inspektør.avviksprosent)
                assertEquals(2, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                    assertEquals(31000.månedlig, it.inntektsopplysning.inspektør.beløp)
                    assertEquals(Inntektsmelding::class, it.inntektsopplysning::class)
                }
                sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
                    assertEquals(31000.månedlig, it.inntektsopplysning.inspektør.beløp)
                    assertEquals(SkattSykepengegrunnlag::class, it.inntektsopplysning::class)
                }
                assertEquals(2, sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                    assertEquals(31000.månedlig, it.rapportertInntekt)
                }
                sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
                    assertEquals(31000.månedlig, it.rapportertInntekt)
                }
            }
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            assertTilstander(
                1.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_SKJØNNSMESSIG_FASTSETTELSE_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                AVSLUTTET
            )
        }
    }

    @Test
    fun `revurderer arbeidsforhold i AvventerGjennomførtRevurdering`() = Toggle.TjuefemprosentAvvik.enable {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT)
            håndterVilkårsgrunnlagMedGhostArbeidsforhold(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            // ny periode
            håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Feriedag)))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            nullstillTilstandsendringer()
            håndterOverstyrArbeidsforhold(1.januar, ArbeidsforholdOverstyrt(a2, true, "test"))
            assertVarsel(RV_IV_2)
            håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT)))
            assertTilstander(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_SKJØNNSMESSIG_FASTSETTELSE_REVURDERING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
            assertTilstander(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVSLUTTET)
            assertTilstander(
                2.vedtaksperiode,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_SKJØNNSMESSIG_FASTSETTELSE_REVURDERING,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                TIL_UTBETALING,
                AVSLUTTET
            )
        }
    }

    @Test
    fun `revurderer tidligere skjæringstidspunkt`() = Toggle.TjuefemprosentAvvik.enable {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT)
            håndterVilkårsgrunnlagMedGhostArbeidsforhold(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            // ny periode med nytt skjæringstidspunlt
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
            håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
            håndterInntektsmelding(listOf(1.mars til 16.mars), beregnetInntekt = INNTEKT)
            håndterVilkårsgrunnlagMedGhostArbeidsforhold(2.vedtaksperiode, skjæringstidspunkt = 1.mars)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Feriedag)))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
            nullstillTilstandsendringer()
            håndterOverstyrArbeidsforhold(1.mars, ArbeidsforholdOverstyrt(a2, true, "test"))
            assertVarsel(RV_IV_2)
            håndterSkjønnsmessigFastsettelse(1.mars, listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT)))
            assertEquals(50, inspektør.vilkårsgrunnlag(2.vedtaksperiode)?.inspektør?.sykepengegrunnlag?.inspektør?.avviksprosent)
            assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
            (inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør ?: fail { "finner ikke vilkårsgrunnlag" }).also { vilkårsgrunnlag ->
                val sykepengegrunnlagInspektør = vilkårsgrunnlag.sykepengegrunnlag.inspektør
                val sammenligningsgrunnlagInspektør = vilkårsgrunnlag.sammenligningsgrunnlag.inspektør

                assertEquals(744000.årlig, sykepengegrunnlagInspektør.beregningsgrunnlag)
                assertEquals(561804.årlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
                assertEquals(744000.årlig, sammenligningsgrunnlagInspektør.sammenligningsgrunnlag)
                assertEquals(FLERE_ARBEIDSGIVERE, sykepengegrunnlagInspektør.inntektskilde)
                assertEquals(FLERE_ARBEIDSGIVERE, inspektør.inntektskilde(1.vedtaksperiode))
                assertEquals(0, vilkårsgrunnlag.sykepengegrunnlag.inspektør.avviksprosent)
                assertEquals(2, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                    assertEquals(31000.månedlig, it.inntektsopplysning.inspektør.beløp)
                    assertEquals(Inntektsmelding::class, it.inntektsopplysning::class)
                }
                sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
                    assertEquals(31000.månedlig, it.inntektsopplysning.inspektør.beløp)
                    assertEquals(SkattSykepengegrunnlag::class, it.inntektsopplysning::class)
                }
                assertEquals(2, sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                    assertEquals(31000.månedlig, it.rapportertInntekt)
                }
                sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
                    assertEquals(31000.månedlig, it.rapportertInntekt)
                }
            }
            (inspektør.vilkårsgrunnlag(2.vedtaksperiode)?.inspektør ?: fail { "finner ikke vilkårsgrunnlag" }).also { vilkårsgrunnlag ->
                val sykepengegrunnlagInspektør = vilkårsgrunnlag.sykepengegrunnlag.inspektør
                val sammenligningsgrunnlagInspektør = vilkårsgrunnlag.sammenligningsgrunnlag.inspektør

                assertEquals(372000.årlig, sykepengegrunnlagInspektør.beregningsgrunnlag)
                assertEquals(372000.årlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
                assertEquals(744000.årlig, sammenligningsgrunnlagInspektør.sammenligningsgrunnlag)
                assertEquals(EN_ARBEIDSGIVER, sykepengegrunnlagInspektør.inntektskilde)
                assertEquals(EN_ARBEIDSGIVER, inspektør.inntektskilde(2.vedtaksperiode))
                assertEquals(50, vilkårsgrunnlag.sykepengegrunnlag.inspektør.avviksprosent)
                assertEquals(1, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                    assertEquals(31000.månedlig, it.inntektsopplysning.inspektør.beløp)
                    assertEquals(SkjønnsmessigFastsatt::class, it.inntektsopplysning::class)
                }
                assertEquals(2, sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                    assertEquals(31000.månedlig, it.rapportertInntekt)
                }
                sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
                    assertEquals(31000.månedlig, it.rapportertInntekt)
                }
            }
            assertTilstander(
                1.vedtaksperiode,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                TIL_UTBETALING,
                AVSLUTTET
            )
            assertTilstander(
                2.vedtaksperiode,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                TIL_UTBETALING,
                AVSLUTTET
            )
        }
    }

    @Test
    fun `flere arbeidsgivere med sykdom`() = Toggle.TjuefemprosentAvvik.enable {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        }
        a1 { håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT) }
        a2 { håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT) }

        a1 {
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                arbeidsforhold = listOf(
                    Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                    Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null),
                    Vilkårsgrunnlag.Arbeidsforhold(a3, 1.desember(2017), null)
                ),
                inntektsvurdering = Inntektsvurdering(
                    listOf(
                        sammenligningsgrunnlag(a1, 1.januar, INNTEKT.repeat(12)),
                        sammenligningsgrunnlag(a2, 1.januar, INNTEKT.repeat(12)),
                        sammenligningsgrunnlag(a3, 1.januar, INNTEKT.repeat(12))
                    )
                ),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                    inntekter = listOf(
                        grunnlag(a1, 1.januar, INNTEKT.repeat(3)),
                        grunnlag(a2, 1.januar, INNTEKT.repeat(3)),
                        grunnlag(a3, 1.januar, INNTEKT.repeat(3))
                    ),
                    arbeidsforhold = emptyList()
                )
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        (inspiser(personInspektør).vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar)?.inspektør ?: fail { "finner ikke vilkårsgrunnlag" }).also { vilkårsgrunnlag ->
            val sykepengegrunnlagInspektør = vilkårsgrunnlag.sykepengegrunnlag.inspektør
            val sammenligningsgrunnlagInspektør = vilkårsgrunnlag.sammenligningsgrunnlag.inspektør

            assertEquals(1116000.årlig, sykepengegrunnlagInspektør.beregningsgrunnlag)
            assertEquals(561804.årlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
            assertEquals(1116000.årlig, sammenligningsgrunnlagInspektør.sammenligningsgrunnlag)
            assertEquals(FLERE_ARBEIDSGIVERE, sykepengegrunnlagInspektør.inntektskilde)
            a1 { assertEquals(FLERE_ARBEIDSGIVERE, inspektør.inntektskilde(1.vedtaksperiode)) }
            a2 { assertEquals(FLERE_ARBEIDSGIVERE, inspektør.inntektskilde(1.vedtaksperiode)) }
            assertEquals(0, vilkårsgrunnlag.sykepengegrunnlag.inspektør.avviksprosent)
            assertEquals(3, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
            sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                assertEquals(31000.månedlig, it.inntektsopplysning.inspektør.beløp)
                assertEquals(Inntektsmelding::class, it.inntektsopplysning::class)
            }
            sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
                assertEquals(31000.månedlig, it.inntektsopplysning.inspektør.beløp)
                assertEquals(Inntektsmelding::class, it.inntektsopplysning::class)
            }
            sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a3).inspektør.also {
                assertEquals(31000.månedlig, it.inntektsopplysning.inspektør.beløp)
                assertEquals(SkattSykepengegrunnlag::class, it.inntektsopplysning::class)
            }
            assertEquals(3, sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
            sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                assertEquals(31000.månedlig, it.rapportertInntekt)
            }
            sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
                assertEquals(31000.månedlig, it.rapportertInntekt)
            }
            sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a3).inspektør.also {
                assertEquals(31000.månedlig, it.rapportertInntekt)
            }
        }
        nullstillTilstandsendringer()
        håndterOverstyrArbeidsforhold(1.januar, ArbeidsforholdOverstyrt(a3, true, "test"))

        a1 {
            assertVarsel(RV_IV_2)
            håndterSkjønnsmessigFastsettelse(1.januar, listOf(
                OverstyrtArbeidsgiveropplysning(a1, 31000.månedlig),
                OverstyrtArbeidsgiveropplysning(a2, 31000.månedlig)
            ))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a1 {
            assertTilstander(
                1.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_SKJØNNSMESSIG_FASTSETTELSE_REVURDERING,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                TIL_UTBETALING,
                AVSLUTTET
            )
        }
        a2 {
            assertTilstander(
                1.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                TIL_UTBETALING,
                AVSLUTTET
            )
        }
        (inspiser(personInspektør).vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar)?.inspektør ?: fail { "finner ikke vilkårsgrunnlag" }).also { vilkårsgrunnlag ->
            val sykepengegrunnlagInspektør = vilkårsgrunnlag.sykepengegrunnlag.inspektør
            val sammenligningsgrunnlagInspektør = vilkårsgrunnlag.sammenligningsgrunnlag.inspektør

            assertEquals(744000.årlig, sykepengegrunnlagInspektør.beregningsgrunnlag)
            assertEquals(561804.årlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
            assertEquals(1116000.årlig, sammenligningsgrunnlagInspektør.sammenligningsgrunnlag)
            assertEquals(FLERE_ARBEIDSGIVERE, sykepengegrunnlagInspektør.inntektskilde)
            a1 { assertEquals(FLERE_ARBEIDSGIVERE, inspektør.inntektskilde(1.vedtaksperiode)) }
            a2 { assertEquals(FLERE_ARBEIDSGIVERE, inspektør.inntektskilde(1.vedtaksperiode)) }
            assertEquals(33, vilkårsgrunnlag.sykepengegrunnlag.inspektør.avviksprosent)
            assertEquals(2, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
            sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                assertEquals(31000.månedlig, it.inntektsopplysning.inspektør.beløp)
                assertEquals(SkjønnsmessigFastsatt::class, it.inntektsopplysning::class)
            }
            sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
                assertEquals(31000.månedlig, it.inntektsopplysning.inspektør.beløp)
                assertEquals(SkjønnsmessigFastsatt::class, it.inntektsopplysning::class)
            }
            assertEquals(3, sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
            sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                assertEquals(31000.månedlig, it.rapportertInntekt)
            }
            sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
                assertEquals(31000.månedlig, it.rapportertInntekt)
            }
            sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a3).inspektør.also {
                assertEquals(31000.månedlig, it.rapportertInntekt)
            }
        }
    }

    @Test
    fun `over 6G -- deaktiverer og aktiverer arbeidsforhold medfører tilbakekreving`() {
        val inntekt = 33000.månedlig
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = inntekt)
            håndterVilkårsgrunnlagMedGhostArbeidsforhold(1.vedtaksperiode, inntekt = inntekt)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertPeriode(17.januar til 31.januar, 1080.daglig)
            håndterOverstyrArbeidsforhold(1.januar, ArbeidsforholdOverstyrt(a2, deaktivert = true, "deaktiverer a2"))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertPeriode(17.januar til 31.januar, 1523.daglig)
            håndterOverstyrArbeidsforhold(1.januar, ArbeidsforholdOverstyrt(a2, deaktivert = false, "aktiverer a2 igjen"))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertPeriode(17.januar til 31.januar, 1080.daglig)
            assertInfo("En endring hos en arbeidsgiver har medført at det trekkes tilbake penger hos andre arbeidsgivere", person())
        }
    }

    @Test
    fun `over 6G -- deaktiverer og aktiverer arbeidsforhold medfører tilbakekreving flere arbeidsgivere`() {
        val inntekt = 23000.månedlig
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        }
        a1 { håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = inntekt) }
        a2 { håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = inntekt) }

        a1 {
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                arbeidsforhold = listOf(
                    Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                    Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null),
                    Vilkårsgrunnlag.Arbeidsforhold(a3, 1.desember(2017), null)
                ),
                inntektsvurdering = Inntektsvurdering(
                    listOf(
                        sammenligningsgrunnlag(a1, 1.januar, inntekt.repeat(12)),
                        sammenligningsgrunnlag(a2, 1.januar, inntekt.repeat(12)),
                        sammenligningsgrunnlag(a3, 1.januar, inntekt.repeat(12))
                    )
                ),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                    inntekter = listOf(
                        grunnlag(a1, 1.januar, inntekt.repeat(3)),
                        grunnlag(a2, 1.januar, inntekt.repeat(3)),
                        grunnlag(a3, 1.januar, inntekt.repeat(3))
                    ),
                    arbeidsforhold = emptyList()
                )
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        håndterOverstyrArbeidsforhold(1.januar, ArbeidsforholdOverstyrt(a3, true, "deaktiverer a3"))

        (a1 og a2) {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        }
        håndterOverstyrArbeidsforhold(1.januar, ArbeidsforholdOverstyrt(a3, false, "aktiverer a3 igjen"))

        (a1 og a2) {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertInfo("En endring hos en arbeidsgiver har medført at det trekkes tilbake penger hos andre arbeidsgivere", person())
        }
    }

    private fun TestPerson.TestArbeidsgiver.assertDag(dato: LocalDate, arbeidsgiverbeløp: Inntekt, personbeløp: Inntekt) {
        inspektør(orgnummer).sisteUtbetalingUtbetalingstidslinje()[dato].let {
            if (it is Utbetalingsdag.NavHelgDag) return
            assertEquals(arbeidsgiverbeløp, it.økonomi.inspektør.arbeidsgiverbeløp)
            assertEquals(personbeløp, it.økonomi.inspektør.personbeløp)
        }
    }
    private fun TestPerson.TestArbeidsgiver.assertPeriode(
        periode: Periode,
        arbeidsgiverbeløp: Inntekt,
        personbeløp: Inntekt = Inntekt.INGEN
    ) =
        periode.forEach { assertDag(it, arbeidsgiverbeløp, personbeløp) }


    private fun TestPerson.TestArbeidsgiver.håndterVilkårsgrunnlagMedGhostArbeidsforhold(vedtaksperiode: UUID, skjæringstidspunkt: LocalDate = 1.januar, inntekt: Inntekt = INNTEKT) {
        håndterVilkårsgrunnlag(
            vedtaksperiode,
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Vilkårsgrunnlag.Arbeidsforhold(a2, 1.desember(2017), null)
            ),
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, skjæringstidspunkt, inntekt.repeat(12)),
                    sammenligningsgrunnlag(a2, skjæringstidspunkt, inntekt.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = listOf(
                    grunnlag(a1, skjæringstidspunkt, inntekt.repeat(3)),
                    grunnlag(a2, skjæringstidspunkt, inntekt.repeat(3))
                ),
                arbeidsforhold = emptyList()
            )
        )
    }
}
