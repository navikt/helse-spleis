package no.nav.helse.spleis.e2e.arbeidsgiveropplysninger

import java.util.*
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.hendelser.Arbeidsgiveropplysning.Begrunnelse.ManglerOpptjening
import no.nav.helse.hendelser.Arbeidsgiveropplysning.Begrunnelse.StreikEllerLockout
import no.nav.helse.hendelser.Arbeidsgiveropplysning.OppgittArbeidgiverperiode
import no.nav.helse.hendelser.Arbeidsgiveropplysning.OppgittInntekt
import no.nav.helse.hendelser.Arbeidsgiveropplysning.OppgittRefusjon
import no.nav.helse.hendelser.Arbeidsgiveropplysning.OpphørAvNaturalytelser
import no.nav.helse.hendelser.Arbeidsgiveropplysning.RedusertUtbetaltBeløpIArbeidsgiverperioden
import no.nav.helse.hendelser.Arbeidsgiveropplysning.UtbetaltDelerAvArbeidsgiverperioden
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_24
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_7
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_8
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.arbeidsgiver
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.assertBeløpstidslinje
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class KorrigerteArbeidsigveropplysningerTest : AbstractDslTest() {

    @Test
    fun `opplyser om korrigerert inntekt på en allerede utbetalt periode`() {
        a1 {
            nyttVedtak(januar)
            val refusjonFørKorrigering = inspektør.refusjon(1.vedtaksperiode)
            håndterKorrigerteArbeidsgiveropplysninger(1.vedtaksperiode, OppgittInntekt(INNTEKT * 1.25))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT * 1.25)
            }
            assertEquals(refusjonFørKorrigering, inspektør.refusjon(1.vedtaksperiode))
        }
    }

    @Test
    fun `opplyser om korrigerert refusjon på en allerede utbetalt periode`() {
        a1 {
            nyttVedtak(januar)
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT)
            }
            val korrigerteArbeidsgiveropplysninger = håndterKorrigerteArbeidsgiveropplysninger(1.vedtaksperiode, OppgittRefusjon(INNTEKT * 1.25, emptyList()))
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT)
            }
            assertBeløpstidslinje(Beløpstidslinje.fra(januar, INNTEKT * 1.25, korrigerteArbeidsgiveropplysninger.arbeidsgiver), inspektør.refusjon(1.vedtaksperiode))
        }
    }

    @Test
    fun `opplyser om korrigerert inntekt OG refusjon på en allerede utbetalt periode`() {
        a1 {
            nyttVedtak(januar)
            val korrigerteArbeidsgiveropplysninger = håndterKorrigerteArbeidsgiveropplysninger(1.vedtaksperiode, OppgittInntekt(INNTEKT * 1.25), OppgittRefusjon(INNTEKT * 1.25, emptyList()))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT * 1.25)
            }
            assertBeløpstidslinje(Beløpstidslinje.fra(januar, INNTEKT * 1.25, korrigerteArbeidsgiveropplysninger.arbeidsgiver), inspektør.refusjon(1.vedtaksperiode))
        }
    }

    @Test
    fun `korrigerende opplysninger mens vi venter på forespurte opplysninger`() {
        a1 {
            håndterSøknad(januar)
            assertThrows<IllegalStateException> {
                håndterKorrigerteArbeidsgiveropplysninger(1.vedtaksperiode, OppgittInntekt(INNTEKT * 1.25))
            }
        }
    }

    @Test
    fun `opplyser om korrigerert inntekt OG refusjon på en allerede utbetalt periode - men beløpene er uendret`() {
        a1 {
            nyttVedtak(januar)
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT)
            }
            val korrigerteArbeidsgiveropplysninger = håndterKorrigerteArbeidsgiveropplysninger(1.vedtaksperiode, OppgittInntekt(INNTEKT), OppgittRefusjon(INNTEKT, emptyList()))
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT)
            }
            // Burde vi unngått en overstyring her?
            assertBeløpstidslinje(Beløpstidslinje.fra(januar, INNTEKT, korrigerteArbeidsgiveropplysninger.arbeidsgiver), inspektør.refusjon(1.vedtaksperiode))
        }
    }

    @Test
    fun `opplyser om korrigerert arbeidsgiverperiode`() {
        a1 {
            nyttVedtak(januar)
            val korrigeringId = håndterKorrigerteArbeidsgiveropplysninger(1.vedtaksperiode, OppgittArbeidgiverperiode(listOf(2.januar til 17.januar)))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertVarsler(1.vedtaksperiode, RV_IM_24)
            assertDokumentsporingPåSisteEndring(1.vedtaksperiode, Dokumentsporing.inntektsmeldingDager(MeldingsreferanseId(korrigeringId)))
            assertTrue(observatør.inntektsmeldingHåndtert.contains(korrigeringId to 1.vedtaksperiode))
        }
    }

    @Test
    fun `opplyser om lik arbeidsgiverperiode`() {
        a1 {
            nyttVedtak(januar)
            val korrigeringId = håndterKorrigerteArbeidsgiveropplysninger(1.vedtaksperiode, OppgittArbeidgiverperiode(listOf(1.januar til 16.januar)))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertVarsler(emptyList(), 1.vedtaksperiode.filter())
            assertTrue(observatør.inntektsmeldingHåndtert.contains(korrigeringId to 1.vedtaksperiode))
        }
    }

    @Test
    fun `opplyser om at de kun UtbetaltDelerAvArbeidsgiverperioden læll`() {
        a1 {
            nyttVedtak(januar)
            val korrigeringId = håndterKorrigerteArbeidsgiveropplysninger(1.vedtaksperiode, OppgittArbeidgiverperiode(listOf(1.januar til 10.januar)), UtbetaltDelerAvArbeidsgiverperioden(ManglerOpptjening, 10.januar))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertVarsler(1.vedtaksperiode, RV_IM_8)
            assertDokumentsporingPåSisteEndring(1.vedtaksperiode, Dokumentsporing.inntektsmeldingDager(MeldingsreferanseId(korrigeringId)))
            assertTrue(observatør.inntektsmeldingHåndtert.contains(korrigeringId to 1.vedtaksperiode))
        }
    }

    @Test
    fun `opplyser om at de kun RedusertUtbetaltBeløpIArbeidsgiverperioden læll`() {
        a1 {
            nyttVedtak(januar)
            val korrigeringId = håndterKorrigerteArbeidsgiveropplysninger(1.vedtaksperiode, OppgittArbeidgiverperiode(listOf(2.januar til 17.januar)), RedusertUtbetaltBeløpIArbeidsgiverperioden(StreikEllerLockout))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertVarsler(1.vedtaksperiode, RV_IM_24, RV_IM_8)
            assertDokumentsporingPåSisteEndring(1.vedtaksperiode, Dokumentsporing.inntektsmeldingDager(MeldingsreferanseId(korrigeringId)))
            assertTrue(observatør.inntektsmeldingHåndtert.contains(korrigeringId to 1.vedtaksperiode))
        }
    }

    @Test
    fun `opplyser om at det vær opphør i naturalytelser læll`() {
        a1 {
            nyttVedtak(januar)
            val korrigeringId = håndterKorrigerteArbeidsgiveropplysninger(1.vedtaksperiode, OpphørAvNaturalytelser)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertVarsler(1.vedtaksperiode, RV_IM_7)
            assertDokumentsporingPåSisteEndring(1.vedtaksperiode, Dokumentsporing.inntektsmeldingDager(MeldingsreferanseId(korrigeringId)))
            assertTrue(observatør.inntektsmeldingHåndtert.contains(korrigeringId to 1.vedtaksperiode))
        }
    }

    private fun assertDokumentsporingPåSisteEndring(vedtaksperiode: UUID, forventet: Dokumentsporing) {
        val faktisk = inspektør.vedtaksperioder(vedtaksperiode).behandlinger.behandlinger.last().endringer.last().dokumentsporing
        assertEquals(forventet, faktisk)
    }
}
