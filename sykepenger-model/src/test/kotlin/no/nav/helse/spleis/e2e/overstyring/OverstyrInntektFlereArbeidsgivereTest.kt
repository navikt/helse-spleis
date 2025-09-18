package no.nav.helse.spleis.e2e.overstyring

import java.time.LocalDate
import no.nav.helse.dsl.Arbeidstakerkilde
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SV_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_2
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertIngenFunksjonelleFeil
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.håndterArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterOverstyrInntekt
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlagFlereArbeidsgivere
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.tilGodkjenning
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class OverstyrInntektFlereArbeidsgivereTest : AbstractEndToEndTest() {

    @Test
    fun `overstyr inntekt med flere AG -- happy case`() {
        tilGodkjenning(januar, a1, a2)
        assertTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)

        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
            assertInntektsgrunnlag(a1, INNTEKT)
            assertInntektsgrunnlag(a2, INNTEKT)
        }
        håndterOverstyrInntekt(19000.månedlig, a1, 1.januar)
        this@OverstyrInntektFlereArbeidsgivereTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)

        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
            assertInntektsgrunnlag(a1, INNTEKT, 19000.månedlig, forventetKorrigertInntekt = 19000.månedlig)
            assertInntektsgrunnlag(a2, INNTEKT)
        }
    }

    @Test
    fun `overstyr inntekt med flere AG -- kan overstyre perioden i AvventerBlokkerende`() {
        tilGodkjenning(januar, a1, a2)
        assertTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)

        håndterOverstyrInntekt(19000.månedlig, a2, 1.januar)
        assertIngenFunksjonelleFeil()
        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
            assertInntektsgrunnlag(a1, INNTEKT)
            assertInntektsgrunnlag(a2, INNTEKT, 19000.månedlig, forventetKorrigertInntekt = 19000.månedlig)
        }
    }

    @Test
    fun `skal ikke kunne overstyre en arbeidsgiver hvis en annen er utbetalt`() {
        tilGodkjenning(januar, a1, a2)
        this@OverstyrInntektFlereArbeidsgivereTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)
        this@OverstyrInntektFlereArbeidsgivereTest.håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterOverstyrInntekt(19000.månedlig, a2, 1.januar)
        assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
            assertInntektsgrunnlag(a1, INNTEKT)
            assertInntektsgrunnlag(a2, INNTEKT, 19000.månedlig, forventetKorrigertInntekt = 19000.månedlig)
        }
    }

    @Test
    fun `flere arbeidsgivere med ghost - overstyrer inntekt til arbeidsgiver med sykdom -- happy case`() {
        tilOverstyring()
        håndterOverstyrInntekt(29000.månedlig, a1, 1.januar)

        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
            assertInntektsgrunnlag(a1, INNTEKT, 29000.månedlig, forventetKorrigertInntekt = 29000.månedlig)
            assertInntektsgrunnlag(a2, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
        }

        nullstillTilstandsendringer()
        this@OverstyrInntektFlereArbeidsgivereTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
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
    fun `overstyrer inntekt til under krav til minste inntekt`() {
        tilGodkjenning(januar, a1, a2, beregnetInntekt = 1959.månedlig)
        håndterOverstyrInntekt(1500.månedlig, a1, 1.januar)
        this@OverstyrInntektFlereArbeidsgivereTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        assertVarsel(RV_SV_1, 1.vedtaksperiode.filter(a1))
        assertIngenFunksjonelleFeil()
        assertTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
    }

    @Test
    fun `overstyring av inntekt kan føre til brukerutbetaling`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT / 4,
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT / 4,
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )

        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1)
        this@OverstyrInntektFlereArbeidsgivereTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterOverstyrInntekt(8000.månedlig, skjæringstidspunkt = 1.januar, orgnummer = a1)
        this@OverstyrInntektFlereArbeidsgivereTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        inspektør(a1).utbetaling(1).also { utbetaling ->
            assertEquals(1, utbetaling.arbeidsgiverOppdrag.size)
            utbetaling.arbeidsgiverOppdrag[0].inspektør.also { linje ->
                assertEquals(358, linje.beløp)
                assertEquals(17.januar til 31.januar, linje.fom til linje.tom)
            }
            assertEquals(1, utbetaling.personOppdrag.size)
            utbetaling.personOppdrag[0].inspektør.also { linje ->
                assertEquals(12, linje.beløp)
                assertEquals(17.januar til 31.januar, linje.fom til linje.tom)
            }
        }
        inspektør(a2).utbetaling(1).also { utbetaling ->
            assertEquals(1, utbetaling.arbeidsgiverOppdrag.size)
            utbetaling.arbeidsgiverOppdrag[0].inspektør.also { linje ->
                assertEquals(358, linje.beløp)
                assertEquals(17.januar til 31.januar, linje.fom til linje.tom)
            }
            assertTrue(utbetaling.personOppdrag.isEmpty())
        }
    }

    private fun tilOverstyring(fom: LocalDate = 1.januar, tom: LocalDate = 31.januar) {
        håndterSykmelding(Sykmeldingsperiode(fom, tom), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(fom, tom, 100.prosent), orgnummer = a1)
        håndterArbeidsgiveropplysninger(
            listOf(fom til fom.plusDays(15)),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))

        this@OverstyrInntektFlereArbeidsgivereTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
    }
}
