package no.nav.helse.spleis.e2e.arbeidsgiveropplysninger

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.hendelser.Arbeidsgiveropplysning
import no.nav.helse.januar
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.arbeidsgiver
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.assertBeløpstidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class KorrigerteArbeidsigveropplysningerTest : AbstractDslTest() {

    @Test
    fun `opplyser om korrigerert inntekt på en allerede utbetalt periode`() {
        a1 {
            nyttVedtak(januar)
            val refusjonFørKorrigering = inspektør.refusjon(1.vedtaksperiode)
            håndterKorrigerteArbeidsgiveropplysninger(1.vedtaksperiode, Arbeidsgiveropplysning.OppgittInntekt(INNTEKT * 1.25))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertEquals(INNTEKT * 1.25, inspektør.inntekt(1.vedtaksperiode).beløp)
            assertEquals(refusjonFørKorrigering, inspektør.refusjon(1.vedtaksperiode))
        }
    }

    @Test
    fun `opplyser om korrigerert refusjon på en allerede utbetalt periode`() {
        a1 {
            nyttVedtak(januar)
            val inntektFørKorrigering = inspektør.inntekt(1.vedtaksperiode).beløp
            val korrigerteArbeidsgiveropplysninger = håndterKorrigerteArbeidsgiveropplysninger(1.vedtaksperiode, Arbeidsgiveropplysning.OppgittRefusjon(INNTEKT * 1.25, emptyList()))
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            assertEquals(inntektFørKorrigering, inspektør.inntekt(1.vedtaksperiode).beløp)
            assertBeløpstidslinje(Beløpstidslinje.fra(januar, INNTEKT * 1.25, korrigerteArbeidsgiveropplysninger.arbeidsgiver), inspektør.refusjon(1.vedtaksperiode))
        }
    }

    @Test
    fun `opplyser om korrigerert inntekt OG refusjon på en allerede utbetalt periode`() {
        a1 {
            nyttVedtak(januar)
            val korrigerteArbeidsgiveropplysninger = håndterKorrigerteArbeidsgiveropplysninger(1.vedtaksperiode, Arbeidsgiveropplysning.OppgittInntekt(INNTEKT * 1.25), Arbeidsgiveropplysning.OppgittRefusjon(INNTEKT * 1.25, emptyList()))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertEquals(INNTEKT * 1.25, inspektør.inntekt(1.vedtaksperiode).beløp)
            assertBeløpstidslinje(Beløpstidslinje.fra(januar, INNTEKT * 1.25, korrigerteArbeidsgiveropplysninger.arbeidsgiver), inspektør.refusjon(1.vedtaksperiode))
        }
    }

    @Test
    fun `korrigerende opplysninger mens vi venter på forespurte opplysninger`() {
        a1 {
            håndterSøknad(januar)
            assertThrows<IllegalStateException> {
                håndterKorrigerteArbeidsgiveropplysninger(1.vedtaksperiode, Arbeidsgiveropplysning.OppgittInntekt(INNTEKT * 1.25))
            }
        }
    }

    @Test
    fun `opplyser om korrigerert inntekt OG refusjon på en allerede utbetalt periode - men beløpene er uendret`() {
        a1 {
            nyttVedtak(januar)
            val inntektFørKorrigering = inspektør.inntekt(1.vedtaksperiode).beløp
            val korrigerteArbeidsgiveropplysninger = håndterKorrigerteArbeidsgiveropplysninger(1.vedtaksperiode, Arbeidsgiveropplysning.OppgittInntekt(INNTEKT), Arbeidsgiveropplysning.OppgittRefusjon(INNTEKT, emptyList()))
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            assertEquals(inntektFørKorrigering, inspektør.inntekt(1.vedtaksperiode).beløp)
            // Burde vi unngått en overstyring her?
            assertBeløpstidslinje(Beløpstidslinje.fra(januar, INNTEKT, korrigerteArbeidsgiveropplysninger.arbeidsgiver), inspektør.refusjon(1.vedtaksperiode))
        }
    }
}
