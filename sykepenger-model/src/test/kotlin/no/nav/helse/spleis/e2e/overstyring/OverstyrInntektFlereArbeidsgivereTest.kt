package no.nav.helse.spleis.e2e.overstyring

import java.time.LocalDate
import no.nav.helse.assertForventetFeil
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.november
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.Inntektskilde.FLERE_ARBEIDSGIVERE
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.Varselkode.RV_IV_2
import no.nav.helse.person.Varselkode.RV_SV_1
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertFunksjonellFeil
import no.nav.helse.spleis.e2e.assertIngenFunksjonelleFeil
import no.nav.helse.spleis.e2e.assertInntektForDato
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.grunnlag
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrInntekt
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyeVedtak
import no.nav.helse.spleis.e2e.repeat
import no.nav.helse.spleis.e2e.sammenligningsgrunnlag
import no.nav.helse.spleis.e2e.tilGodkjenning
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

internal class OverstyrInntektFlereArbeidsgivereTest: AbstractEndToEndTest() {

    @Test
    fun `overstyr inntekt med flere AG -- happy case`() {
        tilGodkjenning(1.januar, 31.januar, a1, a2)
        assertTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)

        assertInntektForDato(20000.månedlig, 1.januar, inspektør = inspektør(a1))
        assertInntektForDato(20000.månedlig, 1.januar, inspektør = inspektør(a2))

        (inspektør(a1).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør ?: fail { "finner ikke vilkårsgrunnlag" }).also { vilkårsgrunnlag ->
            val sykepengegrunnlagInspektør = vilkårsgrunnlag.sykepengegrunnlag.inspektør
            val sammenligningsgrunnlagInspektør = vilkårsgrunnlag.sammenligningsgrunnlag1.inspektør

            assertEquals(480000.årlig, sykepengegrunnlagInspektør.beregningsgrunnlag)
            assertEquals(480000.årlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
            assertEquals(480000.årlig, sammenligningsgrunnlagInspektør.sammenligningsgrunnlag)
            assertEquals(FLERE_ARBEIDSGIVERE, sykepengegrunnlagInspektør.inntektskilde)
            assertEquals(FLERE_ARBEIDSGIVERE, inspektør(a1).inntektskilde(1.vedtaksperiode))
            assertEquals(0, vilkårsgrunnlag.avviksprosent?.roundToInt())
            assertEquals(2, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
            sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                assertEquals(20000.månedlig, it.inntektsopplysning.omregnetÅrsinntekt())
                assertEquals(Inntektshistorikk.Inntektsmelding::class, it.inntektsopplysning::class)
            }
            sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
                assertEquals(20000.månedlig, it.inntektsopplysning.omregnetÅrsinntekt())
                assertEquals(Inntektshistorikk.Inntektsmelding::class, it.inntektsopplysning::class)
            }

            assertEquals(2, sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
            sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                assertEquals(Inntektshistorikk.SkattComposite::class, it.inntektsopplysning::class)
            }
            sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
                assertEquals(Inntektshistorikk.SkattComposite::class, it.inntektsopplysning::class)
            }
        }

        håndterOverstyrInntekt(19000.månedlig, a1, 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)

        (inspektør(a1).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør ?: fail { "finner ikke vilkårsgrunnlag" }).also { vilkårsgrunnlag ->
            val sykepengegrunnlagInspektør = vilkårsgrunnlag.sykepengegrunnlag.inspektør
            val sammenligningsgrunnlagInspektør = vilkårsgrunnlag.sammenligningsgrunnlag1.inspektør

            assertEquals(468000.årlig, sykepengegrunnlagInspektør.beregningsgrunnlag)
            assertEquals(468000.årlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
            assertEquals(480000.årlig, sammenligningsgrunnlagInspektør.sammenligningsgrunnlag)
            assertEquals(FLERE_ARBEIDSGIVERE, sykepengegrunnlagInspektør.inntektskilde)
            assertEquals(FLERE_ARBEIDSGIVERE, inspektør(a1).inntektskilde(1.vedtaksperiode))
            assertEquals(3, vilkårsgrunnlag.avviksprosent?.roundToInt())
            assertEquals(2, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
            sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                assertEquals(19000.månedlig, it.inntektsopplysning.omregnetÅrsinntekt())
                assertEquals(Inntektshistorikk.Saksbehandler::class, it.inntektsopplysning::class)
            }
            sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
                assertEquals(20000.månedlig, it.inntektsopplysning.omregnetÅrsinntekt())
                assertEquals(Inntektshistorikk.Inntektsmelding::class, it.inntektsopplysning::class)
            }

            assertEquals(2, sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
            sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
                assertEquals(Inntektshistorikk.SkattComposite::class, it.inntektsopplysning::class)
            }
            sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
                assertEquals(Inntektshistorikk.SkattComposite::class, it.inntektsopplysning::class)
            }
        }
    }

    @Test
    fun `overstyr inntekt med flere AG -- kan ikke overstyre perioden i AvventerBlokkerende`() {
        tilGodkjenning(1.januar, 31.januar, a1, a2)
        assertTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)

        håndterOverstyrInntekt(19000.månedlig, a2, 1.januar)
        assertIngenFunksjonelleFeil()
        assertInntektForDato(20000.månedlig, 1.januar, inspektør = inspektør(a1))
        assertInntektForDato(20000.månedlig, 1.januar, inspektør = inspektør(a2))
        val vilkårsgrunnlag = inspektør(a1).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør ?: fail { "finner ikke vilkårsgrunnlag" }
        val sykepengegrunnlagInspektør = vilkårsgrunnlag.sykepengegrunnlag.inspektør
        val sammenligningsgrunnlagInspektør = vilkårsgrunnlag.sammenligningsgrunnlag1.inspektør

        assertEquals(480000.årlig, sykepengegrunnlagInspektør.beregningsgrunnlag)
        assertEquals(480000.årlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
        assertEquals(480000.årlig, sammenligningsgrunnlagInspektør.sammenligningsgrunnlag)
        assertEquals(FLERE_ARBEIDSGIVERE, sykepengegrunnlagInspektør.inntektskilde)
        assertEquals(FLERE_ARBEIDSGIVERE, inspektør(a1).inntektskilde(1.vedtaksperiode))
        assertEquals(0, vilkårsgrunnlag.avviksprosent?.roundToInt())
        assertEquals(2, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
        sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
            assertEquals(20000.månedlig, it.inntektsopplysning.omregnetÅrsinntekt())
            assertEquals(Inntektshistorikk.Inntektsmelding::class, it.inntektsopplysning::class)
        }
        sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
            assertEquals(20000.månedlig, it.inntektsopplysning.omregnetÅrsinntekt())
            assertEquals(Inntektshistorikk.Inntektsmelding::class, it.inntektsopplysning::class)
        }

        assertEquals(2, sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
        sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
            assertEquals(Inntektshistorikk.SkattComposite::class, it.inntektsopplysning::class)
        }
        sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
            assertEquals(Inntektshistorikk.SkattComposite::class, it.inntektsopplysning::class)
        }
    }

    @Test
    fun `skal ikke kunne overstyre en arbeidsgiver hvis en annen er utbetalt`() {
        tilGodkjenning(1.januar, 31.januar, a1, a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterOverstyrInntekt(19000.månedlig, a2, 1.januar)
        assertForventetFeil(
            forklaring = "Dette burde støttes av at vi går inn i et revurderingsløp",
            nå = {
                assertTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
                assertTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING, orgnummer = a2)
                assertInntektForDato(20000.månedlig, 1.januar, inspektør = inspektør(a2))
            },
            ønsket = {
                assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a1)
                assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
                assertInntektForDato(19000.månedlig, 1.januar, inspektør = inspektør(a2))
            }
        )
        assertInntektForDato(20000.månedlig, 1.januar, inspektør = inspektør(a1))
        val vilkårsgrunnlag = inspektør(a1).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør ?: fail { "finner ikke vilkårsgrunnlag" }
        val sykepengegrunnlagInspektør = vilkårsgrunnlag.sykepengegrunnlag.inspektør
        val sammenligningsgrunnlagInspektør = vilkårsgrunnlag.sammenligningsgrunnlag1.inspektør

        assertEquals(480000.årlig, sykepengegrunnlagInspektør.beregningsgrunnlag)
        assertEquals(480000.årlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
        assertEquals(480000.årlig, sammenligningsgrunnlagInspektør.sammenligningsgrunnlag)
        assertEquals(FLERE_ARBEIDSGIVERE, sykepengegrunnlagInspektør.inntektskilde)
        assertEquals(FLERE_ARBEIDSGIVERE, inspektør(a1).inntektskilde(1.vedtaksperiode))
        assertEquals(0, vilkårsgrunnlag.avviksprosent?.roundToInt())
        assertEquals(2, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
        sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
            assertEquals(20000.månedlig, it.inntektsopplysning.omregnetÅrsinntekt())
            assertEquals(Inntektshistorikk.Inntektsmelding::class, it.inntektsopplysning::class)
        }
        sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
            assertEquals(20000.månedlig, it.inntektsopplysning.omregnetÅrsinntekt())
            assertEquals(Inntektshistorikk.Inntektsmelding::class, it.inntektsopplysning::class)
        }

        assertEquals(2, sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
        sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
            assertEquals(Inntektshistorikk.SkattComposite::class, it.inntektsopplysning::class)
        }
        sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
            assertEquals(Inntektshistorikk.SkattComposite::class, it.inntektsopplysning::class)
        }
    }

    @Test
    fun `flere arbeidsgivere med ghost - overstyrer inntekt til arbeidsgiver med sykdom -- happy case`() {
        tilOverstyring(
            sammenligningsgrunnlag = mapOf(a1 to 30000.månedlig, a2 to 1000.månedlig),
            sykepengegrunnlag = mapOf(a1 to 30000.månedlig, a2 to 1000.månedlig),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Vilkårsgrunnlag.Arbeidsforhold(a2, 1.november(2017), null)
            )
        )
        håndterOverstyrInntekt(29000.månedlig, a1, 1.januar)

        val vilkårsgrunnlag = inspektør(a1).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør ?: fail { "finner ikke vilkårsgrunnlag" }
        val sykepengegrunnlagInspektør = vilkårsgrunnlag.sykepengegrunnlag.inspektør
        val sammenligningsgrunnlagInspektør = vilkårsgrunnlag.sammenligningsgrunnlag1.inspektør

        assertEquals(360000.årlig, sykepengegrunnlagInspektør.beregningsgrunnlag)
        assertEquals(360000.årlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
        assertEquals(372000.årlig, sammenligningsgrunnlagInspektør.sammenligningsgrunnlag)
        assertEquals(FLERE_ARBEIDSGIVERE, sykepengegrunnlagInspektør.inntektskilde)
        assertEquals(FLERE_ARBEIDSGIVERE, inspektør(a1).inntektskilde(1.vedtaksperiode))
        assertEquals(3, vilkårsgrunnlag.avviksprosent?.roundToInt())
        assertEquals(2, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
        sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
            assertEquals(29000.månedlig, it.inntektsopplysning.omregnetÅrsinntekt())
            assertEquals(Inntektshistorikk.Saksbehandler::class, it.inntektsopplysning::class)
        }
        sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
            assertEquals(1000.månedlig, it.inntektsopplysning.omregnetÅrsinntekt())
            assertEquals(Inntektshistorikk.SkattComposite::class, it.inntektsopplysning::class)
        }

        assertEquals(2, sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
        sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
            assertEquals(Inntektshistorikk.SkattComposite::class, it.inntektsopplysning::class)
        }
        sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
            assertEquals(Inntektshistorikk.SkattComposite::class, it.inntektsopplysning::class)
        }

        nullstillTilstandsendringer()
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        assertTilstander(
            1.vedtaksperiode,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            orgnummer = a1
        )
    }

    @Test
    fun `overstyring av inntekt for flere arbeidsgivere som fører til 25 prosent avvik skal gi error -- ghost`() {
        tilOverstyring(
            sammenligningsgrunnlag = mapOf(a1 to 30000.månedlig, a2 to 1000.månedlig),
            sykepengegrunnlag = mapOf(a1 to 30000.månedlig, a2 to 1000.månedlig),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Vilkårsgrunnlag.Arbeidsforhold(a2, 1.november(2017), null)
            )
        )
        håndterOverstyrInntekt(10000.månedlig, a1, 1.januar)

        val vilkårsgrunnlag = inspektør(a1).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør ?: fail { "finner ikke vilkårsgrunnlag" }
        val sykepengegrunnlagInspektør = vilkårsgrunnlag.sykepengegrunnlag.inspektør
        val sammenligningsgrunnlagInspektør = vilkårsgrunnlag.sammenligningsgrunnlag1.inspektør

        assertEquals(132000.årlig, sykepengegrunnlagInspektør.beregningsgrunnlag)
        assertEquals(132000.årlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
        assertEquals(372000.årlig, sammenligningsgrunnlagInspektør.sammenligningsgrunnlag)
        assertEquals(FLERE_ARBEIDSGIVERE, sykepengegrunnlagInspektør.inntektskilde)
        assertEquals(FLERE_ARBEIDSGIVERE, inspektør(a1).inntektskilde(1.vedtaksperiode))
        assertEquals(65, vilkårsgrunnlag.avviksprosent?.roundToInt())
        assertEquals(2, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
        sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
            assertEquals(10000.månedlig, it.inntektsopplysning.omregnetÅrsinntekt())
            assertEquals(Inntektshistorikk.Saksbehandler::class, it.inntektsopplysning::class)
        }
        sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
            assertEquals(1000.månedlig, it.inntektsopplysning.omregnetÅrsinntekt())
            assertEquals(Inntektshistorikk.SkattComposite::class, it.inntektsopplysning::class)
        }

        assertEquals(2, sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
        sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
            assertEquals(Inntektshistorikk.SkattComposite::class, it.inntektsopplysning::class)
        }
        sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
            assertEquals(Inntektshistorikk.SkattComposite::class, it.inntektsopplysning::class)
        }

        nullstillTilstandsendringer()
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        assertVarsel(RV_IV_2, 1.vedtaksperiode.filter())
        assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, orgnummer = a1)
    }

    @Test
    fun `overstyring av inntekt for flere arbeidsgivere som fører til 25 prosent avvik skal gi error`() {
        tilGodkjenning(1.januar, 31.januar, a1, a2)
        håndterOverstyrInntekt(9999.månedlig, a1, 1.januar)

        val vilkårsgrunnlag = inspektør(a1).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør ?: fail { "finner ikke vilkårsgrunnlag" }
        val sykepengegrunnlagInspektør = vilkårsgrunnlag.sykepengegrunnlag.inspektør
        val sammenligningsgrunnlagInspektør = vilkårsgrunnlag.sammenligningsgrunnlag1.inspektør

        assertEquals(359988.årlig, sykepengegrunnlagInspektør.beregningsgrunnlag)
        assertEquals(359988.årlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
        assertEquals(480000.årlig, sammenligningsgrunnlagInspektør.sammenligningsgrunnlag)
        assertEquals(FLERE_ARBEIDSGIVERE, sykepengegrunnlagInspektør.inntektskilde)
        assertEquals(FLERE_ARBEIDSGIVERE, inspektør(a1).inntektskilde(1.vedtaksperiode))
        assertEquals(25, vilkårsgrunnlag.avviksprosent?.roundToInt())
        assertEquals(2, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
        sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
            assertEquals(9999.månedlig, it.inntektsopplysning.omregnetÅrsinntekt())
            assertEquals(Inntektshistorikk.Saksbehandler::class, it.inntektsopplysning::class)
        }
        sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
            assertEquals(20000.månedlig, it.inntektsopplysning.omregnetÅrsinntekt())
            assertEquals(Inntektshistorikk.Inntektsmelding::class, it.inntektsopplysning::class)
        }

        assertEquals(2, sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
        sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
            assertEquals(Inntektshistorikk.SkattComposite::class, it.inntektsopplysning::class)
        }
        sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
            assertEquals(Inntektshistorikk.SkattComposite::class, it.inntektsopplysning::class)
        }


        nullstillTilstandsendringer()
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        assertVarsel(RV_IV_2, 1.vedtaksperiode.filter())
        assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
    }

    @Test
    fun `overstyrer inntekt til under krav til minste inntekt`() {
        tilGodkjenning(1.januar, 31.januar, a1, a2, beregnetInntekt = 1959.månedlig) {
            1.januar.minusYears(1) til 1.januar.minusMonths(1) inntekter {
                listOf(a1, a2).forEach {
                    it inntekt 1959.månedlig
                }
            }
        }
        håndterOverstyrInntekt(1500.månedlig, a1, 1.januar)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        assertVarsel(RV_SV_1, 1.vedtaksperiode.filter(a1))
        assertIngenFunksjonelleFeil()
        assertTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
    }

    @Test
    fun `overstyring av inntekt kan føre til brukerutbetaling`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1, beregnetInntekt = INNTEKT /4)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a2, beregnetInntekt = INNTEKT /4)

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        val inntektsvurdering = Inntektsvurdering(
            listOf(
                sammenligningsgrunnlag(a1, 1.januar, (INNTEKT /4).repeat(12)),
                sammenligningsgrunnlag(a2, 1.januar, (INNTEKT /4).repeat(12))
            )
        )

        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1, inntektsvurdering = inntektsvurdering)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterOverstyrInntekt(8000.månedlig, skjæringstidspunkt = 1.januar, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        inspektør(a1).utbetaling(1).also { utbetaling ->
            assertEquals(1, utbetaling.inspektør.arbeidsgiverOppdrag.size)
            utbetaling.inspektør.arbeidsgiverOppdrag[0].inspektør.also { linje ->
                assertEquals(358, linje.beløp)
                assertEquals(17.januar til 31.januar, linje.fom til linje.tom)
            }
            assertEquals(1, utbetaling.inspektør.personOppdrag.size)
            utbetaling.inspektør.personOppdrag[0].inspektør.also { linje ->
                assertEquals(11, linje.beløp)
                assertEquals(17.januar til 31.januar, linje.fom til linje.tom)
            }
        }
        inspektør(a2).utbetaling(1).also { utbetaling ->
            assertEquals(1, utbetaling.inspektør.arbeidsgiverOppdrag.size)
            utbetaling.inspektør.arbeidsgiverOppdrag[0].inspektør.also { linje ->
                assertEquals(358, linje.beløp)
                assertEquals(17.januar til 31.januar, linje.fom til linje.tom)
            }
            assertTrue(utbetaling.inspektør.personOppdrag.isEmpty())
        }
    }

    @Test
    fun `Skal ikke revurdere inntekt for flere arbeidsgivere`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        håndterOverstyrInntekt(19000.månedlig, skjæringstidspunkt = 1.januar, orgnummer = a1)
        assertFunksjonellFeil("Forespurt revurdering av inntekt hvor personen har flere arbeidsgivere (inkl. ghosts)")
    }

    private fun tilOverstyring(
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        sammenligningsgrunnlag: Map<String, Inntekt>,
        sykepengegrunnlag: Map<String, Inntekt>,
        arbeidsforhold: List<Vilkårsgrunnlag.Arbeidsforhold>,
        beregnetInntekt: Inntekt = INNTEKT
    ) {
        håndterSykmelding(Sykmeldingsperiode(fom, tom, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(fom, tom, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(fom til fom.plusDays(15)), beregnetInntekt = beregnetInntekt, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        val inntektsvurdering = sammenligningsgrunnlag.keys.map { orgnummer ->
            sammenligningsgrunnlag(orgnummer, fom, sammenligningsgrunnlag[orgnummer]!!.repeat(12))
        }
        val inntektForSykepengegrunnlag = sykepengegrunnlag.keys.map { orgnummer ->
            grunnlag(orgnummer, fom, sykepengegrunnlag[orgnummer]!!.repeat(3))
        }
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(inntektsvurdering),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = inntektForSykepengegrunnlag, arbeidsforhold = emptyList()),
            arbeidsforhold = arbeidsforhold,
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
    }

}